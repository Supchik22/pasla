package io.github.supchik22.graphics

import io.github.supchik22.rendering.Renderable
import io.github.supchik22.util.Window
import io.github.supchik22.world.entity.Player
import io.github.supchik22.graphics.ShaderProgram
import org.joml.Matrix4f
import org.lwjgl.opengl.GL30.*
import org.lwjgl.system.MemoryUtil

object HUD : Renderable {
    private lateinit var shader: ShaderProgram
    private var vao = 0
    private var vbo = 0
    private val projection = Matrix4f()

    fun init(window: Window) {
        shader = ShaderProgram("shaders/hud_vertex.glsl", "shaders/hud_fragment.glsl",listOf("projection","color"))
        vao = glGenVertexArrays()
        vbo = glGenBuffers()
    }

    fun render(window: Window, player: Player) {
        projection.identity().ortho(0f, window.width.toFloat(), window.height.toFloat(), 0f, -1f, 1f)

        val healthPercent = player.getHealth() / player.getMaxHealth()
        val barX = 20f
        val barY = 20f
        val barWidth = 200f
        val barHeight = 20f

        // Рендеримо фон
        renderBar(barX, barY, barWidth, barHeight, floatArrayOf(0.2f, 0.2f, 0.2f, 1f))
        // Рендеримо здоров'я
        renderBar(barX, barY, barWidth * healthPercent, barHeight, floatArrayOf(1f, 0f, 0f, 1f))
    }

    private fun renderBar(x: Float, y: Float, width: Float, height: Float, color: FloatArray) {
        val vertices = floatArrayOf(
            x, y,
            x + width, y,
            x + width, y + height,
            x, y + height
        )

        val buffer = MemoryUtil.memAllocFloat(vertices.size).put(vertices).flip()

        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_DYNAMIC_DRAW)

        glEnableVertexAttribArray(0)
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.SIZE_BYTES, 0)

        shader.use()
        shader.setUniform("projection", projection)
        shader.setUniform("color", color)

        glDrawArrays(GL_TRIANGLE_FAN, 0, 4)

        glDisableVertexAttribArray(0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindVertexArray(0)
        MemoryUtil.memFree(buffer)
    }

    fun cleanup() {
        glDeleteBuffers(vbo)
        glDeleteVertexArrays(vao)
        shader.cleanup()
    }
}
