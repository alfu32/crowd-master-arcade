package com.crowdmasterarcade.controller

import com.crowdmasterarcade.model.AppModelFactory
import com.crowdmasterarcade.model.Card
import com.crowdmasterarcade.model.CardOperation
import com.crowdmasterarcade.model.CardTarget
import com.crowdmasterarcade.model.PlayerBrigade

object CardEffectSystem {
    fun applyCard(player: PlayerBrigade, card: Card, maxFireRate: Float) {
        when (card.target) {
            CardTarget.MANPOWER -> applyManpower(player, card)
            CardTarget.FIREPOWER -> applyFirepower(player, card, maxFireRate)
        }
        card.active = false
        FormationSystem.recalculateFormation(player.soldiers)
    }

    private fun applyManpower(player: PlayerBrigade, card: Card) {
        when (card.operation) {
            CardOperation.PLUS -> addSoldiers(player, card.value.toInt())
            CardOperation.MINUS -> removeSoldiers(player, card.value.toInt())
            CardOperation.TIMES -> multiplySoldiers(player, card.value.toInt())
            CardOperation.DIV -> divideSoldiers(player, card.value.toInt())
        }
    }

    private fun applyFirepower(player: PlayerBrigade, card: Card, maxFireRate: Float) {
        player.fireRate = when (card.operation) {
            CardOperation.PLUS -> player.fireRate + card.value
            CardOperation.MINUS -> player.fireRate - card.value
            CardOperation.TIMES -> player.fireRate * card.value
            CardOperation.DIV -> if (card.value <= 0f) player.fireRate else player.fireRate / card.value
        }.coerceIn(0.1f, maxFireRate)
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
