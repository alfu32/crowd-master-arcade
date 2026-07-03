package com.crowdmasterarcade.model

import com.badlogic.gdx.files.FileHandle
import com.crowdmasterarcade.config.GameConfig
import kotlin.math.max

object ModelFootprintCatalog {
    private val cache = mutableMapOf<String, ModelFootprint>()

    fun footprint(path: String, fallbackHalfWidth: Float = GameConfig.BOSS_COLLISION_RADIUS): ModelFootprint =
        cache.getOrPut(path) {
            runCatching { parseObj(ResourceHome.resolve(path)) }.getOrNull() ?: ModelFootprint(
                halfWidth = fallbackHalfWidth,
                halfDepth = fallbackHalfWidth
            )
        }

    private fun parseObj(file: FileHandle): ModelFootprint? {
        if (!file.exists()) return null
        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY

        file.readString("UTF-8").lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("v ") }
            .forEach { line ->
                val parts = line.split(WHITESPACE)
                if (parts.size < 4) return@forEach
                val x = parts[1].toFloatOrNull() ?: return@forEach
                val z = parts[3].toFloatOrNull() ?: return@forEach
                minX = minOf(minX, x)
                maxX = maxOf(maxX, x)
                minZ = minOf(minZ, z)
                maxZ = maxOf(maxZ, z)
            }

        if (!minX.isFinite() || !maxX.isFinite() || !minZ.isFinite() || !maxZ.isFinite()) return null
        return ModelFootprint(
            halfWidth = max((maxX - minX) * 0.5f, 0.05f),
            halfDepth = max((maxZ - minZ) * 0.5f, 0.05f)
        )
    }

    private val WHITESPACE = Regex("\\s+")
}

data class ModelFootprint(
    val halfWidth: Float,
    val halfDepth: Float
)
