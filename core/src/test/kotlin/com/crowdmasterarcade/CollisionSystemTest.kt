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
import kotlin.test.assertTrue

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

    @Test
    fun enemyCenterOverlapDoesNotDamagePlayerWhenSoldiersDoNotTouch() {
        val appModel = AppModelFactory.initAppModel()
        appModel.cards.clear()
        appModel.bosses.clear()
        appModel.projectiles.forEach { it.active = false }
        val enemy = appModel.enemyBrigades.first()
        val initialPlayerCount = appModel.player.soldiers.size
        enemy.position.set(appModel.player.position)
        enemy.soldiers.forEachIndexed { index, soldier ->
            soldier.worldPosition.set(100f + index, soldier.worldPosition.y, 100f)
        }

        CollisionSystem.update(appModel)

        assertEquals(initialPlayerCount, appModel.player.soldiers.size)
        assertTrue(enemy.alive)
    }

    @Test
    fun enemyContactAppliesDamageOnlyToTouchingSoldiers() {
        val appModel = AppModelFactory.initAppModel()
        appModel.cards.clear()
        appModel.bosses.clear()
        appModel.projectiles.forEach { it.active = false }
        val enemy = appModel.enemyBrigades.first()
        val initialEnemyCount = enemy.soldiers.size
        val initialPlayerCount = appModel.player.soldiers.size
        val playerSoldier = appModel.player.soldiers.first()
        val enemySoldier = enemy.soldiers.first()
        playerSoldier.health = enemy.unitStrength
        enemySoldier.health = appModel.player.soldierHealth
        enemy.soldiers.drop(1).forEachIndexed { index, soldier ->
            soldier.worldPosition.set(100f + index, soldier.worldPosition.y, 100f)
        }
        enemySoldier.worldPosition.set(playerSoldier.worldPosition)

        CollisionSystem.update(appModel)

        assertEquals(initialPlayerCount - 1, appModel.player.soldiers.size)
        assertEquals(initialEnemyCount - 1, enemy.soldiers.size)
        assertTrue(enemy.alive)
    }
}
