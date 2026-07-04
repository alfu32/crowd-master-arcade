package com.crowdmasterarcade.controller

import com.crowdmasterarcade.config.GameConfig
import com.crowdmasterarcade.model.AppModel
import com.crowdmasterarcade.model.GameState
import com.crowdmasterarcade.model.InputState

class GameController {
    fun changeAppModelState(appModel: AppModel, inputState: InputState, deltaTime: Float) {
        if (appModel.gameState != GameState.RUNNING) return

        val inputStep = deltaTime.coerceIn(0f, GameConfig.MAX_DELTA_TIME)
        appModel.runtimeConfig.gameSpeed = (
            appModel.runtimeConfig.gameSpeed +
                inputState.speedDelta * GameConfig.GAME_SPEED_CHANGE_RATE * inputStep
            ).coerceIn(GameConfig.MIN_GAME_SPEED, GameConfig.MAX_GAME_SPEED)
        val step = inputStep * appModel.runtimeConfig.gameSpeed
        MovementSystem.update(appModel, inputState, step)
        FormationSystem.updatePlayerFormation(appModel.player, appModel.road, 0.28f)
        ShootingSystem.update(appModel, step)
        ShootingSystem.updateProjectiles(appModel, step)
        CollisionSystem.update(appModel)
        LevelSystem.updateGameState(appModel)

        inputState.dragDeltaX = 0f
        inputState.dragDeltaY = 0f
        inputState.speedDelta = 0f
    }
}
