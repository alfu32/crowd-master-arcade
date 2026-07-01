package com.crowdmasterarcade.model

object DefaultLevels {
    val ravenBendText: String = """
        name: The Raven's Bend
        road_length: 220
        road_width: 8
        starting_soldiers: 10
        fire_rate: 1.2
        projectile_pool: 768

        cards:
          - op: plus, param: manpower, val: 10, x: -2, z: 28
          - op: minus, param: manpower, val: 5, x: 2, z: 44
          - op: times, param: firepower, val: 2, x: -1.5, z: 60
          - op: div, param: manpower, val: 2, x: 1.5, z: 76
          - op: times, param: firepower, val: 3, x: 0, z: 96

        enemy_brigades:
          - effective: 20, x: 0, z: 88
          - effective: 20, x: 1.4, z: 132

        bosses:
          - power: 400, x: 0, z: 190
          - power: 400, x: 0, z: 380
    """.trimIndent()

    fun ravenBend(): LevelDefinition = LevelTextParser.parse(ravenBendText)
}
