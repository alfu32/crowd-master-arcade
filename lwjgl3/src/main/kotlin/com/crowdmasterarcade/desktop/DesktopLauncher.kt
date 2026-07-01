package com.crowdmasterarcade.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.crowdmasterarcade.CrowdDefenseGame

fun main() {
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("Crowd Master Arcade")
        setWindowedMode(1280, 720)
        useVsync(true)
        setForegroundFPS(60)
    }
    Lwjgl3Application(CrowdDefenseGame(), config)
}
