package com.crowdmasterarcade.controller

import com.badlogic.gdx.math.Vector3
import com.crowdmasterarcade.config.GameConfig
import com.crowdmasterarcade.model.AppModel
import com.crowdmasterarcade.model.Boss
import com.crowdmasterarcade.model.GameState
import com.crowdmasterarcade.model.RegularSoldier

object CollisionSystem {
    fun update(appModel: AppModel) {
        handleCards(appModel)
        handleProjectiles(appModel)
        handleEnemyContact(appModel)
        handleBossContact(appModel)
        appModel.enemyBrigades.forEach { enemy ->
            enemy.soldiers.removeAll { !it.alive }
            if (enemy.soldiers.isEmpty()) enemy.alive = false
        }
    }

    private fun handleCards(appModel: AppModel) {
        appModel.cards.filter { it.active }.forEach { card ->
            if (playerFormationOverlapsCard(appModel, card.position)) {
                CardEffectSystem.applyCard(appModel, card)
            }
        }
    }

    private fun playerFormationOverlapsCard(appModel: AppModel, cardPosition: Vector3): Boolean {
        val aliveSoldiers = appModel.player.soldiers.asSequence().filter { it.alive }
        return aliveSoldiers.any { soldier ->
            overlaps(soldier.worldPosition, cardPosition, GameConfig.CARD_COLLISION_RADIUS)
        }
    }

    private fun handleProjectiles(appModel: AppModel) {
        appModel.projectiles.filter { it.active }.forEach { projectile ->
            var consumed = false
            appModel.enemyBrigades.filter { it.alive }.forEach { enemy ->
                if (!consumed) {
                    enemy.soldiers.firstOrNull { soldier ->
                        soldier.alive && overlaps(projectile.position, soldier.worldPosition, GameConfig.PROJECTILE_COLLISION_RADIUS)
                    }?.let { soldier ->
                        val scored = minOf(projectile.damage, soldier.health.coerceAtLeast(0f))
                        soldier.health -= projectile.damage
                        appModel.scoreData.levelPoints += scored
                        if (soldier.health <= 0f) soldier.alive = false
                        projectile.active = false
                        consumed = true
                    }
                }
            }
            if (!consumed) {
                appModel.bosses.firstOrNull { boss ->
                    boss.active && boss.alive && overlapsBossFootprint(
                        projectile.position,
                        boss,
                        GameConfig.PROJECTILE_COLLISION_RADIUS
                    )
                }?.let { boss ->
                    val scored = minOf(projectile.damage, boss.health.coerceAtLeast(0f))
                    boss.health -= projectile.damage
                    appModel.scoreData.levelPoints += scored
                    boss.alive = boss.health > 0f
                    projectile.active = false
                }
            }
        }
    }

    private fun handleEnemyContact(appModel: AppModel) {
        var playerDeaths = false
        appModel.enemyBrigades.filter { it.alive }.forEach { enemy ->
            val contactPairs = contactPairs(appModel.player.soldiers, enemy.soldiers)
            if (contactPairs.isNotEmpty()) {
                contactPairs.forEach { (playerSoldier, enemySoldier) ->
                    playerSoldier.health -= enemy.unitStrength
                    enemySoldier.health -= appModel.player.soldierHealth
                    if (playerSoldier.health <= 0f && playerSoldier.alive) {
                        playerSoldier.alive = false
                        playerDeaths = true
                    }
                    if (enemySoldier.health <= 0f) enemySoldier.alive = false
                }
                enemy.soldiers.removeAll { !it.alive }
                if (enemy.soldiers.isEmpty()) enemy.alive = false
            }
        }
        if (playerDeaths) {
            appModel.player.soldiers.removeAll { !it.alive }
            FormationSystem.recalculatePlayerFormation(appModel.player, appModel.road)
        }
    }

    private fun handleBossContact(appModel: AppModel) {
        appModel.bosses.filter { it.active && it.alive }.forEach { boss ->
            if (overlapsBossFootprint(appModel.player.position, boss, GameConfig.PLAYER_COLLISION_RADIUS)) {
                appModel.player.soldiers.clear()
                appModel.player.alive = false
                appModel.gameState = GameState.LOST
            }
        }
    }

    fun overlaps(a: Vector3, b: Vector3, radius: Float): Boolean = a.dst2(b) <= radius * radius

    private fun contactPairs(
        playerSoldiers: List<RegularSoldier>,
        enemySoldiers: List<RegularSoldier>
    ): List<Pair<RegularSoldier, RegularSoldier>> {
        val alivePlayers = playerSoldiers.filter { it.alive }
        val aliveEnemies = enemySoldiers.filter { it.alive }
        if (alivePlayers.isEmpty() || aliveEnemies.isEmpty()) return emptyList()
        val padding = GameConfig.SOLDIER_SPACING * 0.75f
        if (!boundsOverlap(alivePlayers, aliveEnemies, padding)) return emptyList()
        val radius = GameConfig.SOLDIER_SPACING * 0.72f
        val pairs = mutableListOf<Pair<RegularSoldier, RegularSoldier>>()
        val matchedEnemies = mutableSetOf<Long>()
        alivePlayers.forEach { player ->
            val enemy = aliveEnemies.firstOrNull { enemy ->
                enemy.id !in matchedEnemies && overlaps(player.worldPosition, enemy.worldPosition, radius)
            }
            if (enemy != null) {
                matchedEnemies += enemy.id
                pairs += player to enemy
            }
        }
        return pairs
    }

    private fun boundsOverlap(
        first: List<RegularSoldier>,
        second: List<RegularSoldier>,
        padding: Float
    ): Boolean {
        val firstMinX = first.minOf { it.worldPosition.x } - padding
        val firstMaxX = first.maxOf { it.worldPosition.x } + padding
        val firstMinZ = first.minOf { it.worldPosition.z } - padding
        val firstMaxZ = first.maxOf { it.worldPosition.z } + padding
        val secondMinX = second.minOf { it.worldPosition.x } - padding
        val secondMaxX = second.maxOf { it.worldPosition.x } + padding
        val secondMinZ = second.minOf { it.worldPosition.z } - padding
        val secondMaxZ = second.maxOf { it.worldPosition.z } + padding
        return firstMinX <= secondMaxX &&
            firstMaxX >= secondMinX &&
            firstMinZ <= secondMaxZ &&
            firstMaxZ >= secondMinZ
    }

    private fun overlapsBossFootprint(point: Vector3, boss: Boss, padding: Float): Boolean =
        point.x >= boss.position.x - boss.hitHalfWidth - padding &&
            point.x <= boss.position.x + boss.hitHalfWidth + padding &&
            point.z >= boss.position.z - boss.hitHalfDepth - padding &&
            point.z <= boss.position.z + boss.hitHalfDepth + padding
}
