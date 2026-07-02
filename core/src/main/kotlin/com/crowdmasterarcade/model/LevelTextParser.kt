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
            .withIndex()
            .map { (index, rawLine) -> ParsedLine(index + 1, rawLine, stripComment(rawLine).trim()) }
            .filter { it.text.isNotBlank() }
            .forEach { parsed ->
                val line = parsed.text
                if (line.endsWith(":") && !line.startsWith("-")) {
                    section = line.dropLast(1).trim().canonical()
                    return@forEach
                }

                if (line.startsWith("-")) {
                    val item = parseInlineMap(line.removePrefix("-").trim())
                    try {
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
                                modelPath = item["model"] ?: "assets/default-decoration.obj",
                                color = item.color("color")
                            )
                            "enemy_brigades", "enemies" -> enemies += EnemyBrigadeDefinition(
                                effective = item.int("effective"),
                                unitStrength = item.float("strength", 10f),
                                name = item["name"],
                                x = item.float("x", 0f),
                                z = item.float("z"),
                                modelPath = item["model"],
                                color = item.color("color")
                            )
                            "bosses", "boss" -> bosses += BossDefinition(
                                power = item.float("power"),
                                name = item["name"],
                                x = item.float("x", 0f),
                                z = item.float("z"),
                                modelPath = item["model"],
                                color = item.color("color")
                            )
                        }
                    } catch (exception: RuntimeException) {
                        throw LevelParseException(
                            "Line ${parsed.number} in section '$section': ${exception.message}. Text: ${parsed.raw.trim()}",
                            exception
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
            maxFireRate = values.float("max_fire_rate", GameConfig.MAX_FIRE_RATE),
            modelPaths = LevelModelPaths(
                soldier = values["soldier_model"] ?: "assets/default-soldier.obj",
                boss = values["boss_model"] ?: "assets/default-boss.obj",
                manpowerCard = values["manpower_card_model"] ?: "assets/default-manpower-card.obj",
                firepowerCard = values["firepower_card_model"] ?: "assets/default-firepower-card.obj"
            ),
            colors = LevelColors(
                player = values.color("player_color") ?: LevelColor.PLAYER,
                enemy = values.color("enemy_color") ?: LevelColor.ENEMY,
                boss = values.color("boss_color") ?: LevelColor.BOSS,
                decoration = values.color("decoration_color") ?: LevelColor.DECORATION
            ),
            cards = cards,
            decorations = decorations,
            enemyBrigades = enemies,
            bosses = bosses.ifEmpty { listOf(BossDefinition(400f, null, 0f, 190f, null, null)) }
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

    private fun stripComment(line: String): String {
        val comment = line.indexOf("# ")
        return if (comment >= 0) line.substring(0, comment) else line
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

    private fun Map<String, String>.color(key: String): LevelColor? =
        this[key]?.let(::parseColor)

    private fun parseColor(value: String): LevelColor {
        val hex = value.trim().trim('"').removePrefix("#")
        require(hex.length == 6 || hex.length == 8) {
            "Expected color #RRGGBB or #RRGGBBAA for value: $value"
        }
        val red = hex.substring(0, 2).toInt(16) / 255f
        val green = hex.substring(2, 4).toInt(16) / 255f
        val blue = hex.substring(4, 6).toInt(16) / 255f
        val alpha = if (hex.length == 8) hex.substring(6, 8).toInt(16) / 255f else 1f
        return LevelColor(red, green, blue, alpha)
    }

    private fun String.canonical(): String =
        trimStart('\uFEFF').lowercase().replace("-", "_").replace(" ", "_")

    private data class ParsedLine(
        val number: Int,
        val raw: String,
        val text: String
    )
}

class LevelParseException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)
