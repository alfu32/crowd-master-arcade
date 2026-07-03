package com.crowdmasterarcade.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.crowdmasterarcade.model.AppModel
import com.crowdmasterarcade.model.Boss
import com.crowdmasterarcade.model.Card
import com.crowdmasterarcade.model.CardOperation
import com.crowdmasterarcade.model.CardTarget
import com.crowdmasterarcade.model.Decoration
import com.crowdmasterarcade.model.LevelColor
import com.crowdmasterarcade.model.LevelModelPaths
import com.crowdmasterarcade.model.ResourceHome
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.math.max
import kotlin.math.min

class WorldRenderer {
    private companion object {
        const val SHADOW_MAP_SIZE = 8192
        const val SHADOW_VIEWPORT = 150f
        const val SHADOW_NEAR = 1f
        const val SHADOW_FAR = 500f
        const val SHADOW_LOOKAHEAD_Z = 18f
        const val ROAD_START_Z = -28f
    }

    private val camera = PerspectiveCamera(50f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    private val environment = Environment()
    private val shadowSettings = ShadowSettings()
    private val shadowLight = DirectionalShadowLight(
        SHADOW_MAP_SIZE,
        SHADOW_MAP_SIZE,
        SHADOW_VIEWPORT,
        SHADOW_VIEWPORT,
        SHADOW_NEAR,
        SHADOW_FAR
    )
    private val mainLight = DirectionalLight()
    private val fillLight = DirectionalLight()
        .set(0.005f, 0.005f, 0.005f, 1.2f, 1.8f, 0.5f)
        .setColor(Color(0.005f, 0.005f, 0.005f, 0.15f))
    private val modelBatch = ModelBatch(SketchShaderProvider({ shadowSettings }, { shadowLight }))
    private val shadowBatch = ModelBatch(DepthShaderProvider())
    private val assets = RenderAssets()
    private val text3d = Text3dRenderer(assets)
    private var activeBatch = modelBatch
    private val shadowCenter = Vector3()
    private val shadowDirection = Vector3(-0.5f, -1.8f, 1.2f)

    init {
        shadowLight.set(0.62f, 0.62f, 0.62f, -0.5f, -1.8f, 1.2f)
        shadowLight.setColor(Color(0.62f, 0.62f, 0.62f, 0.5f))
        mainLight.set(0.78f, 0.78f, 0.78f, -0.5f, -1.8f, 1.2f)
        mainLight.color.set(0.78f, 0.78f, 0.78f, 1f)
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.42f, 0.42f, 0.42f, 1f))
        environment.set(ColorAttribute(ColorAttribute.Specular, 0.2f, 0.2f, 0.2f, 0.95f))
        environment.add(shadowLight)
        environment.add(mainLight)
        environment.add(fillLight)
        environment.shadowMap = shadowLight
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
    }

    fun render(appModel: AppModel) {
        assets.useModelPaths(appModel.levelData.modelPaths)

        camera.viewportWidth = Gdx.graphics.width.toFloat()
        camera.viewportHeight = Gdx.graphics.height.toFloat()
        camera.position.set(appModel.player.position.x * 0.35f, 6.5f, -11f)
        camera.lookAt(appModel.player.position.x * 0.2f, 0f, 15f)
        camera.near = 0.1f
        camera.far = 500f
        camera.update()

        activeBatch = shadowBatch
        shadowCenter.set(appModel.player.position.x, appModel.player.position.y, appModel.player.position.z + SHADOW_LOOKAHEAD_Z)
        shadowLight.begin(shadowCenter, shadowDirection)
        shadowBatch.begin(shadowLight.camera)
        renderSceneModels(appModel)
        shadowBatch.end()
        shadowLight.end()

        activeBatch = modelBatch
        modelBatch.begin(camera)
        renderSceneModels(appModel)
        appModel.cards.filter { it.active }.forEach { card ->
            text3d.renderCardText(modelBatch, environment, card)
        }
        appModel.bosses.filter { it.active && it.alive }.forEach { boss ->
            text3d.renderBossText(modelBatch, environment, boss)
        }
        modelBatch.end()
    }

    private fun renderSceneModels(appModel: AppModel) {
        renderRoad(appModel)
        renderSoldiers(
            appModel.player.soldiers,
            assets.soldier(appModel.levelData.modelPaths.soldier, appModel.player.color.toGdxColor())
        )
        appModel.enemyBrigades.filter { it.alive }.forEach {
            renderSoldiers(it.soldiers, assets.soldier(it.modelPath, it.color.toGdxColor()))
        }
        appModel.cards.filter { it.active }.forEach(::renderCard)
        appModel.decorations.filter { it.active }.forEach(::renderDecoration)
        appModel.projectiles.filter { it.active }.forEach {
            assets.projectile.transform.setToTranslation(it.position)
            activeBatch.render(assets.projectile, environment)
        }
        appModel.bosses.filter { it.active && it.alive }.forEach { boss ->
            val instance = assets.boss(boss.modelPath, boss.color.toGdxColor())
            instance.transform.setToTranslation(boss.position)
            activeBatch.render(instance, environment)
        }
    }

    private fun renderRoad(appModel: AppModel) {
        val centerZ = ROAD_START_Z + appModel.road.length / 2f
        assets.road.transform.setToTranslation(0f, -0.08f, centerZ)
        assets.road.transform.scale(appModel.road.width, 1f, appModel.road.length)
        activeBatch.render(assets.road, environment)
        assets.leftRail.transform.setToTranslation(appModel.road.leftBoundary - 0.15f, 0.05f, centerZ)
        assets.leftRail.transform.scale(1f, 1f, appModel.road.length)
        assets.rightRail.transform.setToTranslation(appModel.road.rightBoundary + 0.15f, 0.05f, centerZ)
        assets.rightRail.transform.scale(1f, 1f, appModel.road.length)
        activeBatch.render(assets.leftRail, environment)
        activeBatch.render(assets.rightRail, environment)
    }

    private fun renderSoldiers(
        soldiers: Iterable<com.crowdmasterarcade.model.RegularSoldier>,
        instance: ModelInstance
    ) {
        soldiers.filter { it.alive }.forEach { soldier ->
            instance.transform.setToTranslation(soldier.worldPosition)
            activeBatch.render(instance, environment)
        }
    }

    private fun renderCard(card: Card) {
        val color = cardColor(card)
        val instance = assets.card(card, color)
        instance.transform.setToTranslation(card.position)
        activeBatch.render(instance, environment)
    }

    private fun renderDecoration(decoration: Decoration) {
        val instance = assets.decoration(decoration.modelPath, decoration.color.toGdxColor())
        instance.transform.setToTranslation(decoration.position)
        activeBatch.render(instance, environment)
    }

    private fun cardColor(card: Card): Color =
        when (card.operation) {
            CardOperation.PLUS -> Color(0.12f, 0.74f, 0.24f, 1f)
            CardOperation.MINUS -> Color(0.88f, 0.24f, 0.22f, 1f)
            CardOperation.TIMES -> Color(0.2f, 0.48f, 0.9f, 1f)
            CardOperation.DIV -> Color(0.9f, 0.66f, 0.14f, 1f)
        }

    private fun LevelColor.toGdxColor(): Color =
        Color(red, green, blue, alpha)

    fun dispose() {
        assets.dispose()
        modelBatch.dispose()
        shadowBatch.dispose()
        shadowLight.dispose()
    }

    private class RenderAssets {
        private val builder = ModelBuilder()
        private val objLoader = ObjLoader()
        private val ownedModels = mutableListOf<Model>()
        private var currentPaths: LevelModelPaths? = null

        val road = instance(box(1f, 0.12f, 1f, Color(0.24f, 0.27f, 0.28f, 1f)))
        val leftRail = instance(box(0.12f, 0.16f, 1f, Color(0.92f, 0.86f, 0.42f, 1f)))
        val rightRail = instance(box(0.12f, 0.16f, 1f, Color(0.92f, 0.86f, 0.42f, 1f)))
        val projectile = instance(sphere(0.18f, Color(1f, 0.9f, 0.2f, 1f)))
        val textBlock = instance(box(0.055f, 0.055f, 0.035f, Color.BLACK))
        private val decorationInstances = mutableMapOf<String, ModelInstance>()
        private val soldierInstances = mutableMapOf<String, ModelInstance>()
        private val bossInstances = mutableMapOf<String, ModelInstance>()
        private val cardInstances = mutableMapOf<String, ModelInstance>()
        private val coloredDecorationInstances = mutableMapOf<ColoredInstanceKey, ModelInstance>()
        private val coloredSoldierInstances = mutableMapOf<ColoredInstanceKey, ModelInstance>()
        private val coloredBossInstances = mutableMapOf<ColoredInstanceKey, ModelInstance>()
        private val coloredCardInstances = mutableMapOf<ColoredInstanceKey, ModelInstance>()

        var soldier = instance(box(0.36f, 0.8f, 0.36f, Color.WHITE))
            private set
        var boss = instance(box(2.4f, 2.6f, 2.4f, Color.WHITE))
            private set
        var manpowerCard = instance(box(1.2f, 1.2f, 0.22f, Color.WHITE))
            private set
        var firepowerCard = instance(box(1.2f, 1.2f, 0.22f, Color.WHITE))
            private set
        var bulletPowerCard = instance(box(1.2f, 1.2f, 0.22f, Color.WHITE))
            private set
        var soldierLifeCard = instance(box(1.2f, 1.2f, 0.22f, Color.WHITE))
            private set
        var manpowerCardTopY = modelTopY(manpowerCard.model)
            private set
        var firepowerCardTopY = modelTopY(firepowerCard.model)
            private set
        var bulletPowerCardTopY = modelTopY(bulletPowerCard.model)
            private set
        var soldierLifeCardTopY = modelTopY(soldierLifeCard.model)
            private set
        var bossTopY = modelTopY(boss.model)
            private set

        fun useModelPaths(paths: LevelModelPaths) {
            if (paths == currentPaths) return
            currentPaths = paths
            soldier = instance(loadObj(paths.soldier, fallback = { box(0.36f, 0.8f, 0.36f, Color.WHITE) }))
            boss = instance(loadObj(paths.boss, fallback = { box(2.4f, 2.6f, 2.4f, Color.WHITE) }))
            manpowerCard = instance(loadObj(paths.manpowerCard, fallback = { box(1.2f, 1.2f, 0.22f, Color.WHITE) }))
            firepowerCard = instance(loadObj(paths.firepowerCard, fallback = { box(1.2f, 1.2f, 0.22f, Color.WHITE) }))
            bulletPowerCard = instance(loadObj(paths.bulletPowerCard, fallback = { box(1.2f, 1.2f, 0.22f, Color.WHITE) }))
            soldierLifeCard = instance(loadObj(paths.soldierLifeCard, fallback = { box(1.2f, 1.2f, 0.22f, Color.WHITE) }))
            manpowerCardTopY = modelTopY(manpowerCard.model)
            firepowerCardTopY = modelTopY(firepowerCard.model)
            bulletPowerCardTopY = modelTopY(bulletPowerCard.model)
            soldierLifeCardTopY = modelTopY(soldierLifeCard.model)
            bossTopY = modelTopY(boss.model)
        }

        fun soldier(path: String, color: Color): ModelInstance =
            coloredInstance(ColoredInstanceKey(path, colorKey(color)), soldierBase(path), coloredSoldierInstances, color)

        private fun soldierBase(path: String): ModelInstance =
            if (currentPaths?.soldier == path) {
                soldier
            } else {
                soldierInstances.getOrPut(path) {
                    instance(loadObj(path, fallback = { box(0.36f, 0.8f, 0.36f, Color.WHITE) }))
                }
            }

        fun boss(path: String, color: Color): ModelInstance =
            coloredInstance(ColoredInstanceKey(path, colorKey(color)), bossBase(path), coloredBossInstances, color)

        private fun bossBase(path: String): ModelInstance =
            if (currentPaths?.boss == path) {
                boss
            } else {
                bossInstances.getOrPut(path) {
                    instance(loadObj(path, fallback = { box(2.4f, 2.6f, 2.4f, Color.WHITE) }))
                }
            }

        fun card(card: Card, color: Color): ModelInstance =
            coloredInstance(ColoredInstanceKey(card.modelPath, colorKey(color)), cardBase(card), coloredCardInstances, color)

        private fun cardBase(card: Card): ModelInstance {
            val defaultPath = cardDefaultPath(card.target)
            if (defaultPath == card.modelPath) {
                return defaultCardInstance(card.target)
            }
            return cardInstances.getOrPut(card.modelPath) {
                instance(loadObj(card.modelPath, fallback = { box(1.2f, 1.2f, 0.22f, Color.WHITE) }))
            }
        }

        fun cardTopY(card: Card): Float =
            if (cardDefaultPath(card.target) == card.modelPath) {
                defaultCardTopY(card.target)
            } else {
                modelTopY(cardBase(card).model)
            }

        private fun cardDefaultPath(target: CardTarget): String? =
            when (target) {
                CardTarget.MANPOWER -> currentPaths?.manpowerCard
                CardTarget.FIREPOWER -> currentPaths?.firepowerCard
                CardTarget.BULLET_POWER -> currentPaths?.bulletPowerCard
                CardTarget.SOLDIER_LIFE -> currentPaths?.soldierLifeCard
            }

        private fun defaultCardInstance(target: CardTarget): ModelInstance =
            when (target) {
                CardTarget.MANPOWER -> manpowerCard
                CardTarget.FIREPOWER -> firepowerCard
                CardTarget.BULLET_POWER -> bulletPowerCard
                CardTarget.SOLDIER_LIFE -> soldierLifeCard
            }

        private fun defaultCardTopY(target: CardTarget): Float =
            when (target) {
                CardTarget.MANPOWER -> manpowerCardTopY
                CardTarget.FIREPOWER -> firepowerCardTopY
                CardTarget.BULLET_POWER -> bulletPowerCardTopY
                CardTarget.SOLDIER_LIFE -> soldierLifeCardTopY
            }

        fun bossTopY(path: String): Float =
            if (currentPaths?.boss == path) bossTopY else modelTopY(bossBase(path).model)

        fun decoration(path: String, color: Color): ModelInstance =
            coloredInstance(ColoredInstanceKey(path, colorKey(color)), decorationBase(path), coloredDecorationInstances, color)

        private fun decorationBase(path: String): ModelInstance =
            decorationInstances.getOrPut(path) {
                instance(loadObj(path, fallback = { box(2f, 2f, 2f, Color.WHITE) }))
            }

        private fun coloredInstance(
            key: ColoredInstanceKey,
            base: ModelInstance,
            cache: MutableMap<ColoredInstanceKey, ModelInstance>,
            color: Color
        ): ModelInstance =
            cache.getOrPut(key) {
                instance(base.model).also { tint(it, color) }
            }

        private fun tint(instance: ModelInstance, color: Color) {
            instance.materials.forEach { material ->
                material.set(ColorAttribute.createDiffuse(color))
                if (color.a < 0.999f) {
                    material.set(BlendingAttribute(true, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, color.a))
                } else {
                    material.remove(BlendingAttribute.Type)
                }
            }
        }

        private fun colorKey(color: Color): Int =
            ((color.r.coerceIn(0f, 1f) * 255f + 0.5f).toInt() shl 24) or
                ((color.g.coerceIn(0f, 1f) * 255f + 0.5f).toInt() shl 16) or
                ((color.b.coerceIn(0f, 1f) * 255f + 0.5f).toInt() shl 8) or
                (color.a.coerceIn(0f, 1f) * 255f + 0.5f).toInt()

        private fun loadObj(path: String, fallback: () -> Model): Model {
            val file = resolve(path)
            if (!file.exists()) return fallback()
            return try {
                objLoader.loadModel(objWithNormals(file))
                    .also(::normalizeModelToMinOrigin)
                    .also(ownedModels::add)
            } catch (_: RuntimeException) {
                fallback()
            }
        }

        private fun resolve(path: String): FileHandle {
            val external = File(path)
            if (external.isAbsolute && external.exists()) return Gdx.files.absolute(path)
            return ResourceHome.resolve(path)
        }

        private fun objWithNormals(file: FileHandle): FileHandle {
            val text = file.readString("UTF-8")
            if (text.lineSequence().any { it.trimStart().startsWith("vn ") }) return file

            val vertices = mutableListOf<Vector3>()
            val output = StringBuilder(text.length + text.length / 4)
            var normalIndex = 0

            text.lineSequence().forEach { line ->
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("v ") -> {
                        val parts = trimmed.split(WHITESPACE)
                        if (parts.size >= 4) {
                            vertices += Vector3(parts[1].toFloat(), parts[2].toFloat(), parts[3].toFloat())
                        }
                        output.append(line).append('\n')
                    }
                    trimmed.startsWith("f ") -> {
                        val faceTokens = trimmed.substring(2).trim().split(WHITESPACE).filter { it.isNotBlank() }
                        val normal = faceNormal(faceTokens, vertices)
                        normalIndex += 1
                        output.append("vn ")
                            .append(normal.x).append(' ')
                            .append(normal.y).append(' ')
                            .append(normal.z).append('\n')
                        output.append("f ")
                            .append(faceTokens.joinToString(" ") { token -> tokenWithNormal(token, normalIndex) })
                            .append('\n')
                    }
                    else -> output.append(line).append('\n')
                }
            }

            return InMemoryFileHandle(file.path(), output.toString())
        }

        private fun faceNormal(faceTokens: List<String>, vertices: List<Vector3>): Vector3 {
            if (faceTokens.size < 3) return Vector3.Y.cpy()
            val a = vertices.getOrNull(vertexIndex(faceTokens[0], vertices.size)) ?: return Vector3.Y.cpy()
            val b = vertices.getOrNull(vertexIndex(faceTokens[1], vertices.size)) ?: return Vector3.Y.cpy()
            val c = vertices.getOrNull(vertexIndex(faceTokens[2], vertices.size)) ?: return Vector3.Y.cpy()
            val normal = TEMP_NORMAL_A.set(b).sub(a).crs(TEMP_NORMAL_B.set(c).sub(a))
            if (normal.len2() < 0.000001f) return Vector3.Y.cpy()
            return normal.nor().cpy()
        }

        private fun vertexIndex(faceToken: String, vertexCount: Int): Int {
            val raw = faceToken.substringBefore('/').toIntOrNull() ?: return -1
            return if (raw > 0) raw - 1 else vertexCount + raw
        }

        private fun tokenWithNormal(token: String, normalIndex: Int): String {
            val parts = token.split('/')
            return when (parts.size) {
                1 -> "${parts[0]}//$normalIndex"
                2 -> "${parts[0]}/${parts[1]}/$normalIndex"
                else -> "${parts[0]}/${parts[1]}/$normalIndex"
            }
        }

        private fun box(width: Float, height: Float, depth: Float, color: Color): Model {
            val model = builder.createBox(
                width,
                height,
                depth,
                Material(ColorAttribute.createDiffuse(color)),
                (Usage.Position or Usage.Normal).toLong()
            )
            ownedModels.add(model)
            return model
        }

        private fun sphere(radius: Float, color: Color): Model {
            val model = builder.createSphere(
                radius,
                radius,
                radius,
                12,
                12,
                Material(ColorAttribute.createDiffuse(color)),
                (Usage.Position or Usage.Normal).toLong()
            )
            ownedModels.add(model)
            return model
        }

        private fun instance(model: Model) = ModelInstance(model)

        private fun modelTopY(model: Model): Float = modelBounds(model).maxY

        private fun normalizeModelToMinOrigin(model: Model) {
            var minX = Float.POSITIVE_INFINITY
            var minY = Float.POSITIVE_INFINITY
            var minZ = Float.POSITIVE_INFINITY
            var maxX = Float.NEGATIVE_INFINITY
            var maxZ = Float.NEGATIVE_INFINITY

            model.meshes.forEach { mesh ->
                val position = mesh.getVertexAttribute(Usage.Position) ?: return@forEach
                val vertexSize = mesh.vertexSize / 4
                val positionOffset = position.offset / 4
                val vertices = FloatArray(mesh.numVertices * vertexSize)
                mesh.getVertices(vertices)
                for (vertex in 0 until mesh.numVertices) {
                    val base = vertex * vertexSize + positionOffset
                    minX = min(minX, vertices[base])
                    minY = min(minY, vertices[base + 1])
                    minZ = min(minZ, vertices[base + 2])
                    maxX = max(maxX, vertices[base])
                    maxZ = max(maxZ, vertices[base + 2])
                }
            }

            if (!minX.isFinite() || !minY.isFinite() || !minZ.isFinite() || !maxX.isFinite() || !maxZ.isFinite()) return

            val centerX = (minX + maxX) * 0.5f
            val centerZ = (minZ + maxZ) * 0.5f

            model.meshes.forEach { mesh ->
                val position = mesh.getVertexAttribute(Usage.Position) ?: return@forEach
                val vertexSize = mesh.vertexSize / 4
                val positionOffset = position.offset / 4
                val vertices = FloatArray(mesh.numVertices * vertexSize)
                mesh.getVertices(vertices)
                for (vertex in 0 until mesh.numVertices) {
                    val base = vertex * vertexSize + positionOffset
                    vertices[base] -= centerX
                    vertices[base + 1] -= minY
                    vertices[base + 2] -= centerZ
                }
                mesh.setVertices(vertices)
            }
        }

        private fun modelBounds(model: Model): ModelBounds {
            var minY = Float.POSITIVE_INFINITY
            var maxY = Float.NEGATIVE_INFINITY
            model.meshes.forEach { mesh ->
                val position = mesh.getVertexAttribute(Usage.Position) ?: return@forEach
                val vertexSize = mesh.vertexSize / 4
                val positionOffset = position.offset / 4
                val vertices = FloatArray(mesh.numVertices * vertexSize)
                mesh.getVertices(vertices)
                for (vertex in 0 until mesh.numVertices) {
                    val base = vertex * vertexSize + positionOffset
                    val y = vertices[base + 1]
                    minY = min(minY, y)
                    maxY = max(maxY, y)
                }
            }
            if (!minY.isFinite() || !maxY.isFinite()) return ModelBounds(0f, 0f)
            return ModelBounds(minY, maxY)
        }

        private data class ModelBounds(val minY: Float, val maxY: Float)
        private data class ColoredInstanceKey(val path: String, val color: Int)

        companion object {
            private val WHITESPACE = Regex("\\s+")
            private val TEMP_NORMAL_A = Vector3()
            private val TEMP_NORMAL_B = Vector3()
        }

        private class InMemoryFileHandle(path: String, private val text: String) : FileHandle(path) {
            override fun exists(): Boolean = true
            override fun read() = ByteArrayInputStream(text.toByteArray(Charsets.UTF_8))
            override fun readString(charset: String?): String = text
            override fun length(): Long = text.toByteArray(Charsets.UTF_8).size.toLong()
        }

        fun dispose() {
            ownedModels.distinct().forEach(Model::dispose)
        }
    }

    private class Text3dRenderer(private val assets: RenderAssets) {
        fun renderCardText(modelBatch: ModelBatch, environment: Environment, card: Card) {
            val cardTopOffset = assets.cardTopY(card)
            val cardTopY = card.position.y + cardTopOffset
            val smallCell = 0.08f
            val largeCell = 0.14f
            val targetBaselineY = cardTopY + 0.58f
            val operationBaselineY = targetBaselineY + glyphHeight(largeCell) + 0.18f
            renderLine(modelBatch, environment, operationLabel(card), card.position.x, operationBaselineY, card.position.z - 0.14f, largeCell)
            renderLine(modelBatch, environment, targetTop(card), card.position.x, targetBaselineY, card.position.z - 0.14f, smallCell)
        }

        fun renderBossText(modelBatch: ModelBatch, environment: Environment, boss: Boss) {
            val baseY = boss.position.y + assets.bossTopY(boss.modelPath) + 0.82f
            val z = boss.position.z - 0.22f
            renderLine(modelBatch, environment, boss.name, boss.position.x, baseY + glyphHeight(0.1f) + 0.18f, z, 0.1f)
            renderLine(
                modelBatch,
                environment,
                "${boss.health.coerceAtLeast(0f).toInt()}/${boss.maxHealth.toInt()}",
                boss.position.x,
                baseY,
                z,
                0.095f
            )
        }

        private fun renderLine(
            modelBatch: ModelBatch,
            environment: Environment,
            text: String,
            centerX: Float,
            baselineY: Float,
            z: Float,
            cell: Float
        ) {
            val upper = text.uppercase()
            val width = upper.sumOf { (glyph(it).firstOrNull()?.length ?: 0) + 1 }.coerceAtLeast(1) - 1
            var cursorX = centerX - width * cell * 0.5f
            upper.forEach { char ->
                val glyph = glyph(char)
                val glyphWidth = glyph.firstOrNull()?.length ?: 0
                glyph.forEachIndexed { row, bits ->
                    bits.forEachIndexed { col, bit ->
                        if (bit == '1') {
                            assets.textBlock.transform.setToTranslation(
                                cursorX + (glyphWidth - 1 - col) * cell,
                                baselineY - row * cell,
                                z
                            )
                            assets.textBlock.transform.rotate(Vector3.Y, 180f)
                            assets.textBlock.transform.scale(cell / 0.055f, cell / 0.055f, 1f)
                            modelBatch.render(assets.textBlock, environment)
                        }
                    }
                }
                cursorX += glyphWidth * cell + cell
            }
        }

        private fun glyphHeight(cell: Float): Float = 5f * cell

        private fun operationLabel(card: Card): String =
            when (card.operation) {
                CardOperation.PLUS -> "+${card.value.toInt()}"
                CardOperation.MINUS -> "-${card.value.toInt()}"
                CardOperation.TIMES -> "X${card.value.toInt()}"
                CardOperation.DIV -> "/${card.value.toInt()}"
            }

        private fun targetTop(card: Card): String =
            when (card.target) {
                CardTarget.MANPOWER -> "MAN"
                CardTarget.FIREPOWER -> "FIRE"
                CardTarget.BULLET_POWER -> "BUL"
                CardTarget.SOLDIER_LIFE -> "LIFE"
            }

        private fun glyph(char: Char): List<String> =
            GLYPHS[char] ?: GLYPHS[' ']!!

        companion object {
            private val GLYPHS = mapOf(
                ' ' to listOf("000", "000", "000", "000", "000"),
                '+' to listOf("00100", "00100", "11111", "00100", "00100"),
                '-' to listOf("00000", "00000", "11111", "00000", "00000"),
                '/' to listOf("00001", "00010", "00100", "01000", "10000"),
                '0' to listOf("111", "101", "101", "101", "111"),
                '1' to listOf("010", "110", "010", "010", "111"),
                '2' to listOf("111", "001", "111", "100", "111"),
                '3' to listOf("111", "001", "111", "001", "111"),
                '4' to listOf("101", "101", "111", "001", "001"),
                '5' to listOf("111", "100", "111", "001", "111"),
                '6' to listOf("111", "100", "111", "101", "111"),
                '7' to listOf("111", "001", "010", "010", "010"),
                '8' to listOf("111", "101", "111", "101", "111"),
                '9' to listOf("111", "101", "111", "001", "111"),
                'A' to listOf("01110", "10001", "11111", "10001", "10001"),
                'B' to listOf("11110", "10001", "11110", "10001", "11110"),
                'C' to listOf("01111", "10000", "10000", "10000", "01111"),
                'D' to listOf("11110", "10001", "10001", "10001", "11110"),
                'E' to listOf("11111", "10000", "11110", "10000", "11111"),
                'F' to listOf("11111", "10000", "11110", "10000", "10000"),
                'G' to listOf("01111", "10000", "10011", "10001", "01111"),
                'H' to listOf("10001", "10001", "11111", "10001", "10001"),
                'I' to listOf("111", "010", "010", "010", "111"),
                'J' to listOf("00111", "00010", "00010", "10010", "01100"),
                'K' to listOf("10001", "10010", "11100", "10010", "10001"),
                'L' to listOf("10000", "10000", "10000", "10000", "11111"),
                'M' to listOf("10001", "11011", "10101", "10001", "10001"),
                'N' to listOf("10001", "11001", "10101", "10011", "10001"),
                'O' to listOf("01110", "10001", "10001", "10001", "01110"),
                'P' to listOf("11110", "10001", "11110", "10000", "10000"),
                'Q' to listOf("01110", "10001", "10001", "10011", "01111"),
                'R' to listOf("11110", "10001", "11110", "10010", "10001"),
                'S' to listOf("01111", "10000", "01110", "00001", "11110"),
                'T' to listOf("11111", "00100", "00100", "00100", "00100"),
                'U' to listOf("10001", "10001", "10001", "10001", "01110"),
                'V' to listOf("10001", "10001", "10001", "01010", "00100"),
                'W' to listOf("10001", "10001", "10101", "11011", "10001"),
                'X' to listOf("10001", "01010", "00100", "01010", "10001"),
                'Y' to listOf("10001", "01010", "00100", "00100", "00100"),
                'Z' to listOf("11111", "00010", "00100", "01000", "11111")
            )
        }
    }
}
