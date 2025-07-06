package io.github.supchik22.world

import java.awt.Color

class BlockEntry(val id: Short, val color: Color = Color.PINK, val solid: Boolean = true)

object BlockRegistry {
    private val entries = mutableMapOf<Short, BlockEntry>()
    private var nextId: Short = 0

    fun register(color: Color, solid: Boolean = true): BlockEntry {
        val id = nextId
        nextId = (nextId + 1).toShort()
        val blockEntry = BlockEntry(id, color, solid)
        entries[id] = blockEntry
        return blockEntry
    }


    fun getEntry(id: Short): BlockEntry? {
        return entries[id]

    }

    fun isSolid(id: Short): Boolean {
        return id != AIR.id && id != GRASS_PLANT.id // наприклад, рослини не суцільні
    }


    val AIR = register(Color.WHITE, solid = false)
    val GRASS = register(Color(55,195,44))
    val DIRT = register(Color.decode("#70553f"))
    val STONE = register(Color.decode("#999999"))
    val GRASS_PLANT = register(Color(80, 200, 60), solid = false)

    val MAPLE_LOG = register(Color(80, 200, 60), solid = true)
    val MAPLE_LEAVES = register(Color(80, 200, 60), solid = true)





}
