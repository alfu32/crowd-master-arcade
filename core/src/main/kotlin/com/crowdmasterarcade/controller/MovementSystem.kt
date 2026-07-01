package com.crowdmasterarcade.controller

import com.crowdmasterarcade.model.AppModel
import com.crowdmasterarcade.model.InputState

object MovementSystem {
    fun update(appModel: AppModel, inputState: InputState, deltaTime: Float) {
        val player = appModel.player
        player.position.x += inputState.moveX.coerceIn(-1f, 1f) * player.lateralSpeed * deltaTime
        player.position.x += inputState.dragDeltaX * 0.025f
        player.position.x = player.position.x.coerceIn(appModel.road.leftBoundary, appModel.road.rightBoundary)

        appModel.cards.filter { it.active }.forEach { it.position.z -= it.speed * deltaTime }
        appModel.enemyBrigades.filter { it.alive }.forEach { it.position.z -= it.speed * deltaTime }
        if (appModel.boss.active && appModel.boss.alive) {
            appModel.boss.position.z -= appModel.boss.speed * deltaTime
        }
    }
}
