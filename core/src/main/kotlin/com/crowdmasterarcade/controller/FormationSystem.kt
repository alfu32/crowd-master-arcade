package com.crowdmasterarcade.controller

import com.badlogic.gdx.math.Vector3
import com.crowdmasterarcade.config.GameConfig
import com.crowdmasterarcade.model.EnemyBrigade
import com.crowdmasterarcade.model.PlayerBrigade
import com.crowdmasterarcade.model.RegularSoldier
import com.crowdmasterarcade.model.Road
import kotlin.math.ceil
import kotlin.math.min

object FormationSystem {
    fun recalculateFormation(
        soldiers: MutableList<RegularSoldier>,
        roadWidth: Float = GameConfig.ROAD_WIDTH,
        zDirection: Float = -1f,
        spacing: Float = GameConfig.SOLDIER_SPACING
    ) {
        val count = soldiers.size
        if (count == 0) return
        val unitSpacing = spacing.coerceAtLeast(GameConfig.SOLDIER_SPACING)
        val usableWidth = (roadWidth - unitSpacing).coerceAtLeast(unitSpacing)
        val columns = ceil(usableWidth / unitSpacing).toInt().coerceAtLeast(1)
        soldiers.forEachIndexed { index, soldier ->
            val row = index / columns
            val column = index % columns
            val rowWidth = minOf(columns, count - row * columns)
            val x = (column - (rowWidth - 1) / 2f) * unitSpacing
            val z = row * unitSpacing * zDirection
            soldier.localOffset.set(x, 0f, z)
        }
    }

    fun recalculatePlayerFormation(player: PlayerBrigade, road: Road) {
        recalculateFormation(
            player.soldiers,
            playerFormationWidth(player.position.x, road, player.formationSpacing),
            zDirection = 1f,
            spacing = player.formationSpacing
        )
    }

    fun recalculateEnemyFormation(enemy: EnemyBrigade, road: Road) {
        recalculateFormation(
            enemy.soldiers,
            maxFormationWidthAt(enemy.position.x, road, enemy.formationSpacing),
            zDirection = 1f,
            spacing = enemy.formationSpacing
        )
    }

    fun updatePlayerFormation(player: PlayerBrigade, road: Road, alpha: Float) {
        recalculatePlayerFormation(player, road)
        updateFormation(player.soldiers, player.position, alpha)
    }

    fun updateEnemyFormation(enemy: EnemyBrigade, alpha: Float) {
        updateFormation(enemy.soldiers, enemy.position, alpha)
    }

    fun clampPlayerCenterX(player: PlayerBrigade, road: Road): Float {
        val aliveSoldiers = player.soldiers.filter { it.alive }
        if (aliveSoldiers.isEmpty()) return player.position.x.coerceIn(road.leftBoundary, road.rightBoundary)
        val minOffsetX = aliveSoldiers.minOf { it.localOffset.x }
        val maxOffsetX = aliveSoldiers.maxOf { it.localOffset.x }
        val minCenterX = road.leftBoundary - minOffsetX
        val maxCenterX = road.rightBoundary - maxOffsetX
        return if (minCenterX <= maxCenterX) {
            player.position.x.coerceIn(minCenterX, maxCenterX)
        } else {
            ((road.leftBoundary + road.rightBoundary) * 0.5f)
        }
    }

    fun playerFormationWidth(centerX: Float, road: Road, minimumWidth: Float = GameConfig.SOLDIER_SPACING): Float =
        min(road.width / 2f, maxFormationWidthAt(centerX, road, minimumWidth))

    fun maxFormationWidthAt(centerX: Float, road: Road, minimumWidth: Float = GameConfig.SOLDIER_SPACING): Float {
        val leftSpace = centerX - road.leftBoundary
        val rightSpace = road.rightBoundary - centerX
        return (2f * min(leftSpace, rightSpace))
            .coerceIn(minimumWidth.coerceAtLeast(GameConfig.SOLDIER_SPACING), road.width)
    }

    private fun updateFormation(soldiers: MutableList<RegularSoldier>, center: Vector3, alpha: Float) {
        soldiers.forEach { soldier ->
            if (soldier.alive) {
                soldier.worldPosition.lerp(TEMP.set(center).add(soldier.localOffset), alpha.coerceIn(0f, 1f))
            }
        }
    }

    private val TEMP = Vector3()
}
