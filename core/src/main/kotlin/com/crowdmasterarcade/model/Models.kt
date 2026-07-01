package com.crowdmasterarcade.model

import com.badlogic.gdx.math.Vector3

data class AppModel(
    var gameState: GameState,
    val player: PlayerBrigade,
    val enemyBrigades: MutableList<EnemyBrigade>,
    val cards: MutableList<Card>,
    val projectiles: MutableList<Projectile>,
    val road: Road,
    val background: Background,
    val boss: Boss,
    val levelData: LevelData,
    val runtimeConfig: RuntimeConfig
) {
    val running: Boolean
        get() = gameState != GameState.EXIT
}

data class PlayerBrigade(
    var position: Vector3,
    var lateralSpeed: Float,
    var soldiers: MutableList<RegularSoldier>,
    var fireRate: Float,
    var fireCooldown: Float,
    var alive: Boolean
)

data class EnemyBrigade(
    val id: Long,
    var position: Vector3,
    var speed: Float,
    var soldiers: MutableList<RegularSoldier>,
    var alive: Boolean
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
    val type: CardType,
    val value: Float,
    var position: Vector3,
    var speed: Float,
    var active: Boolean
)

data class Projectile(
    val id: Long,
    var position: Vector3,
    var velocity: Vector3,
    var damage: Float,
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
    var position: Vector3,
    var health: Float,
    var speed: Float,
    var active: Boolean,
    var alive: Boolean
)

data class RuntimeConfig(
    val maxFireRate: Float,
    val projectileSpeed: Float,
    val projectileDamage: Float
)

data class LevelData(
    val name: String,
    val startingSoldiers: Int
)

data class InputState(
    var moveX: Float = 0f,
    var dragging: Boolean = false,
    var dragDeltaX: Float = 0f
)
