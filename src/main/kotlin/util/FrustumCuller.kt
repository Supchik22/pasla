package io.github.supchik22.util

import org.joml.FrustumIntersection
import org.joml.Matrix4f
import org.joml.Vector3f

object FrustumCuller {
    private val frustum = FrustumIntersection()

    fun updateFromMatrix(viewMatrix: Matrix4f, projectionMatrix: Matrix4f) {
        val viewProj = Matrix4f(projectionMatrix).mul(viewMatrix)
        frustum.set(viewProj)
    }

    fun isChunkVisible(chunkMin: Vector3f, chunkMax: Vector3f): Boolean {
        return frustum.testAab(chunkMin, chunkMax)
    }
}
