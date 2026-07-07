package com.crowdmasterarcade.controller

import com.crowdmasterarcade.config.GameConfig
import com.crowdmasterarcade.model.AppModel
import com.crowdmasterarcade.model.InputState

object MovementSystem {
    fun update(appModel: AppModel, inputState: InputState, deltaTime: Float) {
        val player = appModel.player
        player.position.x += inputState.moveX.coerceIn(-1f, 1f) * player.lateralSpeed * deltaTime
        player.position.x += inputState.dragDeltaX * 0.025f
        player.position.x = FormationSystem.clampPlayerCenterX(player, appModel.road)

        val scrollDistance = GameConfig.WORLD_SCROLL_SPEED * deltaTime
        val dz = -scrollDistance
        if (appModel.introRoadPosition < 0f) {
            appModel.introRoadPosition = (appModel.introRoadPosition + scrollDistance).coerceAtMost(0f)
        }
        appModel.cards.filter { it.active }.forEach { it.position.z += dz }
        appModel.decorations.filter { it.active }.forEach { it.position.z += dz }
        appModel.enemyBrigades.filter { it.alive }.forEach { enemy ->
            enemy.position.z += dz
            enemy.soldiers.filter { it.alive }.forEach { it.worldPosition.z += dz }
        }
        appModel.bosses.filter { it.active && it.alive }.forEach { boss ->
            boss.position.z += dz
        }
    }
}
