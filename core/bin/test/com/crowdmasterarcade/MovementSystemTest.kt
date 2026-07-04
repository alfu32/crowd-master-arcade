package com.crowdmasterarcade

import com.crowdmasterarcade.controller.MovementSystem
import com.crowdmasterarcade.model.AppModelFactory
import com.crowdmasterarcade.model.InputState
import kotlin.test.Test
import kotlin.test.assertEquals

class MovementSystemTest {
    @Test
    fun worldObjectsPreserveLevelZOrderingOnSingleConveyor() {
        val appModel = AppModelFactory.initAppModel()
        val enemy = appModel.enemyBrigades.first()
        val boss = appModel.bosses.first()
        enemy.position.z = 110f
        enemy.speed = 99f
        boss.position.z = 102f
        boss.speed = 1f
        val relativeZ = enemy.position.z - boss.position.z

        MovementSystem.update(appModel, InputState(), deltaTime = 0.5f)

        assertEquals(relativeZ, enemy.position.z - boss.position.z, absoluteTolerance = 0.0001f)
    }

    @Test
    fun enemySoldiersTranslateWithFormationAnchorWithoutLerpDrift() {
        val appModel = AppModelFactory.initAppModel()
        val enemy = appModel.enemyBrigades.first()
        val beforeAnchorZ = enemy.position.z
        val beforeSoldierZ = enemy.soldiers.first().worldPosition.z

        MovementSystem.update(appModel, InputState(), deltaTime = 0.5f)

        val anchorDelta = enemy.position.z - beforeAnchorZ
        val soldierDelta = enemy.soldiers.first().worldPosition.z - beforeSoldierZ
        assertEquals(anchorDelta, soldierDelta, absoluteTolerance = 0.0001f)
    }
}
