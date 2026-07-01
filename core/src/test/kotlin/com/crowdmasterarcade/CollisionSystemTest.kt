package com.crowdmasterarcade

import com.badlogic.gdx.math.Vector3
import com.crowdmasterarcade.controller.CollisionSystem
import com.crowdmasterarcade.model.AppModelFactory
import com.crowdmasterarcade.model.Card
import com.crowdmasterarcade.model.CardOperation
import com.crowdmasterarcade.model.CardTarget
import kotlin.test.Test
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
            position = Vector3(outerSoldier.worldPosition.x, outerSoldier.worldPosition.y, outerSoldier.worldPosition.z),
            speed = 0f,
            active = true
        )

        CollisionSystem.update(appModel)

        assertFalse(appModel.cards.single().active)
    }
}
