package com.crowdmasterarcade.controller

import com.crowdmasterarcade.model.AppModel
import com.crowdmasterarcade.model.GameState

object LevelSystem {
    fun updateGameState(appModel: AppModel) {
        if (!appModel.player.alive || appModel.player.soldiers.isEmpty()) {
            appModel.player.alive = false
            appModel.gameState = GameState.LOST
            return
        }
        if (!appModel.boss.alive) {
            appModel.gameState = GameState.WON
            return
        }
        val allEnemiesCleared = appModel.enemyBrigades.none { it.alive }
        val allCardsCleared = appModel.cards.none { it.active }
        if (allEnemiesCleared && allCardsCleared && !appModel.boss.active) {
            appModel.gameState = GameState.WON
        }
    }
}
