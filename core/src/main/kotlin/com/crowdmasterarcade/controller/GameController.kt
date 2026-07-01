package com.crowdmasterarcade.controller

import com.crowdmasterarcade.config.GameConfig
import com.crowdmasterarcade.model.AppModel
import com.crowdmasterarcade.model.GameState
import com.crowdmasterarcade.model.InputState

class GameController {
    fun changeAppModelState(appModel: AppModel, inputState: InputState, deltaTime: Float) {
        if (appModel.gameState != GameState.RUNNING) return

        val step = deltaTime.coerceIn(0f, GameConfig.MAX_DELTA_TIME)
        MovementSystem.update(appModel, inputState, step)
        FormationSystem.updatePlayerFormation(appModel.player, 0.28f)
        appModel.enemyBrigades.filter { it.alive }.forEach {
            FormationSystem.updateEnemyFormation(it, 0.28f)
        }
        ShootingSystem.update(appModel, step)
        ShootingSystem.updateProjectiles(appModel, step)
        CollisionSystem.update(appModel)
        LevelSystem.updateGameState(appModel)

        inputState.dragDeltaX = 0f
    }
}
