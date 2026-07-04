package com.crowdmasterarcade

import com.badlogic.gdx.math.Vector3
import com.crowdmasterarcade.controller.CollisionSystem
import com.crowdmasterarcade.model.AppModelFactory
import com.crowdmasterarcade.model.Card
import com.crowdmasterarcade.model.CardOperation
import com.crowdmasterarcade.model.CardTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CollisionSystemTest {
    @Test
    fun playerCollectsCardTouchedByOuterSoldier() {
        val appModel = AppModelFactory.initAppModel()
        appModel.cards.clear()
        val outerSoldier = appModel.player.soldiers.maxBy { it.worldPosition.x }
        appModel.cards += Card(
            id = 999L,
            operation = CardOperation.PLUS,
            target = CardTarget.MANPOWER,
            value = 1f,
            modelPath = "assets/default-manpower-card.obj",
            position = Vector3(outerSoldier.worldPosition.x, outerSoldier.worldPosition.y, outerSoldier.worldPosition.z),
            speed = 0f,
            active = true
        )

        CollisionSystem.update(appModel)

        assertFalse(appModel.cards.single().active)
    }

    @Test
    fun projectileScoresActualDamageAgainstEnemyUnit() {
        val appModel = AppModelFactory.initAppModel()
        appModel.cards.clear()
        appModel.bosses.clear()
        val enemy = appModel.enemyBrigades.first()
        val soldier = enemy.soldiers.first()
        soldier.health = 6f
        val projectile = appModel.projectiles.first()
        projectile.position.set(soldier.worldPosition)
        projectile.damage = 10f
        projectile.active = true

        CollisionSystem.update(appModel)

        assertEquals(6f, appModel.scoreData.levelPoints)
        assertFalse(projectile.active)
        assertFalse(soldier.alive)
    }
}
