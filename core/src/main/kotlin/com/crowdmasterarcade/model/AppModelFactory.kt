package com.crowdmasterarcade.model

import com.badlogic.gdx.math.Vector3
import com.crowdmasterarcade.config.GameConfig
import com.crowdmasterarcade.controller.FormationSystem

object AppModelFactory {
    private var nextId = 1L

    fun initAppModel(): AppModel {
        nextId = 1L
        val road = Road(
            width = GameConfig.ROAD_WIDTH,
            length = GameConfig.ROAD_LENGTH,
            leftBoundary = -GameConfig.ROAD_WIDTH / 2f,
            rightBoundary = GameConfig.ROAD_WIDTH / 2f
        )
        val player = PlayerBrigade(
            position = Vector3(0f, GameConfig.PLAYER_Y, GameConfig.PLAYER_Z),
            lateralSpeed = GameConfig.PLAYER_LATERAL_SPEED,
            soldiers = createSoldiers(10),
            fireRate = 1.2f,
            fireCooldown = 0f,
            alive = true
        )
        FormationSystem.recalculateFormation(player.soldiers)
        FormationSystem.updatePlayerFormation(player, 1f)

        val cards = mutableListOf(
            card(CardType.MULTIPLY, 2f, -2f, 28f),
            card(CardType.ADD, 15f, 2f, 44f),
            card(CardType.SUBTRACT, 5f, -1.5f, 60f),
            card(CardType.DIVIDE, 2f, 1.5f, 76f),
            card(CardType.FIRE_RATE_UP, 1f, 0f, 96f)
        )
        val enemies = mutableListOf(
            enemy(18, 0f, 88f),
            enemy(28, 1.4f, 132f)
        )

        return AppModel(
            gameState = GameState.RUNNING,
            player = player,
            enemyBrigades = enemies,
            cards = cards,
            projectiles = MutableList(768) { projectile(active = false) },
            road = road,
            background = Background(theme = "training-ground"),
            boss = Boss(
                position = Vector3(0f, 1.2f, 190f),
                health = 500f,
                speed = GameConfig.BOSS_SPEED,
                active = true,
                alive = true
            ),
            levelData = LevelData(name = "Prototype Run", startingSoldiers = 10),
            runtimeConfig = RuntimeConfig(
                maxFireRate = GameConfig.MAX_FIRE_RATE,
                projectileSpeed = GameConfig.PROJECTILE_SPEED,
                projectileDamage = GameConfig.PROJECTILE_DAMAGE
            )
        )
    }

    fun createSoldiers(count: Int): MutableList<RegularSoldier> =
        MutableList(count.coerceAtLeast(0)) {
            RegularSoldier(
                id = nextId++,
                localOffset = Vector3(),
                worldPosition = Vector3(),
                health = GameConfig.SOLDIER_HEALTH,
                alive = true
            )
        }

    private fun card(type: CardType, value: Float, x: Float, z: Float): Card =
        Card(nextId++, type, value, Vector3(x, 0.7f, z), GameConfig.CARD_SPEED, active = true)

    private fun enemy(count: Int, x: Float, z: Float): EnemyBrigade {
        val brigade = EnemyBrigade(nextId++, Vector3(x, GameConfig.PLAYER_Y, z), GameConfig.ENEMY_SPEED, createSoldiers(count), alive = true)
        FormationSystem.recalculateFormation(brigade.soldiers)
        FormationSystem.updateEnemyFormation(brigade, 1f)
        return brigade
    }

    private fun projectile(active: Boolean): Projectile =
        Projectile(nextId++, Vector3(), Vector3(), GameConfig.PROJECTILE_DAMAGE, active)
}
