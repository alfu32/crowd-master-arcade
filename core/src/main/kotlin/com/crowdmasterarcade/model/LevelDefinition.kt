package com.crowdmasterarcade.model

data class LevelDefinition(
    val name: String,
    val roadLength: Float,
    val roadWidth: Float,
    val startingSoldiers: Int,
    val fireRate: Float,
    val projectilePool: Int,
    val modelPaths: LevelModelPaths,
    val cards: List<CardDefinition>,
    val enemyBrigades: List<EnemyBrigadeDefinition>,
    val bosses: List<BossDefinition>
)

data class LevelModelPaths(
    val soldier: String,
    val boss: String,
    val manpowerCard: String,
    val firepowerCard: String
)

data class CardDefinition(
    val operation: CardOperation,
    val target: CardTarget,
    val value: Float,
    val x: Float,
    val z: Float
)

data class EnemyBrigadeDefinition(
    val effective: Int,
    val x: Float,
    val z: Float
)

data class BossDefinition(
    val power: Float,
    val x: Float,
    val z: Float
)
