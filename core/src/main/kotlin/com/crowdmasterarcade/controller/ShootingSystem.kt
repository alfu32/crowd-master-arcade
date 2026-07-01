package com.crowdmasterarcade.controller

import com.badlogic.gdx.math.Vector3
import com.crowdmasterarcade.config.GameConfig
import com.crowdmasterarcade.model.AppModel
import com.crowdmasterarcade.model.EnemyBrigade

object ShootingSystem {
    fun update(appModel: AppModel, deltaTime: Float) {
        val player = appModel.player
        player.fireCooldown -= deltaTime
        if (player.fireCooldown > 0f || !player.alive || player.soldiers.isEmpty()) return

        var fired = false
        player.soldiers.asSequence().filter { it.alive }.forEach { soldier ->
            val target = findNearestTarget(appModel, soldier.worldPosition) ?: return@forEach
            val projectile = appModel.projectiles.firstOrNull { !it.active } ?: return@forEach
            projectile.position.set(soldier.worldPosition).add(0f, 0.45f, 0.35f)
            projectile.velocity.set(TEMP.set(target).sub(projectile.position).nor().scl(appModel.runtimeConfig.projectileSpeed))
            projectile.damage = appModel.runtimeConfig.projectileDamage
            projectile.active = true
            fired = true
        }

        if (!fired) return
        player.fireCooldown = 1f / player.fireRate.coerceAtLeast(0.1f)
    }

    fun updateProjectiles(appModel: AppModel, deltaTime: Float) {
        appModel.projectiles.filter { it.active }.forEach { projectile ->
            projectile.position.mulAdd(projectile.velocity, deltaTime)
            if (projectile.position.z > appModel.road.length || projectile.position.z < -10f) {
                projectile.active = false
            }
        }
    }

    private fun findNearestTarget(appModel: AppModel, origin: Vector3): Vector3? {
        var nearest: Vector3? = null
        var nearestDst = Float.MAX_VALUE
        appModel.enemyBrigades.filter { it.alive }.forEach { enemy: EnemyBrigade ->
            enemy.soldiers.firstOrNull { it.alive }?.let { soldier ->
                val dst = origin.dst2(soldier.worldPosition)
                if (dst < nearestDst && soldier.worldPosition.z > GameConfig.PLAYER_Z) {
                    nearestDst = dst
                    nearest = soldier.worldPosition
                }
            }
        }
        appModel.bosses.filter { it.active && it.alive }.forEach { boss ->
            val dst = origin.dst2(boss.position)
            if (dst < nearestDst && boss.position.z > GameConfig.PLAYER_Z) {
                nearestDst = dst
                nearest = boss.position
            }
        }
        return nearest
    }

    private val TEMP = Vector3()
}
