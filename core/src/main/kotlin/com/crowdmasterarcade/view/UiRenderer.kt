package com.crowdmasterarcade.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.crowdmasterarcade.model.AppModel
import com.crowdmasterarcade.model.CardOperation
import com.crowdmasterarcade.model.CardTarget
import com.crowdmasterarcade.model.GameState

class UiRenderer {
    private val batch = SpriteBatch()
    private val font = BitmapFont()
    private val layout = GlyphLayout()

    init {
        font.color = Color.WHITE
        font.data.setScale(1.25f)
    }

    fun render(appModel: AppModel) {
        batch.begin()
        val height = Gdx.graphics.height.toFloat()
        font.draw(batch, "Soldiers: ${appModel.player.soldiers.size}", 20f, height - 20f)
        font.draw(batch, "Fire: ${"%.1f".format(appModel.player.fireRate)}/s", 20f, height - 48f)
        font.draw(batch, "Level: ${appModel.levelData.levelNumber}/${appModel.levelData.totalLevels} ${appModel.levelData.name}", 20f, height - 76f)
        font.draw(batch, "Score: ${points(appModel.scoreData.levelPoints)}/${points(appModel.scoreData.levelPossiblePoints)}", 20f, height - 104f)
        font.draw(batch, "Total: ${points(appModel.scoreData.totalPlayerPoints)}/${points(appModel.scoreData.totalPossiblePointsSoFar)}", 20f, height - 132f)
        font.draw(batch, "A/D or arrows move | drag to steer | R restart | Esc quit", 20f, 32f)

        if (appModel.gameState == GameState.RUNNING && appModel.introRoadPosition >= -5f && appModel.introRoadPosition < 0f) {
            drawCentered(
                "level\n${appModel.levelData.levelNumber}/${appModel.levelData.totalLevels} ${appModel.levelData.name}",
                scale = 2.15f
            )
        }

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
            GameState.WON -> drawCentered(
                "level ${appModel.levelData.levelNumber}/${appModel.levelData.totalLevels} ${appModel.levelData.name}\n" +
                    "press c to continue to the next level,r to restart curent level,ESC to exit"
            )
            GameState.LOST -> drawCentered(
                "level ${appModel.levelData.levelNumber}/${appModel.levelData.totalLevels} ${appModel.levelData.name}\n" +
                    "press r to restart curent level,ESC to exit"
            )
            else -> Unit
        }
        batch.end()
    }

    private fun drawCentered(text: String, scale: Float = 1.7f) {
        font.data.setScale(scale)
        layout.setText(font, text)
        font.draw(
            batch,
            text,
            Gdx.graphics.width / 2f - layout.width / 2f,
            Gdx.graphics.height / 2f + layout.height / 2f
        )
        font.data.setScale(1.25f)
    }

    private fun points(value: Float): String =
        value.toInt().toString()

    fun dispose() {
        batch.dispose()
        font.dispose()
    }
}
