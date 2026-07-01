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
import com.crowdmasterarcade.view.GameView

class CrowdDefenseGame : ApplicationAdapter() {
    private lateinit var appModel: AppModel
    private lateinit var controller: GameController
    private lateinit var inputController: InputController
    private lateinit var inputState: InputState
    private lateinit var view: GameView

    override fun create() {
        appModel = AppModelFactory.initAppModel()
        controller = GameController()
        inputController = InputController()
        inputState = InputState()
        view = GameView()
    }

    override fun render() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            appModel = AppModelFactory.initAppModel()
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
}
