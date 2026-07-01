package com.crowdmasterarcade.view

import com.badlogic.gdx.Gdx
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
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import com.crowdmasterarcade.model.AppModel
import com.crowdmasterarcade.model.Card
import com.crowdmasterarcade.model.CardType

class WorldRenderer {
    private val modelBatch = ModelBatch()
    private val spriteBatch = SpriteBatch()
    private val cardFont = BitmapFont()
    private val camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    private val environment = Environment()
    private val assets = PrimitiveAssets()
    private val labelPosition = Vector3()

    init {
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.62f, 0.62f, 0.62f, 1f))
        environment.add(DirectionalLight().set(0.82f, 0.82f, 0.76f, -0.25f, -0.8f, -0.35f))
        cardFont.color = Color.WHITE
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
    }

    fun render(appModel: AppModel) {
        camera.viewportWidth = Gdx.graphics.width.toFloat()
        camera.viewportHeight = Gdx.graphics.height.toFloat()
        camera.position.set(appModel.player.position.x * 0.35f, 7f, -12f)
        camera.lookAt(appModel.player.position.x * 0.2f, 0f, 15f)
        camera.near = 0.1f
        camera.far = 300f
        camera.update()

        modelBatch.begin(camera)
        renderRoad(appModel)
        renderSoldiers(appModel.player.soldiers, assets.playerSoldier, Color.CYAN)
        appModel.enemyBrigades.filter { it.alive }.forEach {
            renderSoldiers(it.soldiers, assets.enemySoldier, Color.SCARLET)
        }
        appModel.cards.filter { it.active }.forEach(::renderCard)
        appModel.projectiles.filter { it.active }.forEach {
            assets.projectile.transform.setToTranslation(it.position)
            modelBatch.render(assets.projectile, environment)
        }
        if (appModel.boss.active && appModel.boss.alive) {
            assets.boss.transform.setToTranslation(appModel.boss.position)
            modelBatch.render(assets.boss, environment)
        }
        modelBatch.end()
        renderCardLabels(appModel)
    }

    private fun renderRoad(appModel: AppModel) {
        assets.road.transform.setToTranslation(0f, -0.08f, appModel.road.length / 2f)
        modelBatch.render(assets.road, environment)
        assets.leftRail.transform.setToTranslation(appModel.road.leftBoundary - 0.15f, 0.05f, appModel.road.length / 2f)
        assets.rightRail.transform.setToTranslation(appModel.road.rightBoundary + 0.15f, 0.05f, appModel.road.length / 2f)
        modelBatch.render(assets.leftRail, environment)
        modelBatch.render(assets.rightRail, environment)
    }

    private fun renderSoldiers(soldiers: Iterable<com.crowdmasterarcade.model.RegularSoldier>, instance: ModelInstance, color: Color) {
        soldiers.filter { it.alive }.forEach { soldier ->
            instance.transform.setToTranslation(soldier.worldPosition)
            instance.transform.scale(0.75f, 0.95f, 0.75f)
            modelBatch.render(instance, environment)
        }
    }

    private fun renderCard(card: Card) {
        val instance = when (card.type) {
            CardType.ADD -> assets.cardAdd
            CardType.SUBTRACT -> assets.cardSubtract
            CardType.MULTIPLY -> assets.cardMultiply
            CardType.DIVIDE -> assets.cardDivide
            CardType.FIRE_RATE_UP -> assets.cardFireRate
        }
        instance.transform.setToTranslation(card.position)
        modelBatch.render(instance, environment)
    }

    private fun renderCardLabels(appModel: AppModel) {
        spriteBatch.begin()
        cardFont.data.setScale(1.25f)
        appModel.cards.filter { it.active }.forEach { card ->
            labelPosition.set(card.position).add(0f, 0.35f, -0.25f)
            camera.project(labelPosition)
            if (labelPosition.z in 0f..1f) {
                val x = labelPosition.x - 36f
                val y = labelPosition.y + 12f
                cardFont.draw(spriteBatch, operationLabel(card), x, y)
                cardFont.data.setScale(0.72f)
                cardFont.draw(spriteBatch, targetLabel(card), x - 8f, y - 24f)
                cardFont.data.setScale(1.25f)
            }
        }
        spriteBatch.end()
    }

    private fun operationLabel(card: Card): String =
        when (card.type) {
            CardType.ADD -> "+${card.value.toInt()}"
            CardType.SUBTRACT -> "-${card.value.toInt()}"
            CardType.MULTIPLY -> "x${card.value.toInt()}"
            CardType.DIVIDE -> "/${card.value.toInt()}"
            CardType.FIRE_RATE_UP -> "+${card.value.toInt()}"
        }

    private fun targetLabel(card: Card): String =
        when (card.type) {
            CardType.FIRE_RATE_UP -> "FIREPOWER"
            else -> "MANPOWER"
        }

    fun dispose() {
        modelBatch.dispose()
        spriteBatch.dispose()
        cardFont.dispose()
        assets.dispose()
    }

    private class PrimitiveAssets {
        private val builder = ModelBuilder()
        private val models = mutableListOf<Model>()

        val road = instance(box(8f, 0.12f, 220f, Color(0.24f, 0.27f, 0.28f, 1f)))
        val leftRail = instance(box(0.12f, 0.16f, 220f, Color(0.92f, 0.86f, 0.42f, 1f)))
        val rightRail = instance(box(0.12f, 0.16f, 220f, Color(0.92f, 0.86f, 0.42f, 1f)))
        val playerSoldier = instance(box(0.36f, 0.8f, 0.36f, Color(0.12f, 0.72f, 0.92f, 1f)))
        val enemySoldier = instance(box(0.36f, 0.8f, 0.36f, Color(0.84f, 0.16f, 0.18f, 1f)))
        val boss = instance(box(2.4f, 2.6f, 2.4f, Color(0.36f, 0.14f, 0.58f, 1f)))
        val projectile = instance(sphere(0.18f, Color(1f, 0.9f, 0.2f, 1f)))
        val cardAdd = instance(box(1.2f, 1.2f, 0.22f, Color(0.12f, 0.74f, 0.24f, 1f)))
        val cardSubtract = instance(box(1.2f, 1.2f, 0.22f, Color(0.88f, 0.24f, 0.22f, 1f)))
        val cardMultiply = instance(box(1.2f, 1.2f, 0.22f, Color(0.2f, 0.48f, 0.9f, 1f)))
        val cardDivide = instance(box(1.2f, 1.2f, 0.22f, Color(0.9f, 0.66f, 0.14f, 1f)))
        val cardFireRate = instance(box(1.2f, 1.2f, 0.22f, Color(0.95f, 0.38f, 0.82f, 1f)))

        private fun box(width: Float, height: Float, depth: Float, color: Color): Model {
            val model = builder.createBox(
                width,
                height,
                depth,
                Material(ColorAttribute.createDiffuse(color)),
                (Usage.Position or Usage.Normal).toLong()
            )
            models.add(model)
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
            models.add(model)
            return model
        }

        private fun instance(model: Model) = ModelInstance(model)

        fun dispose() {
            models.forEach(Model::dispose)
        }
    }
}
