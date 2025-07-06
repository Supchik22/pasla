package io.github.supchik22

import org.lwjgl.glfw.GLFW.*

typealias KeyAction = (window: Long, key: Int, scancode: Int, action: Int, mods: Int) -> Unit
typealias MouseMoveAction = (window: Long, xpos: Double, ypos: Double) -> Unit
object InputHandler {
    private var window: Long = 0L

    private val keyPressedStates = BooleanArray(GLFW_KEY_LAST + 1) { false }
    private val keyPreviousStates = BooleanArray(GLFW_KEY_LAST + 1) { false }

    private val mouseButtonPressedStates = BooleanArray(GLFW_MOUSE_BUTTON_LAST + 1) { false }
    private val mouseButtonPreviousStates = BooleanArray(GLFW_MOUSE_BUTTON_LAST + 1) { false }

    private val keyCallbacks = mutableListOf<KeyAction>()
    private val mouseMoveCallbacks = mutableListOf<MouseMoveAction>()
    private val mouseButtonCallbacks = mutableListOf<(window: Long, button: Int, action: Int, mods: Int) -> Unit>()

    fun initialize(window: Long) {
        this.window = window
        setupKeyboardCallback()
        setupMouseCallback()
        setupMouseButtonCallback()
    }

    private fun setupKeyboardCallback() {
        glfwSetKeyCallback(window) { win, key, scancode, action, mods ->
            if (key in 0..GLFW_KEY_LAST) {
                when (action) {
                    GLFW_PRESS -> keyPressedStates[key] = true
                    GLFW_RELEASE -> keyPressedStates[key] = false
                }
            }
            keyCallbacks.forEach { it(win, key, scancode, action, mods) }
        }
    }

    private fun setupMouseCallback() {
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED)
        glfwSetCursorPosCallback(window) { win, xpos, ypos ->
            mouseMoveCallbacks.forEach { it(win, xpos, ypos) }
        }
    }

    private fun setupMouseButtonCallback() {
        glfwSetMouseButtonCallback(window) { win, button, action, mods ->
            if (button in 0..GLFW_MOUSE_BUTTON_LAST) {
                when (action) {
                    GLFW_PRESS -> mouseButtonPressedStates[button] = true
                    GLFW_RELEASE -> mouseButtonPressedStates[button] = false
                }
            }
            mouseButtonCallbacks.forEach { it(win, button, action, mods) }
        }
    }

    fun addKeyCallback(callback: KeyAction) {
        keyCallbacks.add(callback)
    }

    fun addMouseMoveCallback(callback: MouseMoveAction) {
        mouseMoveCallbacks.add(callback)
    }

    fun addMouseButtonCallback(callback: (window: Long, button: Int, action: Int, mods: Int) -> Unit) {
        mouseButtonCallbacks.add(callback)
    }

    fun isKeyDown(keyCode: Int): Boolean {
        return keyCode in 0..GLFW_KEY_LAST && keyPressedStates[keyCode]
    }

    fun justDown(keyCode: Int): Boolean {
        if (keyCode !in 0..GLFW_KEY_LAST) return false
        return keyPressedStates[keyCode] && !keyPreviousStates[keyCode]
    }

    fun justUp(keyCode: Int): Boolean {
        if (keyCode !in 0..GLFW_KEY_LAST) return false
        return !keyPressedStates[keyCode] && keyPreviousStates[keyCode]
    }

    fun isMouseButtonDown(button: Int): Boolean {
        return button in 0..GLFW_MOUSE_BUTTON_LAST && mouseButtonPressedStates[button]
    }

    fun justMouseButtonDown(button: Int): Boolean {
        if (button !in 0..GLFW_MOUSE_BUTTON_LAST) return false
        return mouseButtonPressedStates[button] && !mouseButtonPreviousStates[button]
    }

    fun justMouseButtonUp(button: Int): Boolean {
        if (button !in 0..GLFW_MOUSE_BUTTON_LAST) return false
        return !mouseButtonPressedStates[button] && mouseButtonPreviousStates[button]
    }

    fun update() {
        for (i in keyPressedStates.indices) {
            keyPreviousStates[i] = keyPressedStates[i]
        }
        for (i in mouseButtonPressedStates.indices) {
            mouseButtonPreviousStates[i] = mouseButtonPressedStates[i]
        }
    }

    fun cleanup() {
        keyCallbacks.clear()
        mouseMoveCallbacks.clear()
        mouseButtonCallbacks.clear()
        keyPressedStates.fill(false)
        keyPreviousStates.fill(false)
        mouseButtonPressedStates.fill(false)
        mouseButtonPreviousStates.fill(false)
        glfwSetKeyCallback(window, null)
        glfwSetCursorPosCallback(window, null)
        glfwSetMouseButtonCallback(window, null)
    }
}
