package com.crowdmasterarcade.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider

class SketchShaderProvider(
    private val shadowSettingsProvider: () -> ShadowSettings,
    private val shadowLightProvider: () -> DirectionalShadowLight
) : DefaultShaderProvider(createConfig()) {

    override fun createShader(renderable: Renderable): Shader =
        SketchDefaultShader(renderable, config, shadowSettingsProvider, shadowLightProvider)

    companion object {
        private fun createConfig(): DefaultShader.Config {
            val vertex = Gdx.files.internal("shaders/sketch-default.vertex.glsl").readString("UTF-8")
            val fragment = Gdx.files.internal("shaders/sketch-default.fragment.glsl").readString("UTF-8")
            return DefaultShader.Config(vertex, fragment)
        }
    }
}
