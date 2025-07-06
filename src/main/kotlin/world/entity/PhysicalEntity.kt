package io.github.supchik22.world.entity

import io.github.supchik22.phyc.AABB
import io.github.supchik22.world.Chunk
import org.joml.Vector3f
import kotlin.math.floor
import kotlin.math.ceil
import kotlin.math.abs // Додано для використання в логіці округлення

open class PhysicalEntity : Entity() {

    val pos = Vector3f(0f, 0f, 0f)
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

    open fun updatePhysics(chunk: Chunk, deltaTime: Float) {
        // Гравітація
        if (!onGround) velocity.y += -9.81f * deltaTime

        // Обмеження швидкості падіння
        if (velocity.y < -50f) velocity.y = -50f

        // Рух з поділом на кроки
        // Збільшимо кількість кроків для більшої точності, особливо при швидкому русі
        val steps = 10 // Збільшено кроки, щоб зменшити ймовірність "проскоку"
        val step = Vector3f(
            velocity.x * deltaTime / steps,
            velocity.y * deltaTime / steps,
            velocity.z * deltaTime / steps
        )

        repeat(steps) {
            moveStep(chunk, step)
        }
    }

    private fun moveStep(chunk: Chunk, step: Vector3f) {
        var currentAABB = getAABB()
        val originalStep = Vector3f(step) // Зберігаємо оригінальний крок для подальших розрахунків

        // Спочатку застосовуємо рух по Y (гравітація)
        currentAABB = currentAABB.offset(0f, step.y, 0f)
        var collidedY = false
        for (block in getCollisions(currentAABB, chunk)) {
            if (currentAABB.intersects(block)) {
                collidedY = true
                if (step.y < 0) { // Якщо рухаємося вниз (падаємо)
                    currentAABB = currentAABB.copy(minY = block.maxY) // Встановлюємо на верхню грань блоку
                } else if (step.y > 0) { // Якщо рухаємося вгору (стрибаємо в стелю)
                    currentAABB = currentAABB.copy(maxY = block.minY) // Встановлюємо на нижню грань блоку
                }
                velocity.y = 0f // Зупиняємо рух по Y
            }
        }
        // Оновлюємо onGround після обробки колізій по Y
        onGround = collidedY && originalStep.y < 0f // Використовуємо originalStep.y для коректного визначення падіння

        // Тепер застосовуємо рух по X
        currentAABB = currentAABB.offset(step.x, 0f, 0f)
        for (block in getCollisions(currentAABB, chunk)) {
            if (currentAABB.intersects(block)) {
                if (step.x > 0) currentAABB = currentAABB.copy(maxX = block.minX)
                else currentAABB = currentAABB.copy(minX = block.maxX)
                velocity.x = 0f
            }
        }

        // І, нарешті, рух по Z
        currentAABB = currentAABB.offset(0f, 0f, step.z)
        for (block in getCollisions(currentAABB, chunk)) {
            if (currentAABB.intersects(block)) {
                if (step.z > 0) currentAABB = currentAABB.copy(maxZ = block.minZ)
                else currentAABB = currentAABB.copy(minZ = block.maxZ)
                velocity.z = 0f
            }
        }

        // Оновлюємо позицію сутності на основі остаточної AABB
        pos.x = (currentAABB.minX + currentAABB.maxX) / 2f
        pos.y = currentAABB.minY // Важливо: встановлюємо на нижню межу AABB
        pos.z = (currentAABB.minZ + currentAABB.maxZ) / 2f
    }

    private fun getCollisions(aabb: AABB, chunk: Chunk): List<AABB> {
        val list = mutableListOf<AABB>()

        // Використовуємо floor і ceil для коректного визначення діапазону блоків,
        // які може перетинати AABB, враховуючи рух.
        // Додаємо невеликий епсілон (0.001f) до min/max, щоб уникнути проблем з рухомою точкою
        // і переконатися, що ми захоплюємо блоки, що знаходяться на межі.
        val startX = floor(aabb.minX - 0.001f).toInt().coerceIn(0, Chunk.CHUNK_SIZE - 1)
        val endX = ceil(aabb.maxX + 0.001f).toInt().coerceIn(0, Chunk.CHUNK_SIZE - 1)
        val startY = floor(aabb.minY - 0.001f).toInt().coerceIn(0, Chunk.CHUNK_SIZE - 1)
        val endY = ceil(aabb.maxY + 0.001f).toInt().coerceIn(0, Chunk.CHUNK_SIZE - 1)
        val startZ = floor(aabb.minZ - 0.001f).toInt().coerceIn(0, Chunk.CHUNK_SIZE - 1)
        val endZ = ceil(aabb.maxZ + 0.001f).toInt().coerceIn(0, Chunk.CHUNK_SIZE - 1)

        for (x in startX..endX) {
            for (y in startY..endY) {
                for (z in startZ..endZ) {
                    if (!chunk.isInBounds(x, y, z)) continue // Перевірка меж чанку
                    if (chunk.isSolid(x, y, z)) {
                        list.add(AABB(x.toFloat(), y.toFloat(), z.toFloat(), x + 1f, y + 1f, z + 1f))
                    }
                }
            }
        }
        return list
    }
}