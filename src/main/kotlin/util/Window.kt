package io.github.supchik22.util

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL20.GL_SHADING_LANGUAGE_VERSION
import org.lwjgl.system.MemoryUtil.NULL

/**
 * Manages the GLFW window, including creation, callbacks, and lifecycle.
 */
class Window(var width: Int, var height: Int, val title: String) {
    var windowHandle: Long = NULL
        private set // Make it read-only from outside

    // A lambda to hold the framebuffer size callback
    private var framebufferSizeCallback: ((Int, Int) -> Unit)? = null

    /**
     * Creates the GLFW window and initializes the OpenGL context.
     * @throws IllegalStateException if GLFW initialization fails.
     * @throws RuntimeException if window creation fails.
     */
    fun create() {
        if (!glfwInit()) throw IllegalStateException("GLFW init failed")

        // Configure GLFW window hints for OpenGL 3.3 Core Profile
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE) // Required for Mac OS

        windowHandle = glfwCreateWindow(width, height, title, NULL, NULL)
        if (windowHandle == NULL) {
            glfwTerminate()
            throw RuntimeException("Failed to create window")
        }

        glfwMakeContextCurrent(windowHandle) // Make the window's context current on the calling thread
        glfwSwapInterval(1) // Enable V-Sync to cap frame rate at monitor's refresh rate
        GL.createCapabilities() // Create OpenGL capabilities for the current context

        println("OpenGL version: ${glGetString(GL_VERSION)}")
        println("GLSL version: ${glGetString(GL_SHADING_LANGUAGE_VERSION)}")

        // Set the internal framebuffer size callback
        glfwSetFramebufferSizeCallback(windowHandle) { _, w, h ->
            width = w
            height = h
            // Call the external callback if it's set
            framebufferSizeCallback?.invoke(w, h)
        }
    }

    /**
     * Sets a callback function to be executed when the framebuffer is resized.
     * @param callback A lambda (width: Int, height: Int) -> Unit that will be called.
     */
    fun setFramebufferSizeCallback(callback: (Int, Int) -> Unit) {
        this.framebufferSizeCallback = callback
    }

    /**
     * Checks if the window should close.
     * @return True if the window close flag has been set, false otherwise.
     */
    fun shouldClose(): Boolean = glfwWindowShouldClose(windowHandle)

    /**
     * Swaps the front and back buffers of the window, displaying the rendered frame.
     */
    fun swapBuffers() = glfwSwapBuffers(windowHandle)

    /**
     * Processes all pending events, such as keyboard input or mouse movement.
     */
    fun pollEvents() = glfwPollEvents()

    /**
     * Destroys the window and terminates GLFW.
     */
    fun destroy() {
        glfwDestroyWindow(windowHandle)
        glfwTerminate()
    }
}