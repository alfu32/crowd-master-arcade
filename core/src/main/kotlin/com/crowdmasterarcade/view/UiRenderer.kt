package com.crowdmasterarcade.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.crowdmasterarcade.model.AppModel
import com.crowdmasterarcade.model.CardOperation
import com.crowdmasterarcade.model.CardTarget
import com.crowdmasterarcade.model.GameState

class UiRenderer {
    private val batch = SpriteBatch()
    private val font = BitmapFont()

    init {
        font.color = Color.WHITE
        font.data.setScale(1.25f)
    }

    fun render(appModel: AppModel) {
        batch.begin()
        val height = Gdx.graphics.height.toFloat()
        font.draw(batch, "Soldiers: ${appModel.player.soldiers.size}", 20f, height - 20f)
        font.draw(batch, "Fire: ${"%.1f".format(appModel.player.fireRate)}/s", 20f, height - 48f)
        font.draw(batch, "Level: ${appModel.levelData.name}", 20f, height - 76f)
        font.draw(batch, "Bosses: ${appModel.bosses.count { it.alive }}", 20f, height - 104f)
        font.draw(batch, "A/D or arrows move | drag to steer | R restart | Esc quit", 20f, 32f)

        appModel.cards.filter { it.active }.forEach { card ->
            val op = when (card.operation) {
                CardOperation.PLUS -> "+${card.value.toInt()}"
                CardOperation.MINUS -> "-${card.value.toInt()}"
                CardOperation.TIMES -> "x${card.value.toInt()}"
                CardOperation.DIV -> "/${card.value.toInt()}"
            }
            val target = when (card.target) {
                CardTarget.MANPOWER -> "MAN"
                CardTarget.FIREPOWER -> "FIRE"
            }
            font.draw(batch, "$op $target", 20f, height - 140f - card.id % 6 * 22f)
        }

        when (appModel.gameState) {
            GameState.WON -> drawCentered("WON - press R")
            GameState.LOST -> drawCentered("LOST - press R")
            else -> Unit
        }
        batch.end()
    }

    private fun drawCentered(text: String) {
        font.data.setScale(2.5f)
        font.draw(batch, text, Gdx.graphics.width / 2f - 150f, Gdx.graphics.height / 2f)
        font.data.setScale(1.25f)
    }

    fun dispose() {
        batch.dispose()
        font.dispose()
    }
}
