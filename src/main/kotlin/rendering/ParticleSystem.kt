package io.github.supchik22.rendering

import io.github.supchik22.game
import io.github.supchik22.graphics.ShaderProgram
import io.github.supchik22.world.BlockRegistry
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL32.GL_PROGRAM_POINT_SIZE
import org.lwjgl.system.MemoryUtil
import java.awt.Color
import kotlin.math.floor

object ParticleSystem {

    data class Particle(
        var position: Vector3f,
        var velocity: Vector3f,
        var life: Float,
        var color: Color,
        var size: Float,
        var acceleration: Vector3f = Vector3f(0f, -9.81f, 0f),
        val radius: Float = 0.1f
    )


    private val particles = mutableListOf<Particle>()
    private const val maxParticles = 1000

    private var vao = 0
    private var vbo = 0

    private lateinit var shader: ShaderProgram

    fun init() {
        shader = ShaderProgram("shaders/particle.vert", "shaders/particle.frag",listOf("projection", "view","cameraPos") )

        vao = glGenVertexArrays()
        vbo = glGenBuffers()

        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)

        val stride = 7 * Float.SIZE_BYTES
        glBufferData(GL_ARRAY_BUFFER, maxParticles * stride.toLong(), GL_DYNAMIC_DRAW)

        // position (location = 0)
        glEnableVertexAttribArray(0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L)

        // color (location = 1)
        glEnableVertexAttribArray(1)
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, (3 * Float.SIZE_BYTES).toLong())

        // size (location = 2)
        glEnableVertexAttribArray(2)
        glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, (6 * Float.SIZE_BYTES).toLong())

        glBindVertexArray(0)
    }

    fun spawn(
        position: Vector3f,
        velocity: Vector3f,
        life: Float,
        color: Color,
        size: Float,
        acceleration: Vector3f = Vector3f(0f, -9.81f, 0f)
    ) {
        if (particles.size < maxParticles) {
            particles.add(
                Particle(
                    Vector3f(position),
                    Vector3f(velocity),
                    life,
                    color,
                    size,
                    Vector3f(acceleration)
                )
            )
        }
    }

    fun spawnBlockParticle(
        position: Vector3f,
        velocity: Vector3f,
        life: Float,
        blockId: Short,
        size: Float,
        acceleration: Vector3f = Vector3f(0f, -9.81f, 0f)
    ) {
        val blockEntry = BlockRegistry.getEntry(blockId) ?: return
        val color = blockEntry.color

        spawn(position, velocity, life, color, size, acceleration)
    }

    fun update(deltaTime: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()

            // застосування прискорення
            p.velocity.add(Vector3f(p.acceleration).mul(deltaTime))

            // розрахунок нового положення
            val newPos = Vector3f(p.position).add(Vector3f(p.velocity).mul(deltaTime))

            // перевірка на зіткнення
            if (collidesWithBlock(newPos, p.radius)) {
                // проста реакція — зупинити частинку
                p.velocity.zero()
            } else {
                p.position.set(newPos)
            }

            p.life -= deltaTime
            if (p.life <= 0f) iterator.remove()
        }
    }
    private fun collidesWithBlock(pos: Vector3f, radius: Float): Boolean {
        val minX = floor(pos.x - radius).toInt()
        val maxX = floor(pos.x + radius).toInt()
        val minY = floor(pos.y - radius).toInt()
        val maxY = floor(pos.y + radius).toInt()
        val minZ = floor(pos.z - radius).toInt()
        val maxZ = floor(pos.z + radius).toInt()

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    if (io.github.supchik22.world.ChunkLoader.isBlockSolidAt(x, y, z)) {
                        return true
                    }
                }
            }
        }

        return false
    }


    fun render(view: Matrix4f, projection: Matrix4f) {
        shader.use()
        shader.setUniform("view", view)
        shader.setUniform("projection", projection)
        shader.setUniform("cameraPos", game.getCamera().position)
        val data = MemoryUtil.memAllocFloat(particles.size * 7)
        for (p in particles) {
            data.put(p.position.x).put(p.position.y).put(p.position.z)

            // Конвертація кольору з 0..255 у 0..1
            data.put(p.color.red / 255f)
            data.put(p.color.green / 255f)
            data.put(p.color.blue / 255f)

            data.put(p.size)
        }

        data.flip()

        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferSubData(GL_ARRAY_BUFFER, 0, data)
        MemoryUtil.memFree(data)

        glBindVertexArray(vao)
        glEnable(GL_PROGRAM_POINT_SIZE)
        glDrawArrays(GL_POINTS, 0, particles.size)
        glBindVertexArray(0)

        shader.detach()
    }

    fun cleanup() {
        glDeleteBuffers(vbo)
        glDeleteVertexArrays(vao)
        shader.cleanup()
    }
}
