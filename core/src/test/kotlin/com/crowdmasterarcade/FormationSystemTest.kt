package com.crowdmasterarcade

import com.crowdmasterarcade.config.GameConfig
import com.crowdmasterarcade.controller.FormationSystem
import com.crowdmasterarcade.model.AppModelFactory
import com.crowdmasterarcade.model.Road
import kotlin.test.assertContentEquals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FormationSystemTest {
    @Test
    fun largeFormationsStayWithinRoadWidthAndGrowByRows() {
        val roadWidth = 8f
        val soldiers = AppModelFactory.createSoldiers(140)

        FormationSystem.recalculateFormation(soldiers, roadWidth)

        val maxAbsX = soldiers.maxOf { kotlin.math.abs(it.localOffset.x) }
        val minZ = soldiers.minOf { it.localOffset.z }

        assertTrue(maxAbsX <= roadWidth / 2f)
        assertTrue(minZ < -GameConfig.SOLDIER_SPACING * 4f)
    }

    @Test
    fun playerFormationIsCappedAtHalfRoadWidth() {
        val road = Road(width = 8f, length = 100f, leftBoundary = -4f, rightBoundary = 4f)

        assertEquals(4f, FormationSystem.playerFormationWidth(0f, road))
    }

    @Test
    fun playerFormationStartsAtAnchorAndExtendsForward() {
        val appModel = AppModelFactory.initAppModel()

        assertEquals(0f, appModel.player.soldiers.minOf { it.localOffset.z })
        assertTrue(appModel.player.soldiers.maxOf { it.localOffset.z } > 0f)
    }

    @Test
    fun enemyFormationWidthAccountsForXPosition() {
        val road = Road(width = 8f, length = 100f, leftBoundary = -4f, rightBoundary = 4f)

        assertEquals(8f, FormationSystem.maxFormationWidthAt(0f, road))
        assertEquals(2f, FormationSystem.maxFormationWidthAt(3f, road))
    }

    @Test
    fun enemyFormationStartsAtAnchorAndExtendsBehindIt() {
        val appModel = AppModelFactory.initAppModel()
        val enemy = appModel.enemyBrigades.first()

        assertEquals(0f, enemy.soldiers.minOf { it.localOffset.z })
        assertTrue(enemy.soldiers.maxOf { it.localOffset.z } > 0f)
    }

    @Test
    fun enemyFormationUpdateDoesNotReconfigureOffsets() {
        val appModel = AppModelFactory.initAppModel()
        val enemy = appModel.enemyBrigades.first()
        val before = enemy.soldiers.map { it.localOffset.cpy() }

        FormationSystem.updateEnemyFormation(enemy, 1f)

        val after = enemy.soldiers.map { it.localOffset }
        assertContentEquals(before, after)
    }

    @Test
    fun playerCenterClampKeepsWholeFormationInsideRoad() {
        val appModel = AppModelFactory.initAppModel()
        appModel.player.position.x = appModel.road.rightBoundary

        appModel.player.position.x = FormationSystem.clampPlayerCenterX(appModel.player, appModel.road)
        FormationSystem.updatePlayerFormation(appModel.player, appModel.road, 1f)

        assertTrue(appModel.player.soldiers.maxOf { it.worldPosition.x } <= appModel.road.rightBoundary)
        assertTrue(appModel.player.soldiers.minOf { it.worldPosition.x } >= appModel.road.leftBoundary)
    }
}
