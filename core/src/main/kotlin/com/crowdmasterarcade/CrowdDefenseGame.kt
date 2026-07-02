package com.crowdmasterarcade

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.crowdmasterarcade.controller.GameController
import com.crowdmasterarcade.controller.InputController
import com.crowdmasterarcade.model.AppModel
import com.crowdmasterarcade.model.AppModelFactory
import com.crowdmasterarcade.model.CampaignLevelContext
import com.crowdmasterarcade.model.CampaignStats
import com.crowdmasterarcade.model.GameState
import com.crowdmasterarcade.model.InputState
import com.crowdmasterarcade.model.LevelCatalog
import com.crowdmasterarcade.model.LevelDefinition
import com.crowdmasterarcade.model.ResourceHome
import com.crowdmasterarcade.view.GameView

class CrowdDefenseGame : ApplicationAdapter() {
    private lateinit var appModel: AppModel
    private lateinit var levels: List<LevelDefinition>
    private lateinit var controller: GameController
    private lateinit var inputController: InputController
    private lateinit var inputState: InputState
    private lateinit var view: GameView
    private lateinit var campaignStats: CampaignStats
    private var levelIndex = 0

    override fun create() {
        ResourceHome.initialize()
        levels = LevelCatalog.load()
        campaignStats = CampaignStats()
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
        if (Gdx.input.isKeyJustPressed(Input.Keys.C) && appModel.gameState == GameState.WON) {
            recordCompletionIfNeeded()
            if (levelIndex + 1 < levels.size) {
                levelIndex += 1
                appModel = loadLevel(levelIndex)
            } else {
                appModel.gameState = GameState.EXIT
                Gdx.app.exit()
            }
            return
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            recordCompletionIfNeeded()
            appModel.gameState = GameState.EXIT
            Gdx.app.exit()
            return
        }

        val previousState = appModel.gameState
        inputController.readInput(inputState)
        controller.changeAppModelState(appModel, inputState, Gdx.graphics.deltaTime)
        if (previousState == GameState.RUNNING && appModel.gameState != GameState.RUNNING) {
            recordCompletionIfNeeded()
        }
        view.presentAppModel(appModel)
    }

    override fun dispose() {
        view.dispose()
    }

    private fun loadLevel(index: Int): AppModel {
        val level = levels[index]
        val levelNumber = index + 1
        val previousTotals = campaignStats.totalsBefore(levelNumber)
        return AppModelFactory.initAppModel(
            level,
            CampaignLevelContext(
                levelNumber = levelNumber,
                totalLevels = levels.size,
                levelPossiblePoints = CampaignLevelContext.possiblePoints(level),
                previousPlayerPoints = previousTotals.playerPoints,
                previousPossiblePoints = previousTotals.possiblePoints
            )
        )
    }

    private fun recordCompletionIfNeeded() {
        if (appModel.completionRecorded || appModel.gameState !in setOf(GameState.WON, GameState.LOST)) return
        campaignStats.record(appModel)
        appModel.completionRecorded = true
    }
}
