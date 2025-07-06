package io.github.supchik22

import kotlin.math.cos
import kotlin.math.sin

import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Math

class Camera {
    val position = Vector3f(0.0f, 17.0f, 0.0f) // Initial position, will be overridden by Player
    var pitch: Float = 0.0f // Rotation around X-axis (up/down look)
    var yaw: Float = -90.0f // Rotation around Y-axis (left/right look). Default to -90 for looking down positive Z.

    val front = Vector3f() // Vector pointing forward from the camera
    val up = Vector3f(0.0f, 1.0f, 0.0f) // Up vector, usually (0,1,0)
    val right = Vector3f() // Vector pointing to the right of the camera

    private val worldUp = Vector3f(0.0f, 1.0f, 0.0f) // Global up direction

    init {
        updateCameraVectors() // Calculate initial front, right, up vectors
    }

    fun getViewMatrix(): Matrix4f {
        // Create a new Vector3f for the target point by adding 'front' to 'position'
        val target = Vector3f(position).add(front)
        return Matrix4f().lookAt(position, target, up)
    }

    // Renamed for clarity and consistency
    fun updateCameraVectors() {
        // Calculate the new front vector
        front.x = (Math.cos(Math.toRadians(yaw.toDouble())) * Math.cos(Math.toRadians(pitch.toDouble()))).toFloat()
        front.y = Math.sin(Math.toRadians(pitch.toDouble())).toFloat()
        front.z = (Math.sin(Math.toRadians(yaw.toDouble())) * Math.cos(Math.toRadians(pitch.toDouble()))).toFloat()
        front.normalize() // Normalize to ensure it's a unit vector

        // Recalculate the right and up vectors
        front.cross(worldUp, right) // Right vector is cross product of front and worldUp
        right.normalize()

        right.cross(front, up) // Up vector is cross product of right and front
        up.normalize()
    }

    // Add getter functions for front and right vectors, as used by Player
    fun getFront(out: Vector3f) {
        out.set(front)
    }

    fun getRight(out: Vector3f) {
        out.set(right)
    }
}