package com.crowdmasterarcade.controller

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.crowdmasterarcade.model.InputState

class InputController {
    private var lastDragX = 0

    fun readInput(inputState: InputState) {
        var moveX = 0f
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) moveX += 1f
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) moveX -= 1f
        inputState.moveX = moveX

        if (Gdx.input.isTouched) {
            val currentX = Gdx.input.x
            inputState.dragging = true
            inputState.dragDeltaX = if (lastDragX == 0) 0f else (lastDragX - currentX).toFloat()
            lastDragX = currentX
        } else {
            inputState.dragging = false
            inputState.dragDeltaX = 0f
            lastDragX = 0
        }
    }
}
