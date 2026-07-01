package com.crowdmasterarcade

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.crowdmasterarcade.controller.GameController
import com.crowdmasterarcade.controller.InputController
import com.crowdmasterarcade.model.AppModel
import com.crowdmasterarcade.model.AppModelFactory
import com.crowdmasterarcade.model.GameState
import com.crowdmasterarcade.model.InputState
import com.crowdmasterarcade.model.LevelCatalog
import com.crowdmasterarcade.model.LevelDefinition
import com.crowdmasterarcade.view.GameView

class CrowdDefenseGame : ApplicationAdapter() {
    private lateinit var appModel: AppModel
    private lateinit var levels: List<LevelDefinition>
    private lateinit var controller: GameController
    private lateinit var inputController: InputController
    private lateinit var inputState: InputState
    private lateinit var view: GameView
    private var levelIndex = 0

    override fun create() {
        levels = LevelCatalog.load()
        appModel = loadLevel(levelIndex)
        controller = GameController()
        inputController = InputController()
        inputState = InputState()
        view = GameView()
    }

    override fun render() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            appModel = loadLevel(levelIndex)
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.N)) {
            levelIndex = (levelIndex + 1) % levels.size
            appModel = loadLevel(levelIndex)
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            appModel.gameState = GameState.EXIT
            Gdx.app.exit()
            return
        }

        inputController.readInput(inputState)
        controller.changeAppModelState(appModel, inputState, Gdx.graphics.deltaTime)
        view.presentAppModel(appModel)
    }

    override fun dispose() {
        view.dispose()
    }

    private fun loadLevel(index: Int): AppModel =
        AppModelFactory.initAppModel(levels[index])
}
