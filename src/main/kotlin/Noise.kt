package io.github.supchik22

import kotlin.math.*
import java.util.Random

class SimpleNoiseGenerator(seed: Int = 0) {

    private val perm = IntArray(256)

    init {
        for (i in 0 until 256) {
            perm[i] = i
        }
        val random = Random(seed.toLong())
        for (i in 255 downTo 1) {
            val j = random.nextInt(i + 1)
            val temp = perm[i]
            perm[i] = perm[j]
            perm[j] = temp
        }
    }

    // Всі аргументи та повернення тепер Float
    private fun fade(t: Float): Float = t * t * t * (t * (t * 6f - 15f) + 10f)
    private fun lerp(t: Float, a: Float, b: Float): Float = a + t * (b - a)

    // Градієнти також Float
    private val gradients = arrayOf(
        floatArrayOf(1f, 1f), floatArrayOf(-1f, 1f), floatArrayOf(1f, -1f), floatArrayOf(-1f, -1f),
        floatArrayOf(1f, 0f), floatArrayOf(-1f, 0f), floatArrayOf(0f, 1f), floatArrayOf(0f, -1f)
    )

    /**
     * Генерує 2D шум OpenSimplex.
     * @param x Координата X
     * @param y Координата Y
     * @param frequency Частота шуму (як сильно "зумити" шум)
     * @return Значення шуму в діапазоні приблизно від -1.0 до 1.0
     */
    // Всі параметри та повернення тепер Float
    fun getNoise(x: Float, y: Float, frequency: Float = 0.015f): Float { // Змінено frequency на Float
        val scaledX = x * frequency
        val scaledY = y * frequency

        var x0 = floor(scaledX).toInt()
        var y0 = floor(scaledY).toInt()
        val xf = scaledX - x0 // xf, yf тепер Float
        val yf = scaledY - y0

        x0 = x0 and 255
        y0 = y0 and 255

        val g00 = perm[perm[x0] + y0] % 8
        val g10 = perm[perm[x0 + 1] + y0] % 8
        val g01 = perm[perm[x0] + y0 + 1] % 8
        val g11 = perm[perm[x0 + 1] + y0 + 1] % 8

        // Передача Float в grad, який тепер також Float
        val n00 = grad(g00, xf, yf)
        val n10 = grad(g10, xf - 1f, yf)
        val n01 = grad(g01, xf, yf - 1f)
        val n11 = grad(g11, xf - 1f, yf - 1f)

        // Передача Float в fade, який тепер також Float
        val u = fade(xf)
        val v = fade(yf)

        val noiseValue = lerp(v, lerp(u, n00, n10), lerp(u, n01, n11))

        return noiseValue * 2f // Множення на Float
    }

    // Всі аргументи та повернення тепер Float
    private fun grad(hash: Int, x: Float, y: Float): Float {
        val g = gradients[hash]
        return x * g[0] + y * g[1]
    }
}