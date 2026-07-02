package com.crowdmasterarcade.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.crowdmasterarcade.CrowdDefenseGame
import java.io.File
import java.io.PrintWriter

fun main() {
    try {
        val config = Lwjgl3ApplicationConfiguration().apply {
            setTitle("Crowd Master Arcade")
            setWindowedMode(1280, 720)
            useVsync(true)
            setForegroundFPS(60)
        }
        Lwjgl3Application(CrowdDefenseGame(), config)
    } catch (throwable: Throwable) {
        writeCrashLog(throwable)
        throw throwable
    }
}

private fun writeCrashLog(throwable: Throwable) {
    val roots = listOfNotNull(
        System.getProperty("crowdmaster.home")?.takeIf { it.isNotBlank() }?.let(::File),
        System.getenv("CROWD_MASTER_ARCADE_HOME")?.takeIf { it.isNotBlank() }?.let(::File),
        System.getProperty("user.home")?.takeIf { it.isNotBlank() }?.let { File(it, ".crowdmaster") },
        File(System.getProperty("java.io.tmpdir"), "crowd-master-arcade")
    )

    roots.forEach { root ->
        runCatching {
            root.mkdirs()
            PrintWriter(root.resolve("last-desktop-crash.log")).use { writer ->
                writer.println("Crowd Master Arcade desktop startup crash")
                writer.println("java.version=${System.getProperty("java.version")}")
                writer.println("os.name=${System.getProperty("os.name")}")
                writer.println("user.home=${System.getProperty("user.home")}")
                writer.println("crowdmaster.home=${System.getProperty("crowdmaster.home")}")
                writer.println("CROWD_MASTER_ARCADE_HOME=${System.getenv("CROWD_MASTER_ARCADE_HOME")}")
                throwable.printStackTrace(writer)
            }
            return
        }
    }
}
