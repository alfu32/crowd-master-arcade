package com.crowdmasterarcade

import com.badlogic.gdx.math.Vector3
import com.crowdmasterarcade.config.GameConfig
import com.crowdmasterarcade.controller.CardEffectSystem
import com.crowdmasterarcade.model.AppModelFactory
import com.crowdmasterarcade.model.Card
import com.crowdmasterarcade.model.CardOperation
import com.crowdmasterarcade.model.CardTarget
import com.crowdmasterarcade.model.LevelColor
import com.crowdmasterarcade.model.PlayerBrigade
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CardEffectSystemTest {
    @Test
    fun subtractNeverDropsBelowZero() {
        val player = player(3)
        val card = card(CardOperation.MINUS, CardTarget.MANPOWER, 10f)
        CardEffectSystem.applyCard(player, card, GameConfig.MAX_FIRE_RATE)

        assertEquals(0, player.soldiers.size)
        assertFalse(player.alive)
        assertFalse(card.active)
    }

    @Test
    fun multiplyAddsFormationSoldiers() {
        val player = player(4)
        CardEffectSystem.applyCard(player, card(CardOperation.TIMES, CardTarget.MANPOWER, 3f), GameConfig.MAX_FIRE_RATE)

        assertEquals(12, player.soldiers.size)
    }

    @Test
    fun fireRateIsCappedByRuntimeMax() {
        val player = player(4)
        player.fireRate = 7.8f
        CardEffectSystem.applyCard(player, card(CardOperation.PLUS, CardTarget.FIREPOWER, 5f), GameConfig.MAX_FIRE_RATE)

        assertEquals(GameConfig.MAX_FIRE_RATE, player.fireRate)
    }

    @Test
    fun fireRateCanBeMultiplied() {
        val player = player(4)
        player.fireRate = 1.5f
        CardEffectSystem.applyCard(player, card(CardOperation.TIMES, CardTarget.FIREPOWER, 2f), GameConfig.MAX_FIRE_RATE)

        assertEquals(3f, player.fireRate)
    }

    @Test
    fun soldierLifeUpdatesCurrentAndFutureSoldiers() {
        val player = player(2)
        CardEffectSystem.applyCard(player, card(CardOperation.PLUS, CardTarget.SOLDIER_LIFE, 5f), GameConfig.MAX_FIRE_RATE)
        CardEffectSystem.applyCard(player, card(CardOperation.PLUS, CardTarget.MANPOWER, 1f), GameConfig.MAX_FIRE_RATE)

        assertEquals(15f, player.soldierHealth)
        assertEquals(listOf(15f, 15f, 15f), player.soldiers.map { it.health })
    }

    @Test
    fun bulletPowerUpdatesProjectileDamage() {
        val appModel = AppModelFactory.initAppModel()
        CardEffectSystem.applyCard(appModel, card(CardOperation.TIMES, CardTarget.BULLET_POWER, 3f))

        assertEquals(GameConfig.PROJECTILE_DAMAGE * 3f, appModel.runtimeConfig.projectileDamage)
    }

    private fun player(count: Int) = PlayerBrigade(
        position = Vector3(),
        lateralSpeed = GameConfig.PLAYER_LATERAL_SPEED,
        soldiers = AppModelFactory.createSoldiers(count),
        soldierHealth = GameConfig.SOLDIER_HEALTH,
        fireRate = 1f,
        fireCooldown = 0f,
        alive = true,
        color = LevelColor.PLAYER
    )

    private fun card(operation: CardOperation, target: CardTarget, value: Float) =
        Card(1L, operation, target, value, "assets/default-manpower-card.obj", Vector3(), 1f, true)
}
