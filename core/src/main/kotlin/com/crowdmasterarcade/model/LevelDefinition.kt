package com.crowdmasterarcade.model

data class LevelDefinition(
    val name: String,
    val roadLength: Float,
    val roadWidth: Float,
    val startingSoldiers: Int,
    val fireRate: Float,
    val projectilePool: Int,
    val projectileLength: Float,
    val modelPaths: LevelModelPaths,
    val cards: List<CardDefinition>,
    val decorations: List<DecorationDefinition>,
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
    val unitStrength: Float,
    val name: String?,
    val x: Float,
    val z: Float
)

data class DecorationDefinition(
    val name: String,
    val power: Float,
    val x: Float,
    val z: Float,
    val modelPath: String
)

data class BossDefinition(
    val power: Float,
    val name: String?,
    val x: Float,
    val z: Float
)
