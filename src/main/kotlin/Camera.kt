package io.github.supchik22


import kotlin.math.cos
import kotlin.math.sin

import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Math

class Camera {
    val position = Vector3f(0.0f, 17.0f, 0.0f) // Start a bit higher to avoid being in the ground
    var pitch: Float = 0.0f // Rotation around X-axis (up/down look)
    var yaw: Float = -90.0f // Rotation around Y-axis (left/right look). Default to -90 for looking down positive Z.

    val front = Vector3f() // Vector pointing forward from the camera
    val up = Vector3f(0.0f, 1.0f, 0.0f) // Up vector, usually (0,1,0)
    val right = Vector3f() // Vector pointing to the right of the camera

    private val worldUp = Vector3f(0.0f, 1.0f, 0.0f) // Global up direction

    init {
        updateVectors() // Calculate initial front, right, up vectors
    }

    fun getViewMatrix(): Matrix4f {
        return Matrix4f().lookAt(position, Vector3f(position).add(front), up)
    }

    fun updateVectors() {
        // Calculate the new front vector
        front.x = (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))).toFloat()
        front.y = Math.sin(Math.toRadians(pitch)).toFloat()
        front.z = (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))).toFloat()
        front.normalize() // Normalize to ensure it's a unit vector

        // Recalculate the right and up vectors
        front.cross(worldUp, right) // Right vector is cross product of front and worldUp
        right.normalize()

        right.cross(front, up) // Up vector is cross product of right and front
        up.normalize()
    }

    // You can keep these move methods for clarity, though InputHandler directly uses vectors now.
    // They are not strictly necessary if InputHandler directly manipulates position using front/right/up.
    // If you plan to call these from MainKt, uncomment them.
    /*
    fun moveForward(amount: Float) { position.add(Vector3f(front).mul(amount)) }
    fun moveBackward(amount: Float) { position.sub(Vector3f(front).mul(amount)) }
    fun moveLeft(amount: Float) { position.sub(Vector3f(right).mul(amount)) }
    fun moveRight(amount: Float) { position.add(Vector3f(right).mul(amount)) }
    fun moveUp(amount: Float) { position.add(Vector3f(worldUp).mul(amount)) } // Moves strictly up in world space
    fun moveDown(amount: Float) { position.sub(Vector3f(worldUp).mul(amount)) } // Moves strictly down in world space
    */
}