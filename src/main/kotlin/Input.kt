package io.github.supchik22

import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.*

typealias KeyAction = (Long, Int, Int, Int, Int) -> Unit

typealias MouseMoveAction = (Long, Double, Double) -> Unit

typealias SpecificKeyAction = (Float) -> Unit

object InputHandler {
    private var window: Long = 0L
    private lateinit var camera: Camera

    private var lastX = 0.0
    private var lastY = 0.0
    private var firstMouse = true

    // Мапи для зберігання зареєстрованих KeyAction та MouseMoveAction
    private val keyCallbacks: MutableList<KeyAction> = mutableListOf()
    private val mouseMoveCallbacks: MutableList<MouseMoveAction> = mutableListOf()

    // Мапа для "дій": key -> список SpecificKeyAction
    // Дозволяє прив'язувати кілька дій до однієї клавіші
    private val keyActions: MutableMap<Int, MutableList<SpecificKeyAction>> = mutableMapOf()

    /**
     * Ініціалізує InputHandler. Має бути викликано один раз при старті програми.
     * @param window Дескриптор вікна GLFW.
     * @param camera Об'єкт камери, який буде контролюватися за замовчуванням.
     */
    fun initialize(window: Long, camera: Camera) {
        this.window = window
        this.camera = camera
        setupMouseCallback() // Встановлюємо колбек миші
        setupKeyboardCallback() // Встановлюємо колбек клавіатури
    }

    // --- 3. Налаштування колбеків GLFW ---

    private fun setupMouseCallback() {
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED)

        glfwSetCursorPosCallback(window) { windowHandle, xpos, ypos ->
            if (firstMouse) {
                lastX = xpos
                lastY = ypos
                firstMouse = false
            }

            val sensitivity = 0.1f
            val xoffset = ((xpos - lastX) * sensitivity).toFloat()
            val yoffset = ((lastY - ypos) * sensitivity).toFloat() // Обертаємо Y-вісь
            lastX = xpos
            lastY = ypos

            // За замовчуванням камера завжди реагує на рух миші
            camera.yaw += xoffset
            camera.pitch += yoffset
            camera.pitch = camera.pitch.coerceIn(-89f, 89f) // Обмежуємо кут нахилу
            camera.updateVectors()

            // Сповіщаємо всі зареєстровані MouseMoveAction
            mouseMoveCallbacks.forEach { it(windowHandle, xpos, ypos) }
        }
    }

    private fun setupKeyboardCallback() {
        glfwSetKeyCallback(window) { windowHandle, key, scancode, action, mods ->
            // Сповіщаємо всі зареєстровані KeyAction
            keyCallbacks.forEach { it(windowHandle, key, scancode, action, mods) }
        }
    }

    // --- 4. Реєстрація колбеків та дій ---

    /**
     * Додає глобальний обробник подій клавіатури.
     * @param callback Функція KeyAction, яка буде викликана при будь-якій події клавіатури.
     */
    fun addKeyCallback(callback: KeyAction) {
        keyCallbacks.add(callback)
    }

    /**
     * Додає глобальний обробник подій руху миші.
     * @param callback Функція MouseMoveAction, яка буде викликана при русі миші.
     */
    fun addMouseMoveCallback(callback: MouseMoveAction) {
        mouseMoveCallbacks.add(callback)
    }

    /**
     * Прив'язує SpecificKeyAction до певної клавіші.
     * @param key Код клавіші GLFW (наприклад, GLFW_KEY_W).
     * @param action Функція SpecificKeyAction, яка буде викликана, коли клавіша натиснута.
     */
    fun registerKeyAction(key: Int, action: SpecificKeyAction) {
        keyActions.getOrPut(key) { mutableListOf() }.add(action)
    }

    // --- 5. Обробка рухів за замовчуванням та виконання зареєстрованих дій ---

    /**
     * Обробляє рух камери за замовчуванням (W, S, A, D) і виконує зареєстровані дії.
     * Цей метод слід викликати в головному циклі гри.
     * @param speed Швидкість переміщення.
     */
    fun handleInput(speed: Float) {

        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS)
            camera.position.add(Vector3f(camera.front).mul(speed))
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS)
            camera.position.sub(Vector3f(camera.front).mul(speed))

        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS)
            camera.position.sub(Vector3f(camera.right).mul(speed))
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS)
            camera.position.add(Vector3f(camera.right).mul(speed))

        keyActions.forEach { (key, actions) ->
            if (glfwGetKey(window, key) == GLFW_PRESS) {
                actions.forEach { it(speed) } // Передаємо швидкість/deltaTime дії
            }
        }
    }

    fun cleanup() {
        keyCallbacks.clear()
        mouseMoveCallbacks.clear()
        keyActions.clear()
        glfwSetKeyCallback(window, null) // Видаляємо колбек клавіатури
        glfwSetCursorPosCallback(window, null) // Видаляємо колбек миші
    }
}