package com.crowdmasterarcade.controller

import com.badlogic.gdx.math.Vector3
import com.crowdmasterarcade.config.GameConfig
import com.crowdmasterarcade.model.AppModel
import com.crowdmasterarcade.model.GameState

object CollisionSystem {
    fun update(appModel: AppModel) {
        handleCards(appModel)
        handleProjectiles(appModel)
        handleEnemyContact(appModel)
        handleBossContact(appModel)
        appModel.decorations.removeAll { !it.active }
        appModel.enemyBrigades.forEach { enemy ->
            enemy.soldiers.removeAll { !it.alive }
            if (enemy.soldiers.isEmpty()) enemy.alive = false
        }
    }

    private fun handleCards(appModel: AppModel) {
        appModel.cards.filter { it.active }.forEach { card ->
            if (overlaps(appModel.player.position, card.position, GameConfig.PLAYER_COLLISION_RADIUS + GameConfig.CARD_COLLISION_RADIUS)) {
                CardEffectSystem.applyCard(appModel.player, card, appModel.runtimeConfig.maxFireRate, appModel.road.width)
            }
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
                        soldier.health -= projectile.damage
                        if (soldier.health <= 0f) soldier.alive = false
                        projectile.active = false
                        consumed = true
                    }
                }
            }
            if (!consumed) {
                appModel.decorations.firstOrNull { decoration ->
                    decoration.active && overlaps(projectile.position, decoration.position, GameConfig.BOSS_COLLISION_RADIUS)
                }?.let { decoration ->
                    decoration.health -= projectile.damage
                    decoration.active = decoration.health > 0f
                    projectile.active = false
                    consumed = true
                }
            }
            if (!consumed) {
                appModel.bosses.firstOrNull { boss ->
                    boss.active && boss.alive && overlaps(projectile.position, boss.position, GameConfig.BOSS_COLLISION_RADIUS)
                }?.let { boss ->
                    boss.health -= projectile.damage
                    boss.alive = boss.health > 0f
                    projectile.active = false
                }
            }
        }
    }

    private fun handleEnemyContact(appModel: AppModel) {
        appModel.enemyBrigades.filter { it.alive }.forEach { enemy ->
            if (overlaps(appModel.player.position, enemy.position, GameConfig.PLAYER_COLLISION_RADIUS + 1f)) {
                val losses = minOf(enemy.soldiers.size, appModel.player.soldiers.size)
                repeat(losses) { appModel.player.soldiers.removeLast() }
                enemy.soldiers.clear()
                enemy.alive = false
                FormationSystem.recalculatePlayerFormation(appModel.player, appModel.road)
            }
        }
    }

    private fun handleBossContact(appModel: AppModel) {
        appModel.bosses.filter { it.active && it.alive }.forEach { boss ->
            if (overlaps(appModel.player.position, boss.position, GameConfig.PLAYER_COLLISION_RADIUS + GameConfig.BOSS_COLLISION_RADIUS)) {
                appModel.player.soldiers.clear()
                appModel.player.alive = false
                appModel.gameState = GameState.LOST
            }
        }
    }

    fun overlaps(a: Vector3, b: Vector3, radius: Float): Boolean = a.dst2(b) <= radius * radius
}
