package com.crowdmasterarcade.controller

import com.crowdmasterarcade.config.GameConfig
import com.crowdmasterarcade.model.AppModel
import com.crowdmasterarcade.model.EnemyBrigade

object ShootingSystem {
    fun update(appModel: AppModel, deltaTime: Float) {
        val player = appModel.player
        player.fireCooldown -= deltaTime
        if (player.fireCooldown > 0f || !player.alive || player.soldiers.isEmpty()) return
        if (!hasTargetAhead(appModel)) return

        var fired = false
        player.soldiers.asSequence().filter { it.alive }.forEach { soldier ->
            val projectile = appModel.projectiles.firstOrNull { !it.active } ?: return@forEach
            projectile.position.set(soldier.worldPosition).add(0f, 0.45f, 0.35f)
            projectile.velocity.set(0f, 0f, appModel.runtimeConfig.projectileSpeed)
            projectile.damage = appModel.runtimeConfig.projectileDamage
            projectile.remainingLife = appModel.runtimeConfig.projectileLifeSeconds
            projectile.active = true
            fired = true
        }

        if (!fired) return
        player.fireCooldown = 1f / player.fireRate.coerceAtLeast(0.1f)
    }

    fun updateProjectiles(appModel: AppModel, deltaTime: Float) {
        appModel.projectiles.filter { it.active }.forEach { projectile ->
            projectile.remainingLife -= deltaTime
            projectile.position.mulAdd(projectile.velocity, deltaTime)
            if (projectile.remainingLife <= 0f || projectile.position.z > appModel.road.length || projectile.position.z < -10f) {
                projectile.active = false
            }
        }
    }

    private fun hasTargetAhead(appModel: AppModel): Boolean {
        appModel.enemyBrigades.filter { it.alive }.forEach { enemy: EnemyBrigade ->
            enemy.soldiers.firstOrNull { it.alive }?.let { soldier ->
                if (soldier.worldPosition.z > GameConfig.PLAYER_Z) return true
            }
        }
        appModel.bosses.filter { it.active && it.alive }.forEach { boss ->
            if (boss.position.z > GameConfig.PLAYER_Z) return true
        }
        return false
    }
}
