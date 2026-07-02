package com.crowdmasterarcade.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.crowdmasterarcade.model.AppModel
import com.crowdmasterarcade.model.GameState

class UiRenderer {
    private val batch = SpriteBatch()
    private val font = BitmapFont()
    private val layout = GlyphLayout()
    private val panelTexture = createPanelTexture()

    init {
        font.color = Color.WHITE
        font.data.setScale(1.05f)
    }

    fun render(appModel: AppModel) {
        batch.begin()
        batch.setColor(1f, 1f, 1f, 1f)
        val height = Gdx.graphics.height.toFloat()
        drawPanel(12f, height - 116f, 520f, 104f)
        drawPanel(12f, 12f, 500f, 36f)
        drawText("Level ${appModel.levelData.levelNumber}/${appModel.levelData.totalLevels}: ${appModel.levelData.name}", 24f, height - 28f, 1.08f)
        drawText("Soldiers ${appModel.player.soldiers.size}", 24f, height - 56f, 1.0f)
        drawText("Fire ${"%.1f".format(appModel.player.fireRate)}/s", 184f, height - 56f, 1.0f)
        drawText("Score ${points(appModel.scoreData.levelPoints)}/${points(appModel.scoreData.levelPossiblePoints)}", 24f, height - 84f, 1.0f)
        drawText("Total ${points(appModel.scoreData.totalPlayerPoints)}/${points(appModel.scoreData.totalPossiblePointsSoFar)}", 246f, height - 84f, 1.0f)
        drawText("A/D or arrows move | drag to steer | R restart | Esc quit", 24f, 36f, 0.95f)

        if (appModel.gameState == GameState.RUNNING && appModel.introRoadPosition >= -5f && appModel.introRoadPosition < 0f) {
            drawCentered(
                "level\n${appModel.levelData.levelNumber}/${appModel.levelData.totalLevels} ${appModel.levelData.name}",
                scale = 2.15f
            )
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

    private fun drawPanel(x: Float, y: Float, width: Float, height: Float) {
        batch.setColor(1f, 1f, 1f, 1f)
        batch.draw(panelTexture, x, y, width, height)
    }

    private fun drawText(text: String, x: Float, y: Float, scale: Float) {
        font.data.setScale(scale)
        font.color = Color.WHITE
        font.draw(batch, text, x, y)
    }

    private fun drawCentered(text: String, scale: Float = 1.7f) {
        font.data.setScale(scale)
        font.color = Color.WHITE
        layout.setText(font, text)
        val padding = 26f
        val panelWidth = layout.width + padding * 2f
        val panelHeight = layout.height + padding * 2f
        drawPanel(
            Gdx.graphics.width / 2f - panelWidth / 2f,
            Gdx.graphics.height / 2f - panelHeight / 2f,
            panelWidth,
            panelHeight
        )
        font.draw(
            batch,
            text,
            Gdx.graphics.width / 2f - layout.width / 2f,
            Gdx.graphics.height / 2f + layout.height / 2f
        )
        font.data.setScale(1.05f)
    }

    private fun points(value: Float): String =
        value.toInt().toString()

    fun dispose() {
        batch.dispose()
        font.dispose()
        panelTexture.dispose()
    }

    private fun createPanelTexture(): Texture {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(0f, 0f, 0f, 0.58f)
        pixmap.fill()
        val texture = Texture(pixmap)
        pixmap.dispose()
        return texture
    }
}
