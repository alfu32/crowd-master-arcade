package com.crowdmasterarcade.controller

import com.crowdmasterarcade.model.AppModelFactory
import com.crowdmasterarcade.model.Card
import com.crowdmasterarcade.model.CardType
import com.crowdmasterarcade.model.PlayerBrigade

object CardEffectSystem {
    fun applyCard(player: PlayerBrigade, card: Card, maxFireRate: Float) {
        when (card.type) {
            CardType.ADD -> addSoldiers(player, card.value.toInt())
            CardType.SUBTRACT -> removeSoldiers(player, card.value.toInt())
            CardType.MULTIPLY -> multiplySoldiers(player, card.value.toInt())
            CardType.DIVIDE -> divideSoldiers(player, card.value.toInt())
            CardType.FIRE_RATE_UP -> player.fireRate = (player.fireRate + card.value).coerceAtMost(maxFireRate)
        }
        card.active = false
        FormationSystem.recalculateFormation(player.soldiers)
    }

    fun addSoldiers(player: PlayerBrigade, count: Int) {
        if (count <= 0) return
        player.soldiers.addAll(AppModelFactory.createSoldiers(count))
    }

    fun removeSoldiers(player: PlayerBrigade, count: Int) {
        repeat(count.coerceAtMost(player.soldiers.size).coerceAtLeast(0)) {
            player.soldiers.removeLast()
        }
        if (player.soldiers.isEmpty()) player.alive = false
    }

    fun multiplySoldiers(player: PlayerBrigade, multiplier: Int) {
        val target = player.soldiers.size * multiplier.coerceAtLeast(0)
        addSoldiers(player, target - player.soldiers.size)
    }

    fun divideSoldiers(player: PlayerBrigade, divisor: Int) {
        if (divisor <= 0) return
        val target = player.soldiers.size / divisor
        removeSoldiers(player, player.soldiers.size - target)
    }
}
