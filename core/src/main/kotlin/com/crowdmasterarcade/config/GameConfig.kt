package com.crowdmasterarcade.config

object GameConfig {
    const val PLAYER_Z = 0f
    const val PLAYER_Y = 0.4f
    const val ROAD_WIDTH = 8f
    const val ROAD_LENGTH = 220f
    const val PLAYER_LATERAL_SPEED = 8f
    const val WORLD_SCROLL_SPEED = 3.2f
    const val CARD_SPEED = 6f
    const val ENEMY_SPEED = 3.2f
    const val BOSS_SPEED = 1.6f
    const val PROJECTILE_SPEED = 26f
    const val PROJECTILE_DAMAGE = 10f
    const val SOLDIER_HEALTH = 10f
    const val SOLDIER_SPACING = 0.55f
    const val PLAYER_COLLISION_RADIUS = 1.3f
    const val CARD_COLLISION_RADIUS = 1.0f
    const val PROJECTILE_COLLISION_RADIUS = 0.55f
    const val BOSS_COLLISION_RADIUS = 1.8f
    const val MAX_FIRE_RATE = 8f
    const val MAX_DELTA_TIME = 1f / 20f
    const val LEVEL_INTRO_DISTANCE = 10f
    const val MIN_GAME_SPEED = 0.35f
    const val MAX_GAME_SPEED = 3.5f
    const val GAME_SPEED_CHANGE_RATE = 1.8f
}
