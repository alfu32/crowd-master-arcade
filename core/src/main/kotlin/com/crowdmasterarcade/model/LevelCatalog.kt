package com.crowdmasterarcade.model

import com.badlogic.gdx.Gdx
import java.io.File

object LevelCatalog {
    private val supportedExtensions = setOf("level", "cma-level", "txt", "yaml", "yml")

    fun load(folderPath: String? = System.getProperty("levels.dir")): List<LevelDefinition> {
        val externalLevels = folderPath
            ?.takeIf { it.isNotBlank() }
            ?.let(::loadFromFolder)
            .orEmpty()
        if (externalLevels.isNotEmpty()) return externalLevels

        val resourceHomeLevels = loadFromResourceHome()
        if (resourceHomeLevels.isNotEmpty()) return resourceHomeLevels

        return loadFromAssets().ifEmpty { listOf(DefaultLevels.ravenBend()) }
    }

    fun loadFromFolder(folderPath: String): List<LevelDefinition> {
        val folder = File(folderPath)
        if (!folder.isDirectory) return emptyList()

        return folder.listFiles()
            .orEmpty()
            .asSequence()
            .filter { it.isFile && isSupportedLevelFile(it.name, it.extension) }
            .sortedBy { it.name }
            .map { LevelTextParser.parse(it.readText()) }
            .toList()
    }

    fun loadFromResourceHome(): List<LevelDefinition> {
        val folder = ResourceHome.levelsFolder()
        if (!folder.exists()) return emptyList()

        return folder.list()
            .asSequence()
            .filter { !it.isDirectory && isSupportedLevelFile(it.name(), it.extension()) }
            .sortedBy { it.name() }
            .map { LevelTextParser.parse(it.readString("UTF-8")) }
            .toList()
    }

    fun loadFromAssets(assetFolder: String = "levels"): List<LevelDefinition> {
        val index = Gdx.files.internal("$assetFolder/index.txt")
        if (!index.exists()) return emptyList()

        return index.readString()
            .lineSequence()
            .map { it.substringBefore("#").trim() }
            .filter { it.isNotBlank() }
            .map { Gdx.files.internal("$assetFolder/$it") }
            .filter { it.exists() }
            .map { LevelTextParser.parse(it.readString()) }
            .toList()
    }

    private fun isSupportedLevelFile(name: String, extension: String): Boolean =
        name != "index.txt" && extension.lowercase() in supportedExtensions
}
