package com.crowdmasterarcade.model

import java.util.Locale

object LevelTextWriter {
    fun write(level: LevelDefinition): String = buildString {
        appendLine("name: ${level.name}")
        appendLine("road_length: ${number(level.roadLength)}")
        appendLine("road_width: ${number(level.roadWidth)}")
        appendLine("starting_soldiers: ${level.startingSoldiers}")
        appendLine("fire_rate: ${number(level.fireRate)}")
        appendLine("projectile_pool: ${level.projectilePool}")
        appendLine("projectile_length: ${number(level.projectileLength)}")
        appendLine("max_fire_rate: ${number(level.maxFireRate)}")
        appendLine("soldier_model: ${level.modelPaths.soldier}")
        appendLine("boss_model: ${level.modelPaths.boss}")
        appendLine("manpower_card_model: ${level.modelPaths.manpowerCard}")
        appendLine("firepower_card_model: ${level.modelPaths.firepowerCard}")
        appendLine("bulletpower_card_model: ${level.modelPaths.bulletPowerCard}")
        appendLine("soldierlife_card_model: ${level.modelPaths.soldierLifeCard}")
        appendLine("bulletrange_card_model: ${level.modelPaths.bulletRangeCard}")
        appendLine("player_color: ${color(level.colors.player)}")
        appendLine("enemy_color: ${color(level.colors.enemy)}")
        appendLine("boss_color: ${color(level.colors.boss)}")
        appendLine("decoration_color: ${color(level.colors.decoration)}")
        appendLine()

        appendLine("cards:")
        level.cards.forEach { card ->
            append("  - op: ${op(card.operation)}, param: ${target(card.target)}, val: ${number(card.value)}, ")
            append("x: ${number(card.x)}, z: ${number(card.z)}")
            card.modelPath?.takeIf { it.isNotBlank() }?.let { append(", model: $it") }
            appendLine()
        }
        appendLine()

        appendLine("decorations:")
        level.decorations.forEach { decoration ->
            append("  - name: ${decoration.name}, power: ${number(decoration.power)}, ")
            append("x: ${number(decoration.x)}, z: ${number(decoration.z)}, model: ${decoration.modelPath}")
            decoration.color?.let { append(", color: ${color(it)}") }
            appendLine()
        }
        appendLine()

        appendLine("background_decorations:")
        level.backgroundDecorations.forEach { decoration ->
            append("  - name: ${decoration.name}, power: ${number(decoration.power)}, ")
            append("x: ${number(decoration.x)}, z: ${number(decoration.z)}, model: ${decoration.modelPath}")
            decoration.color?.let { append(", color: ${color(it)}") }
            appendLine()
        }
        appendLine()

        appendLine("enemy_brigades:")
        level.enemyBrigades.forEach { enemy ->
            enemy.name?.takeIf { it.isNotBlank() }?.let { append("  - name: $it, ") } ?: append("  - ")
            append("effective: ${enemy.effective}, strength: ${number(enemy.unitStrength)}, ")
            append("x: ${number(enemy.x)}, z: ${number(enemy.z)}")
            enemy.modelPath?.takeIf { it.isNotBlank() }?.let { append(", model: $it") }
            enemy.color?.let { append(", color: ${color(it)}") }
            appendLine()
        }
        appendLine()

        appendLine("bosses:")
        level.bosses.forEach { boss ->
            boss.name?.takeIf { it.isNotBlank() }?.let { append("  - name: $it, ") } ?: append("  - ")
            append("power: ${number(boss.power)}, x: ${number(boss.x)}, z: ${number(boss.z)}")
            boss.modelPath?.takeIf { it.isNotBlank() }?.let { append(", model: $it") }
            boss.color?.let { append(", color: ${color(it)}") }
            appendLine()
        }
    }

    fun color(color: LevelColor): String =
        "#${channel(color.red)}${channel(color.green)}${channel(color.blue)}${channel(color.alpha)}"

    private fun channel(value: Float): String =
        ((value.coerceIn(0f, 1f) * 255f) + 0.5f).toInt().toString(16).padStart(2, '0').uppercase()

    private fun number(value: Float): String =
        if (value % 1f == 0f) value.toInt().toString() else String.format(Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')

    private fun op(operation: CardOperation): String =
        when (operation) {
            CardOperation.PLUS -> "plus"
            CardOperation.MINUS -> "minus"
            CardOperation.TIMES -> "times"
            CardOperation.DIV -> "div"
        }

    private fun target(target: CardTarget): String =
        when (target) {
            CardTarget.MANPOWER -> "manpower"
            CardTarget.FIREPOWER -> "firepower"
            CardTarget.BULLET_POWER -> "bulletpower"
            CardTarget.SOLDIER_LIFE -> "soldierlife"
            CardTarget.BULLET_RANGE -> "bulletrange"
        }
}
