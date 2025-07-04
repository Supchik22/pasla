package io.github.supchik22


import io.github.supchik22.graphics.ShaderProgram
import io.github.supchik22.graphics.TextureAtlas
import org.joml.Matrix4f
import org.joml.Math
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL20.GL_SHADING_LANGUAGE_VERSION
import org.lwjgl.system.MemoryUtil.NULL
import java.awt.Color

// Глобальні змінні (якщо потрібно, але краще їх передавати параметрами функцій)
lateinit var textureAtlas: TextureAtlas
lateinit var shaderProgram: ShaderProgram
lateinit var worldGenerator: WorldGenerator
var lastFrameTime: Long = 0L

// Змінні для розмірів вікна, які можуть змінюватися
val windowWidth = intArrayOf(2000)
val windowHeight = intArrayOf(1000)

fun main() {
    if (!glfwInit()) throw IllegalStateException("GLFW init failed")

    // Налаштування для OpenGL Core Profile
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE) // Для Mac OS

    val window = glfwCreateWindow(2000, 1000, "Kotlin Voxel World", NULL, NULL)
    if (window == NULL) {
        glfwTerminate()
        throw RuntimeException("Failed to create window")
    }

    glfwMakeContextCurrent(window)
    glfwSwapInterval(1)
    GL.createCapabilities()

    println("OpenGL version: ${glGetString(GL_VERSION)}")
    println("GLSL version: ${glGetString(GL_SHADING_LANGUAGE_VERSION)}")


    glfwSetFramebufferSizeCallback(window) { _, width, height ->
        glViewport(0, 0, width, height)
        windowWidth[0] = width
        windowHeight[0] = height

    }



    glViewport(0, 0, windowWidth[0], windowHeight[0])
    glEnable(GL_DEPTH_TEST)
    glEnable(GL_CULL_FACE)
    glCullFace(GL_BACK)




    // Ініціалізація шейдерів
    shaderProgram = ShaderProgram("shaders/vertex.glsl", "shaders/fragment.glsl")

    // Ініціалізація WorldGenerator
    worldGenerator = WorldGenerator()

    // Ініціалізація атласу текстур
    val texturePaths = listOf(
        "/textures/air.png",          // 0
        "/textures/dirt.png",         // 1
        "/textures/grass_top.png",    // 2
        "/textures/stone.png",        // 3
        "/textures/grass_side.png",   // 4
        "/textures/grass.png"         // 5
    )

    textureAtlas = TextureAtlas(texturePaths, textureSize = 16)

    // Ініціалізуємо ChunkLoader, передаючи йому генератор та атлас
    ChunkLoader.initialize(worldGenerator, textureAtlas)

    val camera = Camera()
    val input = InputHandler(window, camera)
    input.setupMouseCallback()

    // Завантажуємо початкові чанки навколо камери
    ChunkLoader.updateLoadedChunks(camera.position)

    var lastChunkUpdatePos = Vector3f(camera.position)
    val chunkUpdateThreshold = Chunk.CHUNK_SIZE * 2f

    lastFrameTime = System.nanoTime()

    var frames = 0
    var fps = 0
    var lastTime = System.currentTimeMillis()

    val color = Color.decode("#bbe1fa")
    glClearColor(
        color.red / 255.0f,
        color.green / 255.0f,
        color.blue / 255.0f,
        1.0f
    )


    while (!glfwWindowShouldClose(window)) {
        val currentFrameTime = System.nanoTime()
        val deltaTime = (currentFrameTime - lastFrameTime) / 1_000_000_000.0f
        lastFrameTime = currentFrameTime

        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)



        shaderProgram.use()

        frames++
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastTime >= 1000) {
            fps = frames
            println("FPS: $fps") // або виводь на екран
            frames = 0
            lastTime = currentTime
        }


        val projectionMatrix = Matrix4f()
            .perspective(
                Math.toRadians(75.0f),
                windowWidth[0].toFloat() / windowHeight[0].toFloat(),
                0.1f,
                10000f
            )
        shaderProgram.setUniform("projection", projectionMatrix)

        val viewMatrix = camera.getViewMatrix()
        shaderProgram.setUniform("view", viewMatrix)

        shaderProgram.setUniform("ourTexture", 0)

        textureAtlas.bind()
        for (chunkRendering in ChunkLoader.getAllChunkRenderings()) {
            val modelMatrix = Matrix4f()
            shaderProgram.setUniform("model", modelMatrix)
            chunkRendering.render()
        }
        textureAtlas.unbind()
        shaderProgram.detach()

        input.handleMovement(deltaTime*10.0f)
        glfwSwapBuffers(window)
        glfwPollEvents()


    }

    // Очищаємо ресурси
    ChunkLoader.cleanupAllChunks()
    textureAtlas.cleanup()
    shaderProgram.cleanup()
    glfwDestroyWindow(window)
    glfwTerminate()
}