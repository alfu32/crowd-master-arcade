package com.crowdmasterarcade.view

data class ShadowSettings(
    val shadowBias: Float = 2500f,
    val shadowNormalBias: Float = 5620f,
    val pcfMode: Int = 1,
    val dither: Boolean = false,
    val useCsm: Boolean = true
)
