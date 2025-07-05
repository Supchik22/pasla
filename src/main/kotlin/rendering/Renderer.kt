package io.github.supchik22.rendering

import io.github.supchik22.Camera
import io.github.supchik22.Chunk
import io.github.supchik22.ChunkLoader
import io.github.supchik22.graphics.ShaderProgram
import io.github.supchik22.graphics.TextureAtlas
import org.joml.Matrix4f
import org.joml.Math
import org.lwjgl.opengl.GL11.*
import java.awt.Color

/**
 * Handles all OpenGL rendering operations, including shader management,
 * texture binding, and rendering of game objects like chunks.
 */
class Renderer(private var windowWidth: Int, private var windowHeight: Int) {
    lateinit var shaderProgram: ShaderProgram
    lateinit var textureAtlas: TextureAtlas
    // If you have Skybox, you might manage it here too
    // private lateinit var skybox: Skybox

    /**
     * Initializes basic OpenGL settings like viewport, depth testing, and culling.
     */
    fun initOpenGLSettings() {
        glViewport(0, 0, windowWidth, windowHeight)
        glEnable(GL_DEPTH_TEST) // Enable depth testing for proper 3D rendering
        glEnable(GL_CULL_FACE) // Enable face culling for optimization
        glCullFace(GL_BACK) // Cull back faces

        val bgColor = Color.decode("#bbe1fa") // A pleasant sky blue background color
        glClearColor(bgColor.red / 255.0f, bgColor.green / 255.0f, bgColor.blue / 255.0f, 1.0f)
    }

    /**
     * Initializes rendering-specific resources like shader programs and texture atlases.
     */
    fun initResources() {
        shaderProgram = ShaderProgram("shaders/vertex.glsl", "shaders/fragment.glsl")

        val texturePaths = listOf(
            "/textures/air.png",
            "/textures/dirt.png",
            "/textures/grass_top.png",
            "/textures/stone.png",
            "/textures/grass_side.png",
            "/textures/grass.png"
        )
        textureAtlas = TextureAtlas(texturePaths, textureSize = 16)

        // Skybox.init() // Uncomment if you have a Skybox class to initialize
    }

    /**
     * Updates the OpenGL viewport dimensions. This should be called when the window is resized.
     * @param width The new width of the viewport.
     * @param height The new height of the viewport.
     */
    fun updateViewport(width: Int, height: Int) {
        this.windowWidth = width
        this.windowHeight = height
        glViewport(0, 0, width, height)
    }

    /**
     * Renders the entire game scene from the perspective of the camera.
     * @param camera The camera object used for view transformation.
     */
    fun render(camera: Camera) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT) // Clear color and depth buffers

        val projectionMatrix = Matrix4f()
            .perspective(
                Math.toRadians(75.0f), // Field of View (FOV)
                windowWidth.toFloat() / windowHeight.toFloat(), // Aspect ratio
                0.1f, // Near clipping plane
                10000f // Far clipping plane (for large worlds)
            )
        val viewMatrix = camera.getViewMatrix() // Get the camera's view matrix

        // Skybox.render(projectionMatrix, viewMatrix) // Uncomment if you have a Skybox to render

        shaderProgram.use() // Activate the shader program
        shaderProgram.setUniform("projection", projectionMatrix) // Pass projection matrix to shader
        shaderProgram.setUniform("view", viewMatrix) // Pass view matrix to shader
        shaderProgram.setUniform("ourTexture", 0) // Tell the shader the texture is in texture unit 0

        textureAtlas.bind()
        for (chunkRendering in ChunkLoader.getAllChunkRenderings()) {
            val modelMatrix = Matrix4f()


            shaderProgram.setUniform("model", modelMatrix) // Pass the model matrix to the shader
            chunkRendering.render() // Render the chunk
        }
        textureAtlas.unbind() // Unbind the texture atlas

        shaderProgram.detach() // Deactivate the shader program
    }

    /**
     * Cleans up all OpenGL-related resources managed by the renderer.
     */
    fun cleanup() {
        textureAtlas.cleanup() // Clean up texture atlas resources
        shaderProgram.cleanup() // Clean up shader program resources
        // Skybox.cleanup() // Uncomment if you have a Skybox to clean up
    }
}