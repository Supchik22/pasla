package io.github.supchik22.world

import java.awt.Color

class BlockEntry(val id: Short, val color: Color = Color.PINK, val solid: Boolean = true, val lightEmitted: Int = 0)

object BlockRegistry {
    private val entries = mutableMapOf<Short, BlockEntry>()
    private var nextId: Short = 0

    // Updated register function to accept lightEmitted
    fun register(color: Color, solid: Boolean = true, lightEmitted: Int = 0): BlockEntry {
        val id = nextId
        nextId = (nextId + 1).toShort()
        val blockEntry = BlockEntry(id, color, solid, lightEmitted) // Pass lightEmitted
        entries[id] = blockEntry
        return blockEntry
    }

    fun getEntry(id: Short): BlockEntry? {
        return entries[id]
    }

    // Updated isSolid to use BlockEntry's solid property
    fun isSolid(id: Short): Boolean {
        return entries[id]?.solid ?: false // Default to non-solid if entry not found (or handle as an error)
    }

    fun getLightEmitted(id: Short): Int {
        return entries[id]?.lightEmitted ?: 0
    }

    val AIR = register(Color.WHITE, solid = false)
    val GRASS = register(Color(55,195,44))
    val DIRT = register(Color.decode("#70553f"))
    val STONE = register(Color.decode("#999999"))
    val GRASS_PLANT = register(Color(80, 200, 60), solid = false)

    val MAPLE_LOG = register(Color(80, 200, 60), solid = true)
    val MAPLE_LEAVES = register(Color(80, 200, 60), solid = true)

    val TORCH = register(Color(255,255,0), solid = false, lightEmitted = 7)
}