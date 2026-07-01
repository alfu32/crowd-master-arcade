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
        zDirection: Float = -1f
    ) {
        val count = soldiers.size
        if (count == 0) return
        val usableWidth = (roadWidth - GameConfig.SOLDIER_SPACING).coerceAtLeast(GameConfig.SOLDIER_SPACING)
        val columns = ceil(usableWidth / GameConfig.SOLDIER_SPACING).toInt().coerceAtLeast(1)
        soldiers.forEachIndexed { index, soldier ->
            val row = index / columns
            val column = index % columns
            val rowWidth = minOf(columns, count - row * columns)
            val x = (column - (rowWidth - 1) / 2f) * GameConfig.SOLDIER_SPACING
            val z = row * GameConfig.SOLDIER_SPACING * zDirection
            soldier.localOffset.set(x, 0f, z)
        }
    }

    fun recalculatePlayerFormation(player: PlayerBrigade, road: Road) {
        recalculateFormation(player.soldiers, playerFormationWidth(player.position.x, road))
    }

    fun recalculateEnemyFormation(enemy: EnemyBrigade, road: Road) {
        recalculateFormation(enemy.soldiers, maxFormationWidthAt(enemy.position.x, road), zDirection = 1f)
    }

    fun updatePlayerFormation(player: PlayerBrigade, road: Road, alpha: Float) {
        recalculatePlayerFormation(player, road)
        updateFormation(player.soldiers, player.position, alpha)
    }

    fun updateEnemyFormation(enemy: EnemyBrigade, alpha: Float) {
        updateFormation(enemy.soldiers, enemy.position, alpha)
    }

    fun playerFormationWidth(centerX: Float, road: Road): Float =
        min(road.width / 2f, maxFormationWidthAt(centerX, road))

    fun maxFormationWidthAt(centerX: Float, road: Road): Float {
        val leftSpace = centerX - road.leftBoundary
        val rightSpace = road.rightBoundary - centerX
        return (2f * min(leftSpace, rightSpace))
            .coerceIn(GameConfig.SOLDIER_SPACING, road.width)
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
