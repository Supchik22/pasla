package io.github.supchik22.util

import org.joml.Vector3f
import org.joml.Vector3i
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.roundToLong

// --- Float ---

fun Float.snap(gridSize: Float): Float {
    return (this / gridSize).roundToInt() * gridSize
}

fun Float.snapDown(gridSize: Float): Float {
    return floor(this / gridSize) * gridSize
}

// --- Double ---

fun Double.snap(gridSize: Double): Double {
    return (this / gridSize).roundToLong() * gridSize
}

fun Double.snapDown(gridSize: Double): Double {
    return floor(this / gridSize) * gridSize
}

// --- Int ---

fun Int.snap(gridSize: Int): Int {
    return ((this + gridSize / 2) / gridSize) * gridSize
}

// можна додати snapDown для Int, якщо треба
fun Int.snapDown(gridSize: Int): Int {
    return (this / gridSize) * gridSize
}

// --- Long ---

fun Long.snap(gridSize: Long): Long {
    return ((this + gridSize / 2) / gridSize) * gridSize
}

fun Long.snapDown(gridSize: Long): Long {
    return (this / gridSize) * gridSize
}

// --- Short ---

fun Short.snap(gridSize: Short): Short {
    val result = ((this.toInt() + gridSize.toInt() / 2) / gridSize.toInt()) * gridSize.toInt()
    return result.toShort()
}

fun Short.snapDown(gridSize: Short): Short {
    val result = floor(this.toDouble() / gridSize.toDouble()) * gridSize.toDouble()
    return result.toInt().toShort()
}

// --- Vector3i ---

fun Vector3i.snap(gridSize: Int) {
    this.x = this.x.snap(gridSize)
    this.y = this.y.snap(gridSize)
    this.z = this.z.snap(gridSize)
}

fun Vector3i.snapDown(gridSize: Int) {
    this.x = this.x.snapDown(gridSize)
    this.y = this.y.snapDown(gridSize)
    this.z = this.z.snapDown(gridSize)
}

fun Vector3f.toFloat(): Float {
    return this.length() // JOML's built-in method to calculate vector length
}

