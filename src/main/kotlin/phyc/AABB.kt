package io.github.supchik22.phyc

data class AABB(
    var minX: Float,
    var minY: Float,
    var minZ: Float,
    var maxX: Float,
    var maxY: Float,
    var maxZ: Float
) {
    fun offset(dx: Float, dy: Float, dz: Float): AABB {
        return AABB(minX + dx, minY + dy, minZ + dz,
            maxX + dx, maxY + dy, maxZ + dz)
    }

    fun intersects(other: AABB): Boolean {
        return maxX > other.minX && minX < other.maxX &&
                maxY > other.minY && minY < other.maxY &&
                maxZ > other.minZ && minZ < other.maxZ
    }
}
