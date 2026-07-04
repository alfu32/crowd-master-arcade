package com.crowdmasterarcade.controller

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.crowdmasterarcade.model.InputState

class InputController {
    private var lastDragX = 0
    private var lastDragY = 0

    fun readInput(inputState: InputState) {
        var moveX = 0f
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) moveX += 1f
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) moveX -= 1f
        inputState.moveX = moveX

        var speedDelta = 0f
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) speedDelta += 1f
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) speedDelta -= 1f

        if (Gdx.input.isTouched) {
            val currentX = Gdx.input.x
            val currentY = Gdx.input.y
            inputState.dragging = true
            inputState.dragDeltaX = if (lastDragX == 0) 0f else (lastDragX - currentX).toFloat()
            inputState.dragDeltaY = if (lastDragY == 0) 0f else (lastDragY - currentY).toFloat()
            speedDelta += inputState.dragDeltaY * 0.025f
            lastDragX = currentX
            lastDragY = currentY
        } else {
            inputState.dragging = false
            inputState.dragDeltaX = 0f
            inputState.dragDeltaY = 0f
            lastDragX = 0
            lastDragY = 0
        }
        inputState.speedDelta = speedDelta.coerceIn(-1f, 1f)
    }
}
