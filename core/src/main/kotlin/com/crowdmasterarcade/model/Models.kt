package com.crowdmasterarcade.model

import com.badlogic.gdx.math.Vector3

data class AppModel(
    var gameState: GameState,
    val player: PlayerBrigade,
    val enemyBrigades: MutableList<EnemyBrigade>,
    val cards: MutableList<Card>,
    val projectiles: MutableList<Projectile>,
    val decorations: MutableList<Decoration>,
    val backgroundDecorations: MutableList<Decoration>,
    val road: Road,
    val background: Background,
    val bosses: MutableList<Boss>,
    val levelData: LevelData,
    val runtimeConfig: RuntimeConfig,
    val scoreData: ScoreData,
    var introRoadPosition: Float = -10f,
    var completionRecorded: Boolean = false
) {
    val running: Boolean
        get() = gameState != GameState.EXIT

    val boss: Boss
        get() = bosses.firstOrNull { it.active && it.alive } ?: bosses.first()
}

data class PlayerBrigade(
    var position: Vector3,
    var lateralSpeed: Float,
    var soldiers: MutableList<RegularSoldier>,
    var soldierHealth: Float,
    var fireRate: Float,
    var fireCooldown: Float,
    var alive: Boolean,
    val color: LevelColor,
    val formationSpacing: Float
)

data class EnemyBrigade(
    val id: Long,
    val name: String,
    var position: Vector3,
    var speed: Float,
    var unitStrength: Float,
    val modelPath: String,
    val color: LevelColor,
    var soldiers: MutableList<RegularSoldier>,
    var alive: Boolean,
    val formationSpacing: Float
)

data class RegularSoldier(
    val id: Long,
    var localOffset: Vector3,
    var worldPosition: Vector3,
    var health: Float,
    var alive: Boolean
)

data class Card(
    val id: Long,
    val operation: CardOperation,
    val target: CardTarget,
    val value: Float,
    val modelPath: String,
    var position: Vector3,
    var speed: Float,
    var active: Boolean
)

data class Projectile(
    val id: Long,
    var position: Vector3,
    var velocity: Vector3,
    var damage: Float,
    var remainingLife: Float,
    var active: Boolean
)

data class Decoration(
    val id: Long,
    val name: String,
    var position: Vector3,
    var health: Float,
    val maxHealth: Float,
    val modelPath: String,
    val color: LevelColor,
    var active: Boolean
)

data class Road(
    val width: Float,
    val length: Float,
    val leftBoundary: Float,
    val rightBoundary: Float
)

data class Background(
    val theme: String
)

data class Boss(
    val name: String,
    var position: Vector3,
    var health: Float,
    val maxHealth: Float,
    val modelPath: String,
    val color: LevelColor,
    val hitHalfWidth: Float,
    val hitHalfDepth: Float,
    var speed: Float,
    var active: Boolean,
    var alive: Boolean
)

data class RuntimeConfig(
    val maxFireRate: Float,
    val projectileSpeed: Float,
    var projectileDamage: Float,
    var projectileLifeSeconds: Float,
    var gameSpeed: Float = 1f
)

data class LevelData(
    val name: String,
    val startingSoldiers: Int,
    val modelPaths: LevelModelPaths,
    val levelNumber: Int,
    val totalLevels: Int
)

data class ScoreData(
    var levelPoints: Float,
    val levelPossiblePoints: Float,
    val previousPlayerPoints: Float,
    val previousPossiblePoints: Float
) {
    val totalPlayerPoints: Float
        get() = previousPlayerPoints + levelPoints

    val totalPossiblePointsSoFar: Float
        get() = previousPossiblePoints + levelPossiblePoints
}

data class InputState(
    var moveX: Float = 0f,
    var speedDelta: Float = 0f,
    var dragging: Boolean = false,
    var dragDeltaX: Float = 0f,
    var dragDeltaY: Float = 0f
)
