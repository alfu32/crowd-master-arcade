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
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.crowdmasterarcade.model.AppModel
import com.crowdmasterarcade.model.Card
import com.crowdmasterarcade.model.CardOperation
import com.crowdmasterarcade.model.CardTarget
import com.crowdmasterarcade.model.LevelModelPaths
import java.io.File

class WorldRenderer {
    private val modelBatch = ModelBatch()
    private val camera = PerspectiveCamera(50f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    private val environment = Environment()
    private val assets = RenderAssets()
    private val text3d = Text3dRenderer(assets)

    init {
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.62f, 0.62f, 0.62f, 1f))
        environment.add(DirectionalLight().set(0.82f, 0.82f, 0.76f, -0.25f, -0.8f, -0.35f))
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

        modelBatch.begin(camera)
        renderRoad(appModel)
        renderSoldiers(appModel.player.soldiers, assets.soldier, Color(0.12f, 0.72f, 0.92f, 1f))
        appModel.enemyBrigades.filter { it.alive }.forEach {
            renderSoldiers(it.soldiers, assets.soldier, Color(0.84f, 0.16f, 0.18f, 1f))
        }
        appModel.cards.filter { it.active }.forEach(::renderCard)
        appModel.projectiles.filter { it.active }.forEach {
            assets.projectile.transform.setToTranslation(it.position)
            modelBatch.render(assets.projectile, environment)
        }
        appModel.bosses.filter { it.active && it.alive }.forEach { boss ->
            assets.boss.transform.setToTranslation(boss.position)
            colorize(assets.boss, Color(0.36f, 0.14f, 0.58f, 1f))
            modelBatch.render(assets.boss, environment)
        }
        appModel.cards.filter { it.active }.forEach { card ->
            text3d.renderCardText(modelBatch, environment, card)
        }
        modelBatch.end()
    }

    private fun renderRoad(appModel: AppModel) {
        assets.road.transform.setToTranslation(0f, -0.08f, appModel.road.length / 2f)
        modelBatch.render(assets.road, environment)
        assets.leftRail.transform.setToTranslation(appModel.road.leftBoundary - 0.15f, 0.05f, appModel.road.length / 2f)
        assets.rightRail.transform.setToTranslation(appModel.road.rightBoundary + 0.15f, 0.05f, appModel.road.length / 2f)
        modelBatch.render(assets.leftRail, environment)
        modelBatch.render(assets.rightRail, environment)
    }

    private fun renderSoldiers(
        soldiers: Iterable<com.crowdmasterarcade.model.RegularSoldier>,
        instance: ModelInstance,
        color: Color
    ) {
        colorize(instance, color)
        soldiers.filter { it.alive }.forEach { soldier ->
            instance.transform.setToTranslation(soldier.worldPosition)
            modelBatch.render(instance, environment)
        }
    }

    private fun renderCard(card: Card) {
        val instance = if (card.target == CardTarget.FIREPOWER) assets.firepowerCard else assets.manpowerCard
        colorize(instance, if (card.target == CardTarget.FIREPOWER) Color(0.95f, 0.38f, 0.82f, 1f) else cardColor(card))
        instance.transform.setToTranslation(card.position)
        modelBatch.render(instance, environment)
    }

    private fun cardColor(card: Card): Color =
        when (card.operation) {
            CardOperation.PLUS -> Color(0.12f, 0.74f, 0.24f, 1f)
            CardOperation.MINUS -> Color(0.88f, 0.24f, 0.22f, 1f)
            CardOperation.TIMES -> Color(0.2f, 0.48f, 0.9f, 1f)
            CardOperation.DIV -> Color(0.9f, 0.66f, 0.14f, 1f)
        }

    private fun colorize(instance: ModelInstance, color: Color) {
        instance.materials.forEach { material ->
            material.set(ColorAttribute.createDiffuse(color))
        }
    }

    fun dispose() {
        assets.dispose()
        modelBatch.dispose()
    }

    private class RenderAssets {
        private val builder = ModelBuilder()
        private val objLoader = ObjLoader()
        private val ownedModels = mutableListOf<Model>()
        private var currentPaths: LevelModelPaths? = null

        val road = instance(box(8f, 0.12f, 220f, Color(0.24f, 0.27f, 0.28f, 1f)))
        val leftRail = instance(box(0.12f, 0.16f, 220f, Color(0.92f, 0.86f, 0.42f, 1f)))
        val rightRail = instance(box(0.12f, 0.16f, 220f, Color(0.92f, 0.86f, 0.42f, 1f)))
        val projectile = instance(sphere(0.18f, Color(1f, 0.9f, 0.2f, 1f)))
        val textBlock = instance(box(0.055f, 0.055f, 0.035f, Color.WHITE))

        var soldier = instance(box(0.36f, 0.8f, 0.36f, Color.WHITE))
            private set
        var boss = instance(box(2.4f, 2.6f, 2.4f, Color.WHITE))
            private set
        var manpowerCard = instance(box(1.2f, 1.2f, 0.22f, Color.WHITE))
            private set
        var firepowerCard = instance(box(1.2f, 1.2f, 0.22f, Color.WHITE))
            private set

        fun useModelPaths(paths: LevelModelPaths) {
            if (paths == currentPaths) return
            currentPaths = paths
            soldier = instance(loadObj(paths.soldier, fallback = { box(0.36f, 0.8f, 0.36f, Color.WHITE) }))
            boss = instance(loadObj(paths.boss, fallback = { box(2.4f, 2.6f, 2.4f, Color.WHITE) }))
            manpowerCard = instance(loadObj(paths.manpowerCard, fallback = { box(1.2f, 1.2f, 0.22f, Color.WHITE) }))
            firepowerCard = instance(loadObj(paths.firepowerCard, fallback = { box(1.2f, 1.2f, 0.22f, Color.WHITE) }))
        }

        private fun loadObj(path: String, fallback: () -> Model): Model {
            val file = resolve(path)
            if (!file.exists()) return fallback()
            return try {
                objLoader.loadModel(file).also(ownedModels::add)
            } catch (_: RuntimeException) {
                fallback()
            }
        }

        private fun resolve(path: String): FileHandle {
            val external = File(path)
            if (external.isAbsolute && external.exists()) return Gdx.files.absolute(path)
            val internal = Gdx.files.internal(path)
            if (internal.exists()) return internal
            return Gdx.files.local(path)
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

        fun dispose() {
            ownedModels.distinct().forEach(Model::dispose)
        }
    }

    private class Text3dRenderer(private val assets: RenderAssets) {
        fun renderCardText(modelBatch: ModelBatch, environment: Environment, card: Card) {
            renderLine(modelBatch, environment, operationLabel(card), card.position.x, card.position.y + 0.31f, card.position.z - 0.135f, 0.075f)
            renderLine(modelBatch, environment, targetTop(card), card.position.x, card.position.y - 0.12f, card.position.z - 0.135f, 0.038f)
            renderLine(modelBatch, environment, "POWER", card.position.x, card.position.y - 0.30f, card.position.z - 0.135f, 0.038f)
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
            val width = upper.sumOf { (glyph(it).firstOrNull()?.length ?: 0) + 1 } - 1
            var cursorX = centerX - width * cell * 0.5f
            upper.forEach { char ->
                val glyph = glyph(char)
                glyph.forEachIndexed { row, bits ->
                    bits.forEachIndexed { col, bit ->
                        if (bit == '1') {
                            assets.textBlock.transform.setToTranslation(
                                cursorX + col * cell,
                                baselineY - row * cell,
                                z
                            )
                            assets.textBlock.transform.scale(cell / 0.055f, cell / 0.055f, 1f)
                            modelBatch.render(assets.textBlock, environment)
                        }
                    }
                }
                cursorX += (glyph.firstOrNull()?.length ?: 0) * cell + cell
            }
        }

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
                'E' to listOf("11111", "10000", "11110", "10000", "11111"),
                'F' to listOf("11111", "10000", "11110", "10000", "10000"),
                'I' to listOf("111", "010", "010", "010", "111"),
                'M' to listOf("10001", "11011", "10101", "10001", "10001"),
                'N' to listOf("10001", "11001", "10101", "10011", "10001"),
                'O' to listOf("01110", "10001", "10001", "10001", "01110"),
                'P' to listOf("11110", "10001", "11110", "10000", "10000"),
                'R' to listOf("11110", "10001", "11110", "10010", "10001"),
                'W' to listOf("10001", "10001", "10101", "11011", "10001"),
                'X' to listOf("10001", "01010", "00100", "01010", "10001")
            )
        }
    }
}
