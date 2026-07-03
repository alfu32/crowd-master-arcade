package com.crowdmasterarcade.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.crowdmasterarcade.model.AppModel

class GameView {
    private val worldRenderer = WorldRenderer()
    private val uiRenderer = UiRenderer()
    val uiStage = uiRenderer.stage

    fun presentAppModel(appModel: AppModel) {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0.54f, 0.72f, 0.78f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        worldRenderer.render(appModel)
        uiRenderer.render(appModel)
    }

    fun dispose() {
        worldRenderer.dispose()
        uiRenderer.dispose()
    }
}
