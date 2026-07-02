package com.crowdmasterarcade.model

import com.badlogic.gdx.math.Vector3
import com.crowdmasterarcade.config.GameConfig
import com.crowdmasterarcade.controller.FormationSystem

object AppModelFactory {
    private var nextId = 1L

    fun initAppModel(
        levelDefinition: LevelDefinition = DefaultLevels.ravenBend(),
        campaignContext: CampaignLevelContext = CampaignLevelContext.singleLevel(levelDefinition)
    ): AppModel {
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
        FormationSystem.recalculatePlayerFormation(player, road)
        FormationSystem.updatePlayerFormation(player, road, 1f)

        val cards = levelDefinition.cards.mapTo(mutableListOf()) {
            card(it, levelDefinition.modelPaths, GameConfig.LEVEL_INTRO_DISTANCE)
        }
        val decorations = levelDefinition.decorations.mapTo(mutableListOf()) { decoration(it, GameConfig.LEVEL_INTRO_DISTANCE) }
        val enemies = levelDefinition.enemyBrigades.mapIndexedTo(mutableListOf()) { index, definition ->
            enemy(definition, index + 1, road, levelDefinition.modelPaths, GameConfig.LEVEL_INTRO_DISTANCE)
        }
        val bosses = levelDefinition.bosses.mapIndexedTo(mutableListOf()) { index, definition ->
            boss(definition, index + 1, levelDefinition.modelPaths, GameConfig.LEVEL_INTRO_DISTANCE)
        }
        val projectileLifeSeconds = levelDefinition.projectileLength / GameConfig.PROJECTILE_SPEED

        return AppModel(
            gameState = GameState.RUNNING,
            player = player,
            enemyBrigades = enemies,
            cards = cards,
            projectiles = MutableList(levelDefinition.projectilePool) { projectile(active = false) },
            decorations = decorations,
            road = road,
            background = Background(theme = "training-ground"),
            bosses = bosses,
            levelData = LevelData(
                name = levelDefinition.name,
                startingSoldiers = levelDefinition.startingSoldiers,
                modelPaths = levelDefinition.modelPaths,
                levelNumber = campaignContext.levelNumber,
                totalLevels = campaignContext.totalLevels
            ),
            runtimeConfig = RuntimeConfig(
                maxFireRate = GameConfig.MAX_FIRE_RATE,
                projectileSpeed = GameConfig.PROJECTILE_SPEED,
                projectileDamage = GameConfig.PROJECTILE_DAMAGE,
                projectileLifeSeconds = projectileLifeSeconds
            ),
            scoreData = ScoreData(
                levelPoints = 0f,
                levelPossiblePoints = campaignContext.levelPossiblePoints,
                previousPlayerPoints = campaignContext.previousPlayerPoints,
                previousPossiblePoints = campaignContext.previousPossiblePoints
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

    private fun card(definition: CardDefinition, levelModels: LevelModelPaths, zOffset: Float): Card =
        Card(
            id = nextId++,
            operation = definition.operation,
            target = definition.target,
            value = definition.value,
            modelPath = definition.modelPath ?: when (definition.target) {
                CardTarget.MANPOWER -> levelModels.manpowerCard
                CardTarget.FIREPOWER -> levelModels.firepowerCard
            },
            position = Vector3(definition.x, 0.7f, definition.z + zOffset),
            speed = GameConfig.CARD_SPEED,
            active = true
        )

    private fun enemy(
        definition: EnemyBrigadeDefinition,
        index: Int,
        road: Road,
        levelModels: LevelModelPaths,
        zOffset: Float
    ): EnemyBrigade {
        val soldiers = createSoldiers(definition.effective)
        soldiers.forEach { it.health = definition.unitStrength }
        val brigade = EnemyBrigade(
            id = nextId++,
            name = definition.name ?: "brigade $index",
            position = Vector3(definition.x, GameConfig.PLAYER_Y, definition.z + zOffset),
            speed = GameConfig.ENEMY_SPEED,
            unitStrength = definition.unitStrength,
            modelPath = definition.modelPath ?: levelModels.soldier,
            soldiers = soldiers,
            alive = true
        )
        FormationSystem.recalculateEnemyFormation(brigade, road)
        FormationSystem.updateEnemyFormation(brigade, 1f)
        return brigade
    }

    private fun decoration(definition: DecorationDefinition, zOffset: Float): Decoration =
        Decoration(
            id = nextId++,
            name = definition.name,
            position = Vector3(definition.x, 0.7f, definition.z + zOffset),
            health = definition.power,
            maxHealth = definition.power,
            modelPath = definition.modelPath,
            active = true
        )

    private fun boss(definition: BossDefinition, index: Int, levelModels: LevelModelPaths, zOffset: Float): Boss =
        Boss(
            name = definition.name ?: "General $index",
            position = Vector3(definition.x, 1.2f, definition.z + zOffset),
            health = definition.power,
            maxHealth = definition.power,
            modelPath = definition.modelPath ?: levelModels.boss,
            speed = GameConfig.BOSS_SPEED,
            active = true,
            alive = true
        )

    private fun projectile(active: Boolean): Projectile =
        Projectile(nextId++, Vector3(), Vector3(), GameConfig.PROJECTILE_DAMAGE, remainingLife = 0f, active)
}

data class CampaignLevelContext(
    val levelNumber: Int,
    val totalLevels: Int,
    val levelPossiblePoints: Float,
    val previousPlayerPoints: Float,
    val previousPossiblePoints: Float
) {
    companion object {
        fun singleLevel(levelDefinition: LevelDefinition): CampaignLevelContext =
            CampaignLevelContext(
                levelNumber = 1,
                totalLevels = 1,
                levelPossiblePoints = possiblePoints(levelDefinition),
                previousPlayerPoints = 0f,
                previousPossiblePoints = 0f
            )

        fun possiblePoints(levelDefinition: LevelDefinition): Float =
            levelDefinition.enemyBrigades.sumOf { it.effective.toDouble() * it.unitStrength.toDouble() }.toFloat() +
                levelDefinition.bosses.sumOf { it.power.toDouble() }.toFloat()
    }
}
