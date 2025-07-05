package io.github.supchik22.util

import org.lwjgl.glfw.GLFW.glfwGetTime // LWJGL's high-precision time function

object GameTime {

    private var lastFrameTime: Double = 0.0
    private var delta: Double = 0.0

    private var framesThisSecond: Int = 0
    private var lastFpsUpdateTime: Double = 0.0
    var fps: Int = 0
        private set

    fun init() {
        lastFrameTime = glfwGetTime()
        lastFpsUpdateTime = glfwGetTime()
    }

    fun updateDeltaTime(): Double {
        val currentTime = glfwGetTime()
        delta = currentTime - lastFrameTime
        lastFrameTime = currentTime
        return delta
    }

    fun getDeltaTime(): Double {
        return delta
    }

    fun updateFps() {
        framesThisSecond++
        val currentTime = glfwGetTime()
        if (currentTime - lastFpsUpdateTime >= 1.0) {
            fps = framesThisSecond
            framesThisSecond = 0
            lastFpsUpdateTime = currentTime
            println("FPS: $fps")
        }
    }
}