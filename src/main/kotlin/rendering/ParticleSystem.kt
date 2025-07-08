package io.github.supchik22.rendering

import io.github.supchik22.graphics.ShaderProgram
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL32.GL_PROGRAM_POINT_SIZE
import org.lwjgl.system.MemoryUtil

object ParticleSystem {

    data class Particle(
        var position: Vector3f,
        var velocity: Vector3f,
        var life: Float,
        var color: Vector3f,
        var size: Float
    )

    private val particles = mutableListOf<Particle>()
    private const val maxParticles = 1000

    private var vao = 0
    private var vbo = 0

    private lateinit var shader: ShaderProgram

    fun init() {
        shader = ShaderProgram("shaders/particle.vert", "shaders/particle.frag")

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

    fun spawn(position: Vector3f, velocity: Vector3f, life: Float, color: Vector3f, size: Float) {
        if (particles.size < maxParticles) {
            particles.add(
                Particle(
                    Vector3f(position),
                    Vector3f(velocity),
                    life,
                    Vector3f(color),
                    size
                )
            )
        }
    }

    fun update(deltaTime: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.position.add(Vector3f(p.velocity).mul(deltaTime))
            p.life -= deltaTime
            if (p.life <= 0f) iterator.remove()
        }
    }

    fun render(view: Matrix4f, projection: Matrix4f) {
        shader.use()
        shader.setUniform("view", view)
        shader.setUniform("projection", projection)

        val data = MemoryUtil.memAllocFloat(particles.size * 7)
        for (p in particles) {
            data.put(p.position.x).put(p.position.y).put(p.position.z)
            data.put(p.color.x).put(p.color.y).put(p.color.z)
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
