package com.crowdmasterarcade.controller

import com.badlogic.gdx.math.Vector3
import com.crowdmasterarcade.config.GameConfig
import com.crowdmasterarcade.model.EnemyBrigade
import com.crowdmasterarcade.model.PlayerBrigade
import com.crowdmasterarcade.model.RegularSoldier
import kotlin.math.ceil
import kotlin.math.sqrt

object FormationSystem {
    fun recalculateFormation(soldiers: MutableList<RegularSoldier>) {
        val count = soldiers.size
        if (count == 0) return
        val columns = ceil(sqrt(count.toFloat())).toInt().coerceAtLeast(1)
        soldiers.forEachIndexed { index, soldier ->
            val row = index / columns
            val column = index % columns
            val rowWidth = minOf(columns, count - row * columns)
            val x = (column - (rowWidth - 1) / 2f) * GameConfig.SOLDIER_SPACING
            val z = -row * GameConfig.SOLDIER_SPACING
            soldier.localOffset.set(x, 0f, z)
        }
    }

    fun updatePlayerFormation(player: PlayerBrigade, alpha: Float) {
        updateFormation(player.soldiers, player.position, alpha)
    }

    fun updateEnemyFormation(enemy: EnemyBrigade, alpha: Float) {
        updateFormation(enemy.soldiers, enemy.position, alpha)
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
