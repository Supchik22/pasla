package io.github.supchik22

import io.github.supchik22.graphics.renderHUD
import io.github.supchik22.rendering.OpenGLCommandQueue
import io.github.supchik22.rendering.Renderer
import io.github.supchik22.util.ChunkPos
import io.github.supchik22.util.GameTime
import io.github.supchik22.util.Window
import io.github.supchik22.world.Chunk
import io.github.supchik22.world.ChunkLoader
import io.github.supchik22.world.entity.Player
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.* // Import GLFW for key codes


/**
 * Main class orchestrating the game's lifecycle, including initialization,
 * the game loop, updates, rendering, and resource cleanup.
 */
class Game {
    private lateinit var window: Window
    private lateinit var renderer: Renderer
    private lateinit var camera: Camera
    private lateinit var worldGenerator: WorldGenerator

    private lateinit var imGuiLayer: ImGuiLayer

    private var lastCameraChunkPos: ChunkPos? = null

    val player: Player = Player()

    private val FIXED_TIMESTEP = 1f / 60f  // 60 фізичних кадрів на секунду
    private var physicsAccumulator = 0f


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
        camera = player.camera

        InputHandler.initialize(window.windowHandle)

        // Initialize game time tracking
        GameTime.init()


        // Perform initial chunk loading based on player's initial position
        // The camera position will be updated to player.pos in the update loop
        ChunkLoader.updateLoadedChunks(player.pos)

        imGuiLayer = ImGuiLayer()
        imGuiLayer.init(window.windowHandle)


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

            physicsAccumulator += deltaTime

            // --- Обробка фіксованих фізичних кадрів ---
            while (physicsAccumulator >= FIXED_TIMESTEP) {
                update(FIXED_TIMESTEP)
                physicsAccumulator -= FIXED_TIMESTEP
            }

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

        player.updatePhysics(deltaTime)

        camera.position.set(player.pos)
        camera.position.add(0f,player.height,0f)


        if (ChunkLoader.areChunksLoadedAround(player.pos, radiusChunks = 1) && !(player.loaded) ) {
            player.loaded = true
            player.teleport(Vector3f(0f,200f,0f))
        }

        // --- Chunk Loading based on Player/Camera Position ---
        // Calculate current chunk position based on player's actual position
        val currentChunkPos = ChunkPos(
            (player.pos.x / Chunk.CHUNK_SIZE).toInt(),
            (player.pos.y / Chunk.CHUNK_SIZE).toInt(),
            (player.pos.z / Chunk.CHUNK_SIZE).toInt()
        )

        // Only update loaded chunks if the player has moved into a new chunk
        if (currentChunkPos != lastCameraChunkPos) {
            println("DEBUG Game: Player moved to new chunk: $currentChunkPos. Updating loaded chunks.")
            ChunkLoader.updateLoadedChunks(player.pos) // Use player.pos for chunk loading
            lastCameraChunkPos = currentChunkPos
        }

        // Check if the chunk containing the player is loaded. If not, something is wrong
        // with chunk loading or the player is outside the loaded world.
        val playerChunk = ChunkLoader.getChunkContainingPosition(player.pos)
        if (playerChunk == null) {
            println("WARNING: Player is in an unloaded chunk at ${player.pos}!")
            // You might want to teleport the player back to a safe zone or load the chunk immediately.
        }
        InputHandler.update()
        OpenGLCommandQueue.processCommands()
    }

    /**
     * Triggers the rendering of the game scene.
     */
    private fun render() {
        renderer.render(camera)

        imGuiLayer.render()
    }


    /**
     * Cleans up all allocated resources before exiting the application.
     */
    fun cleanup() {
        ChunkLoader.cleanupAllChunks() // Release chunk-related resources
        renderer.cleanup() // Release renderer-related resources (shaders, textures)
        window.destroy() // Destroy the GLFW window and terminate GLFW
        imGuiLayer.dispose()
    }
}