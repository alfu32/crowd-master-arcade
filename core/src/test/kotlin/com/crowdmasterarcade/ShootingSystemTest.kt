package com.crowdmasterarcade

import com.crowdmasterarcade.controller.ShootingSystem
import com.crowdmasterarcade.model.AppModelFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class ShootingSystemTest {
    @Test
    fun eachPlayerSoldierFiresAProjectileOnVolley() {
        val appModel = AppModelFactory.initAppModel()
        val soldierCount = appModel.player.soldiers.count { it.alive }

        ShootingSystem.update(appModel, deltaTime = 1f)

        assertEquals(soldierCount, appModel.projectiles.count { it.active })
    }
}
