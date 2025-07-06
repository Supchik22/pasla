package io.github.supchik22

import io.github.supchik22.rendering.OpenGLCommandQueue
import io.github.supchik22.rendering.Renderer
import io.github.supchik22.util.ChunkPos
import io.github.supchik22.util.GameTime // Import the GameTime object
import io.github.supchik22.util.Window // Import the Window class
import io.github.supchik22.world.Chunk
import io.github.supchik22.world.ChunkLoader
import io.github.supchik22.world.entity.Player
import org.joml.Vector3f
import kotlin.div
import kotlin.text.toInt

/**
 * Main class orchestrating the game's lifecycle, including initialization,
 * the game loop, updates, rendering, and resource cleanup.
 */
class Game {
    private lateinit var window: Window
    private lateinit var renderer: Renderer
    private lateinit var camera: Camera
    private lateinit var worldGenerator: WorldGenerator

    private var lastCameraChunkPos: ChunkPos? = null

    val player: Player = Player()

    /**
     * Initializes all core game components and resources.
     */
    fun init() {
        // Initialize the game window
        window = Window(2000, 1000, "Voxel World")
        window.create()

        // Initialize the renderer, which handles OpenGL settings and resources
        renderer = Renderer(window.width, window.height) // Pass initial window dimensions
        renderer.initOpenGLSettings()
        renderer.initResources()

        // Initialize the world generator
        worldGenerator = WorldGenerator()

        // Initialize the ChunkLoader, passing necessary dependencies
        ChunkLoader.initialize(worldGenerator, renderer.textureAtlas)

        // Initialize the camera and input handler, linking them to the window
        camera = Camera()

        player.teleport(Vector3f(0f,170f,1f))

        InputHandler.initialize(window.windowHandle)

        // Initialize game time tracking
        GameTime.init()

        // Perform initial chunk loading based on camera position
        ChunkLoader.updateLoadedChunks(camera.position)

        // Set up the framebuffer size callback to update renderer dimensions
        window.setFramebufferSizeCallback { width, height ->
            renderer.updateViewport(width, height)
        }
    }

    /**
     * Runs the main game loop.
     */
    fun run() {
        while (!window.shouldClose()) {
            // Update delta time for consistent movement and physics
            val deltaTime = GameTime.updateDeltaTime().toFloat()

            // Update game logic
            update(deltaTime)

            // Render the scene
            render()

            // Swap buffers to display the rendered frame and poll for events
            window.swapBuffers()
            window.pollEvents()

            // Update FPS counter
            GameTime.updateFps()
        }
    }

    /**
     * Updates game state, including camera movement and chunk loading.
     */
    private fun update(deltaTime: Float) {
        // Розрахунок поточного чанку камери
        val currentChunkPos = ChunkPos(
            (camera.position.x / Chunk.CHUNK_SIZE).toInt(),
            (camera.position.y / Chunk.CHUNK_SIZE).toInt(),
            (camera.position.z / Chunk.CHUNK_SIZE).toInt()
        )


        if (currentChunkPos != lastCameraChunkPos) {
            ChunkLoader.updateLoadedChunks(camera.position)
            lastCameraChunkPos = currentChunkPos
        }

        val playerChunk = ChunkLoader.getChunkContainingPosition(player.pos) ?: return


        // Оновити фізику гравця
        player.updatePhysics(playerChunk, deltaTime)
        camera.position.set(player.pos)

        OpenGLCommandQueue.processCommands()
    }

    /**
     * Triggers the rendering of the game scene.
     */
    private fun render() {
        renderer.render(camera)
    }

    /**
     * Cleans up all allocated resources before exiting the application.
     */
    fun cleanup() {
        ChunkLoader.cleanupAllChunks() // Release chunk-related resources
        renderer.cleanup() // Release renderer-related resources (shaders, textures)
        window.destroy() // Destroy the GLFW window and terminate GLFW
    }
}