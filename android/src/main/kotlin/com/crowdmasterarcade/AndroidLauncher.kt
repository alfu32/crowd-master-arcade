package com.crowdmasterarcade

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import java.io.File

class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val resourceBase = getExternalFilesDir(null) ?: filesDir
        System.setProperty("crowdmaster.home", File(resourceBase, ".crowdmaster").absolutePath)
        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
            useAccelerometer = false
            useCompass = false
        }
        initialize(CrowdDefenseGame(), config)
    }
}
