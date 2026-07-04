package com.crowdmasterarcade

import com.crowdmasterarcade.controller.ShootingSystem
import com.crowdmasterarcade.model.AppModelFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ShootingSystemTest {
    @Test
    fun eachPlayerSoldierFiresAProjectileOnVolley() {
        val appModel = AppModelFactory.initAppModel()
        val soldierCount = appModel.player.soldiers.count { it.alive }

        ShootingSystem.update(appModel, deltaTime = 1f)

        assertEquals(soldierCount, appModel.projectiles.count { it.active })
    }

    @Test
    fun projectilesFireStraightForwardAndExpireByLevelLife() {
        val appModel = AppModelFactory.initAppModel()

        ShootingSystem.update(appModel, deltaTime = 1f)
        val projectile = appModel.projectiles.first { it.active }

        assertEquals(0f, projectile.velocity.x)
        assertEquals(0f, projectile.velocity.y)
        assertEquals(appModel.runtimeConfig.projectileSpeed, projectile.velocity.z)
        assertEquals(appModel.runtimeConfig.projectileLifeSeconds, projectile.remainingLife)

        ShootingSystem.updateProjectiles(appModel, appModel.runtimeConfig.projectileLifeSeconds)

        assertFalse(projectile.active)
    }
}
