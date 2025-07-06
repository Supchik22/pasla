package io.github.supchik22.world.entity

import io.github.supchik22.phyc.AABB
import io.github.supchik22.world.ChunkLoader
import org.joml.Vector3f
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.abs

open class PhysicalEntity : Entity() {

    open val pos = Vector3f(0f, 0f, 0f)
    val velocity = Vector3f(0f, 0f, 0f)

    val width = 0.6f
    val height = 1.8f

    var onGround = false

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

    open fun updatePhysics(deltaTime: Float) {
        if (!onGround) {
            velocity.y += -9.81f * deltaTime
        }

        if (velocity.y < -50f) velocity.y = -50f

        val steps = 10
        val step = Vector3f(
            velocity.x * deltaTime / steps,
            velocity.y * deltaTime / steps,
            velocity.z * deltaTime / steps
        )

        repeat(steps) {
            moveStep(step)
        }
    }

    private fun moveStep(step: Vector3f) {
        val originalStepY = step.y

        // --- Рух по Y ---
        pos.y += step.y
        var currentAABB = getAABB()

        var collidedY = false
        for (blockAABB in getCollisions(currentAABB)) {
            if (currentAABB.intersects(blockAABB)) {
                collidedY = true
                if (step.y < 0) {
                    // Підняти позицію гравця вгору на верхню межу блоку
                    pos.y = blockAABB.maxY
                    onGround = true
                } else {
                    // Опускаємо гравця під нижню межу блоку
                    pos.y = blockAABB.minY - height
                }
                velocity.y = 0f
                currentAABB = getAABB()
                break
            }
        }
        if (!collidedY) onGround = false

        // --- Рух по X ---
        pos.x += step.x
        currentAABB = getAABB()

        for (blockAABB in getCollisions(currentAABB)) {
            if (currentAABB.intersects(blockAABB)) {
                if (step.x > 0) {
                    pos.x = blockAABB.minX - width / 2f
                } else {
                    pos.x = blockAABB.maxX + width / 2f
                }
                velocity.x = 0f
                currentAABB = getAABB()
                break
            }
        }

        // --- Рух по Z ---
        pos.z += step.z
        currentAABB = getAABB()

        for (blockAABB in getCollisions(currentAABB)) {
            if (currentAABB.intersects(blockAABB)) {
                if (step.z > 0) {
                    pos.z = blockAABB.minZ - width / 2f
                } else {
                    pos.z = blockAABB.maxZ + width / 2f
                }
                velocity.z = 0f
                currentAABB = getAABB()
                break
            }
        }

        // --- Виштовхування при застряганні ---
        val collisions = getCollisions(currentAABB)
        for (blockAABB in collisions) {
            if (currentAABB.intersects(blockAABB)) {
                val overlapX1 = blockAABB.maxX - currentAABB.minX
                val overlapX2 = currentAABB.maxX - blockAABB.minX
                val overlapY1 = blockAABB.maxY - currentAABB.minY
                val overlapY2 = currentAABB.maxY - blockAABB.minY
                val overlapZ1 = blockAABB.maxZ - currentAABB.minZ
                val overlapZ2 = currentAABB.maxZ - blockAABB.minZ

                val overlapX = if (overlapX1 < overlapX2) overlapX1 else -overlapX2
                val overlapY = if (overlapY1 < overlapY2) overlapY1 else -overlapY2
                val overlapZ = if (overlapZ1 < overlapZ2) overlapZ1 else -overlapZ2

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
                        if (overlapY > 0) onGround = true
                    }
                    else -> {
                        pos.z += overlapZ
                        velocity.z = 0f
                    }
                }
                currentAABB = getAABB()
            }
        }
    }

    private fun getCollisions(aabb: AABB): List<AABB> {
        val list = mutableListOf<AABB>()

        val startX = floor(aabb.minX).toInt()
        val endX = ceil(aabb.maxX).toInt()
        val startY = floor(aabb.minY).toInt()
        val endY = ceil(aabb.maxY).toInt()
        val startZ = floor(aabb.minZ).toInt()
        val endZ = ceil(aabb.maxZ).toInt()

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
}
