package com.crowdmasterarcade.model

import com.badlogic.gdx.math.Vector3
import com.crowdmasterarcade.config.GameConfig
import com.crowdmasterarcade.controller.FormationSystem

object AppModelFactory {
    private var nextId = 1L

    fun initAppModel(levelDefinition: LevelDefinition = DefaultLevels.ravenBend()): AppModel {
        nextId = 1L
        val road = Road(
            width = levelDefinition.roadWidth,
            length = levelDefinition.roadLength,
            leftBoundary = -levelDefinition.roadWidth / 2f,
            rightBoundary = levelDefinition.roadWidth / 2f
        )
        val player = PlayerBrigade(
            position = Vector3(0f, GameConfig.PLAYER_Y, GameConfig.PLAYER_Z),
            lateralSpeed = GameConfig.PLAYER_LATERAL_SPEED,
            soldiers = createSoldiers(levelDefinition.startingSoldiers),
            fireRate = levelDefinition.fireRate,
            fireCooldown = 0f,
            alive = true
        )
        FormationSystem.recalculateFormation(player.soldiers)
        FormationSystem.updatePlayerFormation(player, 1f)

        val cards = levelDefinition.cards.mapTo(mutableListOf()) { card(it) }
        val enemies = levelDefinition.enemyBrigades.mapTo(mutableListOf()) { enemy(it.effective, it.x, it.z) }
        val bosses = levelDefinition.bosses.mapTo(mutableListOf()) { boss(it.power, it.x, it.z) }

        return AppModel(
            gameState = GameState.RUNNING,
            player = player,
            enemyBrigades = enemies,
            cards = cards,
            projectiles = MutableList(levelDefinition.projectilePool) { projectile(active = false) },
            road = road,
            background = Background(theme = "training-ground"),
            bosses = bosses,
            levelData = LevelData(
                name = levelDefinition.name,
                startingSoldiers = levelDefinition.startingSoldiers,
                modelPaths = levelDefinition.modelPaths
            ),
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

    private fun card(definition: CardDefinition): Card =
        Card(
            id = nextId++,
            operation = definition.operation,
            target = definition.target,
            value = definition.value,
            position = Vector3(definition.x, 0.7f, definition.z),
            speed = GameConfig.CARD_SPEED,
            active = true
        )

    private fun enemy(count: Int, x: Float, z: Float): EnemyBrigade {
        val brigade = EnemyBrigade(nextId++, Vector3(x, GameConfig.PLAYER_Y, z), GameConfig.ENEMY_SPEED, createSoldiers(count), alive = true)
        FormationSystem.recalculateFormation(brigade.soldiers)
        FormationSystem.updateEnemyFormation(brigade, 1f)
        return brigade
    }

    private fun boss(power: Float, x: Float, z: Float): Boss =
        Boss(
            position = Vector3(x, 1.2f, z),
            health = power,
            speed = GameConfig.BOSS_SPEED,
            active = true,
            alive = true
        )

    private fun projectile(active: Boolean): Projectile =
        Projectile(nextId++, Vector3(), Vector3(), GameConfig.PROJECTILE_DAMAGE, active)
}
