package com.crowdmasterarcade.model

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import java.io.File

object ResourceHome {
    private const val ROOT_FOLDER = ".crowdmaster"
    private const val HOME_PROPERTY = "crowdmaster.home"
    private const val HOME_ENV = "CROWD_MASTER_ARCADE_HOME"

    private var initialized = false
    private var rootOverride: FileHandle? = null

    val root: FileHandle
        get() = rootOverride ?: defaultRoot()

    fun initialize() {
        if (initialized) return
        val root = root
        root.mkdirs()
        seedLevels(root)
        seedAssets(root)
        initialized = true
    }

    fun resolve(path: String): FileHandle {
        initialize()
        val resource = root.child(path)
        if (resource.exists()) return resource
        return Gdx.files.internal(path)
    }

    fun levelsFolder(): FileHandle {
        initialize()
        return root
    }

    fun resetFromPackagedAssets() {
        val root = root
        if (root.exists()) root.deleteDirectory()
        initialized = false
        initialize()
    }

    internal fun useRootForTests(root: FileHandle?) {
        rootOverride = root
        initialized = false
    }

    private fun defaultRoot(): FileHandle {
        System.getProperty(HOME_PROPERTY)
            ?.takeIf { it.isNotBlank() }
            ?.let { return Gdx.files.absolute(it) }

        System.getenv(HOME_ENV)
            ?.takeIf { it.isNotBlank() }
            ?.let { return Gdx.files.absolute(it) }

        System.getenv("HOME")
            ?.takeIf { it.isNotBlank() }
            ?.let { return Gdx.files.absolute(File(it, ROOT_FOLDER).absolutePath) }

        return if (Gdx.files.isExternalStorageAvailable) {
            Gdx.files.external(ROOT_FOLDER)
        } else {
            Gdx.files.local(ROOT_FOLDER)
        }
    }

    private fun seedLevels(root: FileHandle) {
        val index = Gdx.files.internal("levels/index.txt")
        if (index.exists()) {
            index.readString("UTF-8")
                .lineSequence()
                .map { it.substringBefore("#").trim() }
                .filter { it.isNotBlank() }
                .forEach { name ->
                    copyMissing(Gdx.files.internal("levels/$name"), root.child(name))
                }
            return
        }

        val levels = Gdx.files.internal("levels")
        if (levels.exists()) {
            levels.list().forEach { level ->
                if (!level.isDirectory) copyMissing(level, root.child(level.name()))
            }
        }
    }

    private fun seedOctdSources(root: FileHandle) {
        packagedAssetEntries()
            .filter { it.endsWith(".octd", ignoreCase = true) }
            .forEach { name ->
                copyMissingPackagedResource("assets/$name", root.child(name.substringAfterLast('/')))
            }
    }

    private fun seedAssets(root: FileHandle) {
        val assetRoot = root.child("assets")
        packagedAssetEntries().forEach { name ->
            copyMissingPackagedResource("assets/$name", assetRoot.child(name))
        }
        seedOctdSources(root)
    }

    private fun packagedAssetEntries(): List<String> {
        val classpathIndex = readPackagedResourceText("assets/index.txt")
        if (classpathIndex != null) return parseIndex(classpathIndex)

        val index = Gdx.files.internal("assets/index.txt")
        if (index.exists()) return parseIndex(index.readString("UTF-8"))

        return listFilesRecursive(Gdx.files.internal("assets"))
    }

    private fun parseIndex(text: String): List<String> =
        text
            .lineSequence()
            .map { it.substringBefore("#").trim() }
            .filter { it.isNotBlank() }
            .toList()

    private fun readPackagedResourceText(path: String): String? =
        packagedResourceStream(path)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }

    private fun copyMissingPackagedResource(path: String, target: FileHandle) {
        if (target.exists()) return
        packagedResourceStream(path)?.use { input ->
            target.parent().mkdirs()
            target.write(false).use { output -> input.copyTo(output) }
            return
        }
        copyMissing(Gdx.files.internal(path), target)
    }

    private fun packagedResourceStream(path: String) =
        ResourceHome::class.java.classLoader.getResourceAsStream(path)

    private fun listFilesRecursive(root: FileHandle): List<String> =
        runCatching {
            if (!root.exists() || !root.isDirectory) return@runCatching emptyList()
            root.list()
                .flatMap { child ->
                    if (child.isDirectory) {
                        listFilesRecursive(child).map { "${child.name()}/$it" }
                    } else {
                        listOf(child.name())
                    }
                }
        }.getOrDefault(emptyList())

    private fun copyMissing(source: FileHandle, target: FileHandle) {
        if (!source.exists()) return
        if (source.isDirectory) {
            target.mkdirs()
            source.list().forEach { child ->
                copyMissing(child, target.child(child.name()))
            }
            return
        }
        if (target.exists()) return
        target.parent().mkdirs()
        source.copyTo(target)
    }
}
