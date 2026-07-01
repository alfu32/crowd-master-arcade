package com.crowdmasterarcade

import com.badlogic.gdx.math.Vector3
import com.crowdmasterarcade.config.GameConfig
import com.crowdmasterarcade.controller.CardEffectSystem
import com.crowdmasterarcade.model.AppModelFactory
import com.crowdmasterarcade.model.Card
import com.crowdmasterarcade.model.CardType
import com.crowdmasterarcade.model.PlayerBrigade
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CardEffectSystemTest {
    @Test
    fun subtractNeverDropsBelowZero() {
        val player = player(3)
        val card = card(CardType.SUBTRACT, 10f)
        CardEffectSystem.applyCard(player, card, GameConfig.MAX_FIRE_RATE)

        assertEquals(0, player.soldiers.size)
        assertFalse(player.alive)
        assertFalse(card.active)
    }

    @Test
    fun multiplyAddsFormationSoldiers() {
        val player = player(4)
        CardEffectSystem.applyCard(player, card(CardType.MULTIPLY, 3f), GameConfig.MAX_FIRE_RATE)

        assertEquals(12, player.soldiers.size)
    }

    @Test
    fun fireRateIsCapped() {
        val player = player(4)
        player.fireRate = 7.8f
        CardEffectSystem.applyCard(player, card(CardType.FIRE_RATE_UP, 5f), GameConfig.MAX_FIRE_RATE)

        assertEquals(GameConfig.MAX_FIRE_RATE, player.fireRate)
    }

    private fun player(count: Int) = PlayerBrigade(
        position = Vector3(),
        lateralSpeed = GameConfig.PLAYER_LATERAL_SPEED,
        soldiers = AppModelFactory.createSoldiers(count),
        fireRate = 1f,
        fireCooldown = 0f,
        alive = true
    )

    private fun card(type: CardType, value: Float) = Card(1L, type, value, Vector3(), 1f, true)
}
