package io.github.supchik22.util


import org.joml.Vector3f
import kotlin.math.floor

class Ray(
    val origin: Vector3f,
    val direction: Vector3f
) {

    /**
     * Визначає блок, у який влучає промінь, і сусідню позицію для розміщення блоку.
     * @param maxDistance Максимальна дальність рейкасту.
     * @param stepSize Крок між перевірками (менший — точніший).
     * @param isBlockSolid Функція, яка повертає true, якщо блок є твердим.
     * @return Пара: координати влученого блоку і сусіднього (або null, якщо нічого не знайдено)
     */
    fun cast(

        maxDistance: Float = 5.0f,
        stepSize: Float = 0.1f,
        isBlockSolid: (x: Int, y: Int, z: Int) -> Boolean
    ): HitResult? {
        val current = Vector3f()
        val lastAir = Vector3f()

        val normalizedDir = Vector3f(direction).normalize()

        for (i in 0 until (maxDistance / stepSize).toInt()) {
            current.set(normalizedDir).mul(i * stepSize).add(origin)

            val bx = floor(current.x).toInt()
            val by = floor(current.y).toInt()
            val bz = floor(current.z).toInt()

            if (isBlockSolid(bx, by, bz)) {
                val hit = BlockHit(Vector3f(bx.toFloat(), by.toFloat(), bz.toFloat()))
                val place = BlockHit(Vector3f(floor(lastAir.x).toInt().toFloat(), floor(lastAir.y).toInt().toFloat(), floor(lastAir.z).toInt().toFloat()))
                return HitResult(hit, place)
            }

            lastAir.set(current)
        }

        return null
    }

    data class BlockHit(val pos: Vector3f)
    data class HitResult(val hitBlock: BlockHit, val adjacentAir: BlockHit)
}
