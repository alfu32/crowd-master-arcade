package com.crowdmasterarcade.model

import com.badlogic.gdx.Gdx
import java.io.File

object LevelCatalog {
    private val supportedExtensions = setOf("level", "txt", "yaml", "yml")

    fun load(folderPath: String? = System.getProperty("levels.dir")): List<LevelDefinition> {
        val externalLevels = folderPath
            ?.takeIf { it.isNotBlank() }
            ?.let(::loadFromFolder)
            .orEmpty()
        if (externalLevels.isNotEmpty()) return externalLevels

        return loadFromAssets().ifEmpty { listOf(DefaultLevels.ravenBend()) }
    }

    fun loadFromFolder(folderPath: String): List<LevelDefinition> {
        val folder = File(folderPath)
        if (!folder.isDirectory) return emptyList()

        return folder.listFiles()
            .orEmpty()
            .asSequence()
            .filter { it.isFile && it.extension.lowercase() in supportedExtensions }
            .sortedBy { it.name }
            .map { LevelTextParser.parse(it.readText()) }
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
}
