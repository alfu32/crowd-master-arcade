package com.crowdmasterarcade

import com.crowdmasterarcade.controller.CollisionSystem
import com.crowdmasterarcade.model.AppModelFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DecorationCollisionTest {
    @Test
    fun projectilesIgnoreDecorations() {
        val appModel = AppModelFactory.initAppModel()
        val decoration = appModel.decorations.first()
        val projectile = appModel.projectiles.first()
        projectile.position.set(decoration.position)
        projectile.active = true
        projectile.damage = 100f

        CollisionSystem.update(appModel)

        assertTrue(projectile.active)
        assertTrue(decoration.active)
        assertEquals(decoration.maxHealth, decoration.health)
    }
}
