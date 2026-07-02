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
        copyMissing(Gdx.files.internal("assets"), root.child("assets"))
        seedOctdSources(root)
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
        val assets = Gdx.files.internal("assets")
        if (!assets.exists()) return
        assets.list()
            .filter { !it.isDirectory && it.extension().equals("octd", ignoreCase = true) }
            .forEach { source -> copyMissing(source, root.child(source.name())) }
    }

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
