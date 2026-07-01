package com.crowdmasterarcade.controller

import com.crowdmasterarcade.config.GameConfig
import com.crowdmasterarcade.model.AppModel
import com.crowdmasterarcade.model.InputState

object MovementSystem {
    fun update(appModel: AppModel, inputState: InputState, deltaTime: Float) {
        val player = appModel.player
        player.position.x += inputState.moveX.coerceIn(-1f, 1f) * player.lateralSpeed * deltaTime
        player.position.x += inputState.dragDeltaX * 0.025f
        player.position.x = player.position.x.coerceIn(appModel.road.leftBoundary, appModel.road.rightBoundary)

        appModel.cards.filter { it.active }.forEach { it.position.z -= it.speed * deltaTime }
        appModel.decorations.filter { it.active }.forEach { it.position.z -= GameConfig.BOSS_SPEED * deltaTime }
        appModel.enemyBrigades.filter { it.alive }.forEach { it.position.z -= it.speed * deltaTime }
        appModel.bosses.filter { it.active && it.alive }.forEach { boss ->
            boss.position.z -= boss.speed * deltaTime
        }
    }
}
