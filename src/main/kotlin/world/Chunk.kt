package io.github.supchik22.world

import org.joml.Vector3f
import org.joml.Vector3i

class Chunk(val pos: Vector3f) {
    companion object {
        const val CHUNK_SIZE = 16
        const val CHUNK_VOLUME = CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE
    }
    var isDirty: Boolean = true
    private val blocks: ShortArray = ShortArray(CHUNK_VOLUME)

    fun getBlocks(): ShortArray = blocks

    fun markDirty() {
        isDirty = true
    }


    private fun index(x: Int, y: Int, z: Int): Int {

        require(x in 0 until CHUNK_SIZE)
        require(y in 0 until CHUNK_SIZE)
        require(z in 0 until CHUNK_SIZE)
        return x + z * CHUNK_SIZE + y * CHUNK_SIZE * CHUNK_SIZE
    }

    fun setBlock(x: Int, y: Int, z: Int, id: Short) {
        blocks[index(x, y, z)] = id
    }

    fun setBlock(pos: Vector3i, id: Short) {
        setBlock(pos.x, pos.y, pos.z, id)
    }

    fun getBlock(x: Int, y: Int, z: Int): Short {
        return blocks[index(x, y, z)]
    }

    fun getBlock(pos: Vector3i): Short {
        return getBlock(pos.x, pos.y, pos.z)
    }

    fun isSolid(x: Int, y: Int, z: Int): Boolean {
        val id = getBlock(x, y, z)
        return id != 0.toShort() && id != 4.toShort()
    }

    fun isSolid(pos: Vector3i): Boolean {
        return isSolid(pos.x, pos.y, pos.z)
    }

    fun isInBounds(x: Int, y: Int, z: Int): Boolean {
        return x in 0 until CHUNK_SIZE &&
                y in 0 until CHUNK_SIZE &&
                z in 0 until CHUNK_SIZE
    }

    fun isInBounds(pos: Vector3i): Boolean {
        return isInBounds(pos.x, pos.y, pos.z)
    }

    fun needToBeRendered(pos: Vector3i): Boolean {
        if (!isSolid(pos)) return false

        val directions = arrayOf(
            Vector3i(0, 1, 0), Vector3i(0, -1, 0), // up, down
            Vector3i(0, 0, 1), Vector3i(0, 0, -1), // forward, back
            Vector3i(1, 0, 0), Vector3i(-1, 0, 0)  // right, left
        )

        for (dir in directions) {
            val neighborPos = Vector3i(pos).add(dir)
            if (!isInBounds(neighborPos) || !isSolid(neighborPos)) {
                return true
            }
        }

        return false
    }

    fun clearBlocks() {
        blocks.fill(0)
        markDirty()
    }

}
