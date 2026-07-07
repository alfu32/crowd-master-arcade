package com.crowdmasterarcade.model

data class LevelDefinition(
    val name: String,
    val roadLength: Float,
    val roadWidth: Float,
    val startingSoldiers: Int,
    val fireRate: Float,
    val projectilePool: Int,
    val projectileLength: Float,
    val maxFireRate: Float,
    val modelPaths: LevelModelPaths,
    val colors: LevelColors,
    val cards: List<CardDefinition>,
    val decorations: List<DecorationDefinition>,
    val backgroundDecorations: List<BackgroundDecorationDefinition>,
    val enemyBrigades: List<EnemyBrigadeDefinition>,
    val bosses: List<BossDefinition>
)

data class LevelModelPaths(
    val soldier: String,
    val boss: String,
    val manpowerCard: String,
    val firepowerCard: String,
    val bulletPowerCard: String,
    val soldierLifeCard: String,
    val bulletRangeCard: String
)

data class LevelColors(
    val player: LevelColor,
    val enemy: LevelColor,
    val boss: LevelColor,
    val decoration: LevelColor
)

data class LevelColor(
    val red: Float,
    val green: Float,
    val blue: Float,
    val alpha: Float
) {
    companion object {
        val PLAYER = LevelColor(0.12f, 0.72f, 0.92f, 1f)
        val ENEMY = LevelColor(0.84f, 0.16f, 0.18f, 1f)
        val BOSS = LevelColor(0.36f, 0.14f, 0.58f, 1f)
        val DECORATION = LevelColor(0.55f, 0.52f, 0.47f, 1f)
    }
}

data class CardDefinition(
    val operation: CardOperation,
    val target: CardTarget,
    val value: Float,
    val x: Float,
    val z: Float,
    val modelPath: String?
)

data class EnemyBrigadeDefinition(
    val effective: Int,
    val unitStrength: Float,
    val name: String?,
    val x: Float,
    val z: Float,
    val modelPath: String?,
    val color: LevelColor?
)

data class DecorationDefinition(
    val name: String,
    val power: Float,
    val x: Float,
    val z: Float,
    val modelPath: String,
    val color: LevelColor?
)

data class BackgroundDecorationDefinition(
    val name: String,
    val power: Float,
    val x: Float,
    val z: Float,
    val modelPath: String,
    val color: LevelColor?
)

data class BossDefinition(
    val power: Float,
    val name: String?,
    val x: Float,
    val z: Float,
    val modelPath: String?,
    val color: LevelColor?
)
