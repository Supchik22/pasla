package io.github.supchik22.world.entity

import io.github.supchik22.phyc.AABB
import io.github.supchik22.world.ChunkLoader
import org.joml.Vector3f
import org.joml.Vector3i
import kotlin.math.*

open class PhysicalEntity : Entity() {

    open val pos = Vector3f(0f, 0f, 0f)
    val velocity = Vector3f(0f, 0f, 0f)

    val width = 0.6f
    val height = 1.8f

    var onGround = false

    open var mass: Float = 1.0f

    fun teleport(p: Vector3f) {
        pos.set(p)
        velocity.set(0f, 0f, 0f)
    }

    fun getAABB(): AABB {
        val hw = width / 2f
        return AABB(
            pos.x - hw, pos.y, pos.z - hw,
            pos.x + hw, pos.y + height, pos.z + hw
        )
    }

    fun clampAirControlVelocity(currentVel: Vector3f, targetVel: Vector3f, maxAngleDeg: Float, deltaTime: Float, airAcceleration: Float): Vector3f {
        val currentDir = Vector3f(currentVel.x, 0f, currentVel.z)
        val targetDir = Vector3f(targetVel.x, 0f, targetVel.z)

        if (currentDir.lengthSquared() < 0.001f) {
            return Vector3f(targetVel)
        }

        currentDir.normalize()
        targetDir.normalize()

        val dot = currentDir.dot(targetDir).coerceIn(-1f, 1f)
        val angle = acos(dot.toDouble()).toFloat() * (180f / Math.PI.toFloat())

        val resultDir = if (angle <= maxAngleDeg) {
            targetDir
        } else {
            // Обертаємо поточний напрямок у бік цільового
            val maxAngleRad = Math.toRadians(maxAngleDeg.toDouble()).toFloat()
            val angleRatio = maxAngleRad / acos(dot.toDouble()).toFloat()
            currentDir.lerp(targetDir, angleRatio).normalize()
        }

        val speed = Vector3f(currentVel.x, 0f, currentVel.z).length()
        val limitedVel = Vector3f(resultDir).mul(speed)

        limitedVel.x += (targetVel.x - limitedVel.x) * airAcceleration * deltaTime / mass
        limitedVel.z += (targetVel.z - limitedVel.z) * airAcceleration * deltaTime / mass

        return Vector3f(limitedVel.x, currentVel.y, limitedVel.z)
    }
    /**
     * Повертає координати блоку безпосередньо під центром ентіті.
     * Добре підходить для простих перевірок, але може бути неточним,
     * якщо ентіті стоїть на межі кількох блоків.
     */

    fun getBlockUnderCenter(): Vector3i {
        val x = floor(pos.x).toInt()
        val y = floor(pos.y - 0.01f).toInt()
        val z = floor(pos.z).toInt()
        return Vector3i(x, y, z)
    }

    fun getBlockUnderFeet(): Vector3i {
        return Vector3i(
            floor(pos.x).toInt(),
            floor(pos.y - 0.01f).toInt(),
            floor(pos.z).toInt()
        )
    }

    open fun updatePhysics(deltaTime: Float) {
        if (!onGround) {
            velocity.y += (-9.81f * deltaTime)
        }

        if (velocity.y < -50f) velocity.y = -50f

        val steps = 10
        val step = Vector3f(
            velocity.x * deltaTime / steps,
            velocity.y * deltaTime / steps,
            velocity.z * deltaTime / steps
        )

        var wasOnGround = false
        repeat(steps) {
            if (moveStep(step)) {
                wasOnGround = true
            }
        }
        onGround = wasOnGround
    }

    // moveStep повертає true, якщо був контакт знизу
    private fun moveStep(step: Vector3f): Boolean {
        var touchedGround = false

        // Y-вісь
        pos.y += step.y
        var aabb = getAABB()
        for (block in getCollisions(aabb)) {
            if (aabb.intersects(block)) {
                // Зупиняємо рух по Y, не змінюємо положення
                pos.y -= step.y
                velocity.y = 0f
                if (step.y < 0f) touchedGround = true
                break
            }
        }

        // X-вісь
        pos.x += step.x
        aabb = getAABB()
        for (block in getCollisions(aabb)) {
            if (aabb.intersects(block)) {
                pos.x -= step.x
                velocity.x = 0f
                break
            }
        }

        // Z-вісь
        pos.z += step.z
        aabb = getAABB()
        for (block in getCollisions(aabb)) {
            if (aabb.intersects(block)) {
                pos.z -= step.z
                velocity.z = 0f
                break
            }
        }

        return touchedGround
    }


    private fun getCollisions(aabb: AABB): List<AABB> {
        val list = mutableListOf<AABB>()

        val startX = floor(aabb.minX).toInt()
        val endX = floor(aabb.maxX).toInt()
        val startY = floor(aabb.minY).toInt()
        val endY = floor(aabb.maxY).toInt()
        val startZ = floor(aabb.minZ).toInt()
        val endZ = floor(aabb.maxZ).toInt()

        for (x in startX..endX) {
            for (y in startY..endY) {
                for (z in startZ..endZ) {
                    if (ChunkLoader.isBlockSolidAt(x, y, z)) {
                        list.add(AABB(x.toFloat(), y.toFloat(), z.toFloat(), x + 1f, y + 1f, z + 1f))
                    }
                }
            }
        }
        return list
    }
    private fun resolveOverlaps(): Boolean {
        var resolved = false
        var aabb = getAABB()
        for (block in getCollisions(aabb)) {
            if (aabb.intersects(block)) {
                val overlapX1 = block.maxX - aabb.minX
                val overlapX2 = aabb.maxX - block.minX
                val overlapY1 = block.maxY - aabb.minY
                val overlapY2 = aabb.maxY - block.minY
                val overlapZ1 = block.maxZ - aabb.minZ
                val overlapZ2 = aabb.maxZ - block.minZ

                val overlapX = if (overlapX1 < overlapX2) -overlapX1 else overlapX2
                val overlapY = if (overlapY1 < overlapY2) -overlapY1 else overlapY2
                val overlapZ = if (overlapZ1 < overlapZ2) -overlapZ1 else overlapZ2

                val absX = abs(overlapX)
                val absY = abs(overlapY)
                val absZ = abs(overlapZ)

                when {
                    absX <= absY && absX <= absZ -> {
                        pos.x += overlapX
                        velocity.x = 0f
                    }
                    absY <= absX && absY <= absZ -> {
                        pos.y += overlapY
                        velocity.y = 0f
                        if (overlapY > 0f) onGround = true
                    }
                    else -> {
                        pos.z += overlapZ
                        velocity.z = 0f
                    }
                }

                resolved = true
                aabb = getAABB() // оновити AABB після зсуву
            }
        }
        return resolved
    }

}
