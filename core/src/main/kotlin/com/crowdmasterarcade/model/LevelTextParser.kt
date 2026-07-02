package com.crowdmasterarcade.model

import com.crowdmasterarcade.config.GameConfig

object LevelTextParser {
    fun parse(text: String): LevelDefinition {
        val values = mutableMapOf<String, String>()
        val cards = mutableListOf<CardDefinition>()
        val decorations = mutableListOf<DecorationDefinition>()
        val enemies = mutableListOf<EnemyBrigadeDefinition>()
        val bosses = mutableListOf<BossDefinition>()
        var section = ""

        text.lineSequence()
            .map { it.substringBefore("#").trim() }
            .filter { it.isNotBlank() }
            .forEach { line ->
                if (line.endsWith(":") && !line.startsWith("-")) {
                    section = line.dropLast(1).trim().canonical()
                    return@forEach
                }

                if (line.startsWith("-")) {
                    val item = parseInlineMap(line.removePrefix("-").trim())
                    when (section) {
                        "cards" -> cards += CardDefinition(
                            operation = parseOperation(item.required("op")),
                            target = parseTarget(item.required("param")),
                            value = item.float("val"),
                            x = item.float("x", 0f),
                            z = item.float("z"),
                            modelPath = item["model"]
                        )
                        "decorations" -> decorations += DecorationDefinition(
                            name = item["name"] ?: "decoration ${decorations.size + 1}",
                            power = item.float("power", 1f),
                            x = item.float("x", 0f),
                            z = item.float("z"),
                            modelPath = item["model"] ?: "assets/default-decoration.obj"
                        )
                        "enemy_brigades", "enemies" -> enemies += EnemyBrigadeDefinition(
                            effective = item.int("effective"),
                            unitStrength = item.float("strength", 10f),
                            name = item["name"],
                            x = item.float("x", 0f),
                            z = item.float("z"),
                            modelPath = item["model"]
                        )
                        "bosses", "boss" -> bosses += BossDefinition(
                            power = item.float("power"),
                            name = item["name"],
                            x = item.float("x", 0f),
                            z = item.float("z"),
                            modelPath = item["model"]
                        )
                    }
                    return@forEach
                }

                val separator = line.indexOf(":")
                if (separator > 0) {
                    values[line.substring(0, separator).trim().canonical()] =
                        line.substring(separator + 1).trim().trim('"')
                }
            }

        return LevelDefinition(
            name = values["name"] ?: "Untitled Level",
            roadLength = values.float("road_length", GameConfig.ROAD_LENGTH),
            roadWidth = values.float("road_width", GameConfig.ROAD_WIDTH),
            startingSoldiers = values.int("starting_soldiers", 10),
            fireRate = values.float("fire_rate", 1.2f),
            projectilePool = values.int("projectile_pool", 768),
            projectileLength = values.float("projectile_length", 80f),
            modelPaths = LevelModelPaths(
                soldier = values["soldier_model"] ?: "assets/default-soldier.obj",
                boss = values["boss_model"] ?: "assets/default-boss.obj",
                manpowerCard = values["manpower_card_model"] ?: "assets/default-manpower-card.obj",
                firepowerCard = values["firepower_card_model"] ?: "assets/default-firepower-card.obj"
            ),
            cards = cards,
            decorations = decorations,
            enemyBrigades = enemies,
            bosses = bosses.ifEmpty { listOf(BossDefinition(400f, null, 0f, 190f, null)) }
        )
    }

    private fun parseInlineMap(text: String): Map<String, String> {
        val cleaned = text.trim().removePrefix("(").removeSuffix(")")
        return cleaned.split(",")
            .mapNotNull { part ->
                val separator = part.indexOf(":")
                if (separator > 0) {
                    return@mapNotNull part.substring(0, separator).trim().canonical() to
                        part.substring(separator + 1).trim().trim('"')
                }
                val trimmed = part.trim()
                val shorthandSeparator = trimmed.indexOf(" ")
                if (shorthandSeparator <= 0) return@mapNotNull null
                trimmed.substring(0, shorthandSeparator).trim().canonical() to
                    trimmed.substring(shorthandSeparator + 1).trim().trim('"')
            }
            .toMap()
    }

    private fun parseOperation(value: String): CardOperation =
        when (value.canonical()) {
            "plus", "add", "+" -> CardOperation.PLUS
            "minus", "subtract", "-" -> CardOperation.MINUS
            "times", "multiply", "x", "*" -> CardOperation.TIMES
            "div", "divide", "/" -> CardOperation.DIV
            else -> error("Unknown card operation: $value")
        }

    private fun parseTarget(value: String): CardTarget =
        when (value.canonical()) {
            "manpower", "soldiers", "soldier" -> CardTarget.MANPOWER
            "firepower", "fire_rate", "firerate" -> CardTarget.FIREPOWER
            else -> error("Unknown card target: $value")
        }

    private fun Map<String, String>.required(key: String): String =
        this[key] ?: error("Missing required level field: $key")

    private fun Map<String, String>.float(key: String, default: Float? = null): Float =
        this[key]?.toFloat() ?: default ?: error("Missing required float level field: $key")

    private fun Map<String, String>.int(key: String, default: Int? = null): Int =
        this[key]?.toInt() ?: default ?: error("Missing required int level field: $key")

    private fun String.canonical(): String =
        lowercase().replace("-", "_").replace(" ", "_")
}
