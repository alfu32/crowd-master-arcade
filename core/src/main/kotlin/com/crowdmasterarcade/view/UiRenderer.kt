package com.crowdmasterarcade.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.crowdmasterarcade.model.AppModel
import com.crowdmasterarcade.model.GameState
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton

class UiRenderer(
    private val onPause: () -> Unit,
    private val onContinue: () -> Unit,
    private val onRetry: () -> Unit,
    private val onMenu: () -> Unit,
    private val onNextLevel: () -> Unit
) {
    val stage = Stage(ScreenViewport())
    private lateinit var root: VisTable
    private lateinit var topBar: VisTable
    private lateinit var hudLabel: VisLabel
    private lateinit var footerLabel: VisLabel
    private lateinit var overlayLabel: VisLabel
    private lateinit var pauseButton: VisTextButton
    private lateinit var centerButtons: VisTable
    private val darkBackground = solidDrawable(Color(0.08f, 0.09f, 0.1f, 0.82f))
    private var centerButtonsState: GameState? = null

    init {
        ensureVisUiLoaded()
        root = VisTable()
        topBar = VisTable()
        hudLabel = VisLabel("")
        footerLabel = VisLabel("A/D left/right | W/S speed | drag to steer/speed | R restart | Esc menu")
        overlayLabel = VisLabel("")
        pauseButton = VisTextButton("Pause")
        centerButtons = VisTable()
        hudLabel.setFontScale(1.65f)
        footerLabel.setFontScale(1.1f)
        overlayLabel.setFontScale(1.65f)
        hudLabel.color = Color.WHITE
        footerLabel.color = Color.WHITE
        overlayLabel.color = Color.WHITE
        topBar.background = darkBackground
        centerButtons.background = darkBackground
        root.setFillParent(true)
        root.top().left().pad(10f)
        stage.addActor(root)

        topBar.add(hudLabel).left().expandX().fillX().pad(8f)
        root.add(topBar).left().top().expandX().fillX().row()
        root.add(pauseButton).right().top().expandX().padTop(8f).width(150f).height(44f).row()
        root.add().expand().fill().row()
        root.add(overlayLabel).center().expandX().padBottom(16f).row()
        root.add(centerButtons).center().expandX().padBottom(120f).row()
        root.add().expand().fill().row()
        root.add(footerLabel).left()

        pauseButton.addChangeListener { onPause() }
    }

    fun render(appModel: AppModel) {
        stage.viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
        hudLabel.setText(hudLine(appModel))
        overlayLabel.setText(overlayText(appModel))
        overlayLabel.isVisible = overlayLabel.text.isNotEmpty()
        pauseButton.isVisible = appModel.gameState == GameState.RUNNING
        rebuildCenterButtons(appModel)
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
                "level ${appModel.levelData.levelNumber}/${appModel.levelData.totalLevels} ${appModel.levelData.name}"
            appModel.gameState == GameState.LOST ->
                "level ${appModel.levelData.levelNumber}/${appModel.levelData.totalLevels} ${appModel.levelData.name}"
            appModel.gameState == GameState.PAUSED ->
                "Paused"
            else -> ""
        }

    private fun rebuildCenterButtons(appModel: AppModel) {
        if (centerButtonsState == appModel.gameState) return
        centerButtonsState = appModel.gameState
        centerButtons.clearChildren()
        val buttons = when (appModel.gameState) {
            GameState.PAUSED -> listOf(
                "Continue" to onContinue,
                "Restart level" to onRetry,
                "Back to menu" to onMenu
            )
            GameState.WON -> listOf(
                "Continue to next level" to onNextLevel,
                "Retry" to onRetry,
                "Return to menu" to onMenu
            )
            GameState.LOST -> listOf(
                "Retry" to onRetry,
                "Return to menu" to onMenu
            )
            else -> emptyList()
        }
        centerButtons.isVisible = buttons.isNotEmpty()
        if (buttons.isEmpty()) return
        buttons.forEach { (text, action) ->
            val button = VisTextButton(text)
            button.label.setFontScale(1.25f)
            button.addChangeListener { action() }
            centerButtons.add(button).width(310f).height(52f).pad(6f).row()
        }
        centerButtons.pack()
    }

    private fun points(value: Float): String =
        value.toInt().toString()

    fun dispose() {
        darkBackground.region.texture.dispose()
        stage.dispose()
    }

    companion object {
        fun ensureVisUiLoaded() {
            if (!VisUI.isLoaded()) VisUI.load()
        }

        private fun solidDrawable(color: Color): TextureRegionDrawable {
            val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
            pixmap.setColor(color)
            pixmap.fill()
            val texture = Texture(pixmap)
            pixmap.dispose()
            return TextureRegionDrawable(TextureRegion(texture))
        }
    }
}

private fun VisTextButton.addChangeListener(action: () -> Unit) {
    addListener(object : ChangeListener() {
        override fun changed(event: ChangeEvent, actor: com.badlogic.gdx.scenes.scene2d.Actor) {
            action()
        }
    })
}
