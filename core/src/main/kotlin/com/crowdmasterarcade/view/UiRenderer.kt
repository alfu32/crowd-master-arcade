package com.crowdmasterarcade.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.crowdmasterarcade.model.AppModel
import com.crowdmasterarcade.model.GameState
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable

class UiRenderer {
    val stage = Stage(ScreenViewport())
    private val root = VisTable()
    private val hudLabel = VisLabel("")
    private val footerLabel = VisLabel("A/D left/right | W/S speed | drag to steer/speed | R restart | Esc quit")
    private val overlayLabel = VisLabel("")

    init {
        ensureVisUiLoaded()
        root.setFillParent(true)
        root.top().left()
        stage.addActor(root)

        root.add(hudLabel).left().pad(10f).expandX().fillX().row()
        root.add().expand().fill().row()
        root.add(overlayLabel).center().padBottom(180f).row()
        root.add().expand().fill().row()
        root.add(footerLabel).left().pad(10f)
    }

    fun render(appModel: AppModel) {
        stage.viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
        hudLabel.setText(hudLine(appModel))
        overlayLabel.setText(overlayText(appModel))
        overlayLabel.isVisible = overlayLabel.text.isNotEmpty()
        stage.act(Gdx.graphics.deltaTime)
        stage.draw()
    }

    private fun hudLine(appModel: AppModel): String =
        "level ${appModel.levelData.levelNumber} ${appModel.levelData.name} " +
            "soldiers:${appModel.player.soldiers.size} " +
            "fire:${"%.1f".format(appModel.player.fireRate)} " +
            "bullet caliber:${points(appModel.runtimeConfig.projectileDamage)} " +
            "life:${points(appModel.player.soldierHealth)} " +
            "speed:${"%.1f".format(appModel.runtimeConfig.gameSpeed)} " +
            "score:${points(appModel.scoreData.levelPoints)}/${points(appModel.scoreData.levelPossiblePoints)}"

    private fun overlayText(appModel: AppModel): String =
        when {
            appModel.gameState == GameState.RUNNING &&
                appModel.introRoadPosition >= -5f &&
                appModel.introRoadPosition < 0f ->
                "level\n${appModel.levelData.levelNumber}/${appModel.levelData.totalLevels} ${appModel.levelData.name}"
            appModel.gameState == GameState.WON ->
                "level ${appModel.levelData.levelNumber}/${appModel.levelData.totalLevels} ${appModel.levelData.name}\n" +
                    "press c to continue to the next level,r to restart curent level,ESC to exit"
            appModel.gameState == GameState.LOST ->
                "level ${appModel.levelData.levelNumber}/${appModel.levelData.totalLevels} ${appModel.levelData.name}\n" +
                    "press r to restart curent level,ESC to exit"
            else -> ""
        }

    private fun points(value: Float): String =
        value.toInt().toString()

    fun dispose() {
        stage.dispose()
    }

    companion object {
        fun ensureVisUiLoaded() {
            if (!VisUI.isLoaded()) VisUI.load()
        }
    }
}
