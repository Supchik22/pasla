package io.github.supchik22

import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.*

class InputHandler(val window: Long, val camera: Camera) {
    private var lastX = 400.0
    private var lastY = 300.0
    private var firstMouse = true

    fun setupMouseCallback() {
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED)

        glfwSetCursorPosCallback(window) { _, xpos, ypos ->
            if (firstMouse) {
                lastX = xpos
                lastY = ypos
                firstMouse = false
            }

            val sensitivity = 0.1f
            val xoffset = ((xpos - lastX) * sensitivity).toFloat()
            val yoffset = ((lastY - ypos) * sensitivity).toFloat()
            lastX = xpos
            lastY = ypos

            camera.yaw += xoffset
            camera.pitch += yoffset
            camera.pitch = camera.pitch.coerceIn(-89f, 89f)

            camera.updateVectors()
        }
    }

    fun handleMovement(speed: Float = 0.9f) {
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS)
            camera.position.add(Vector3f(camera.front).mul(speed))
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS)
            camera.position.sub(Vector3f(camera.front).mul(speed))

        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS)
            camera.position.sub(Vector3f(camera.right).mul(speed))
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS)
            camera.position.add(Vector3f(camera.right).mul(speed))
    }
}
