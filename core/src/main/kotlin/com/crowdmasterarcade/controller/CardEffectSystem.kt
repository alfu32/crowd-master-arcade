package com.crowdmasterarcade.controller

import com.crowdmasterarcade.config.GameConfig
import com.crowdmasterarcade.model.AppModel
import com.crowdmasterarcade.model.AppModelFactory
import com.crowdmasterarcade.model.Card
import com.crowdmasterarcade.model.CardOperation
import com.crowdmasterarcade.model.CardTarget
import com.crowdmasterarcade.model.PlayerBrigade
import com.crowdmasterarcade.model.Road

object CardEffectSystem {
    fun applyCard(appModel: AppModel, card: Card) {
        applyCard(appModel.player, card, appModel.runtimeConfig.maxFireRate, appModel.road.width, appModel)
    }

    fun applyCard(player: PlayerBrigade, card: Card, maxFireRate: Float, roadWidth: Float = GameConfig.ROAD_WIDTH) {
        applyCard(player, card, maxFireRate, roadWidth, appModel = null)
    }

    private fun applyCard(
        player: PlayerBrigade,
        card: Card,
        maxFireRate: Float,
        roadWidth: Float,
        appModel: AppModel?
    ) {
        when (card.target) {
            CardTarget.MANPOWER -> applyManpower(player, card)
            CardTarget.FIREPOWER -> applyFirepower(player, card, maxFireRate)
            CardTarget.BULLET_POWER -> appModel?.let { applyBulletPower(it, card) }
            CardTarget.SOLDIER_LIFE -> applySoldierLife(player, card)
            CardTarget.BULLET_RANGE -> appModel?.let { applyBulletRange(it, card) }
        }
        card.active = false
        val road = Road(roadWidth, 0f, -roadWidth / 2f, roadWidth / 2f)
        FormationSystem.recalculatePlayerFormation(player, road)
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

    private fun applyBulletPower(appModel: AppModel, card: Card) {
        appModel.runtimeConfig.projectileDamage = when (card.operation) {
            CardOperation.PLUS -> appModel.runtimeConfig.projectileDamage + card.value
            CardOperation.MINUS -> appModel.runtimeConfig.projectileDamage - card.value
            CardOperation.TIMES -> appModel.runtimeConfig.projectileDamage * card.value
            CardOperation.DIV -> if (card.value <= 0f) appModel.runtimeConfig.projectileDamage else appModel.runtimeConfig.projectileDamage / card.value
        }.coerceAtLeast(1f)
    }

    private fun applyBulletRange(appModel: AppModel, card: Card) {
        val currentLength = appModel.runtimeConfig.projectileLifeSeconds * appModel.runtimeConfig.projectileSpeed
        val newLength = when (card.operation) {
            CardOperation.PLUS -> currentLength + card.value
            CardOperation.MINUS -> currentLength - card.value
            CardOperation.TIMES -> currentLength * card.value
            CardOperation.DIV -> if (card.value <= 0f) currentLength else currentLength / card.value
        }.coerceAtLeast(5f)
        appModel.runtimeConfig.projectileLifeSeconds = newLength / appModel.runtimeConfig.projectileSpeed
    }

    private fun applySoldierLife(player: PlayerBrigade, card: Card) {
        val oldHealth = player.soldierHealth
        val newHealth = when (card.operation) {
            CardOperation.PLUS -> oldHealth + card.value
            CardOperation.MINUS -> oldHealth - card.value
            CardOperation.TIMES -> oldHealth * card.value
            CardOperation.DIV -> if (card.value <= 0f) oldHealth else oldHealth / card.value
        }.coerceAtLeast(1f)
        val delta = newHealth - oldHealth
        player.soldierHealth = newHealth
        player.soldiers.filter { it.alive }.forEach { soldier ->
            soldier.health = (soldier.health + delta).coerceAtLeast(1f)
        }
    }

    fun addSoldiers(player: PlayerBrigade, count: Int) {
        if (count <= 0) return
        player.soldiers.addAll(AppModelFactory.createSoldiers(count, player.soldierHealth))
    }

    fun removeSoldiers(player: PlayerBrigade, count: Int) {
        repeat(count.coerceAtMost(player.soldiers.size).coerceAtLeast(0)) {
            player.soldiers.removeAt(player.soldiers.lastIndex)
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
