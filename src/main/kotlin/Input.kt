package io.github.supchik22

import org.lwjgl.glfw.GLFW.*

typealias KeyAction = (window: Long, key: Int, scancode: Int, action: Int, mods: Int) -> Unit
typealias MouseMoveAction = (window: Long, xpos: Double, ypos: Double) -> Unit

object InputHandler {
    private var window: Long = 0L

    private val keyCallbacks = mutableListOf<KeyAction>()
    private val mouseMoveCallbacks = mutableListOf<MouseMoveAction>()

    fun initialize(window: Long) {
        this.window = window
        setupKeyboardCallback()
        setupMouseCallback()
    }

    private fun setupKeyboardCallback() {
        glfwSetKeyCallback(window) { win, key, scancode, action, mods ->
            keyCallbacks.forEach { it(win, key, scancode, action, mods) }
        }
    }

    private fun setupMouseCallback() {
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED)

        glfwSetCursorPosCallback(window) { win, xpos, ypos ->
            mouseMoveCallbacks.forEach { it(win, xpos, ypos) }
        }
    }

    fun addKeyCallback(callback: KeyAction) {
        keyCallbacks.add(callback)
    }

    fun addMouseMoveCallback(callback: MouseMoveAction) {
        mouseMoveCallbacks.add(callback)
    }

    fun cleanup() {
        keyCallbacks.clear()
        mouseMoveCallbacks.clear()
        glfwSetKeyCallback(window, null)
        glfwSetCursorPosCallback(window, null)
    }
}
