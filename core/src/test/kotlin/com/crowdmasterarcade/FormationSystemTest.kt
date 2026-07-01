package com.crowdmasterarcade

import com.crowdmasterarcade.config.GameConfig
import com.crowdmasterarcade.controller.FormationSystem
import com.crowdmasterarcade.model.AppModelFactory
import kotlin.test.Test
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
}
