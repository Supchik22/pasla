package io.github.supchik22

import io.github.supchik22.world.BlockRegistry
import kotlin.random.Random

data class PendingStructure(
    val globalX: Int,
    val globalY: Int,
    val globalZ: Int,
    val type: StructureType
)

enum class StructureType {
    TREE
}

class WorldGenerator {

    private val BASE_GROUND_HEIGHT = 100
    private val GROUND_LAYER_THICKNESS = 3

    private val AIR_ID: Short = 0
    private val DIRT_ID: Short = 2
    private val GRASS_ID: Short = 1
    private val STONE_ID: Short = 3

    private val HEIGHT_MAP_RES = 5

    private val MIN_TRUNK_HEIGHT = 4
    private val MAX_TRUNK_HEIGHT = 6
    private val LEAF_RADIUS = 2

    val pendingStructures = mutableListOf<PendingStructure>()

    fun generateChunkContent(
        chunkX: Int,
        chunkY: Int,
        chunkZ: Int,
        blocks: ShortArray,
        chunkSize: Int
    ) {
        blocks.fill(AIR_ID)

        val chunkWorldOffsetY = chunkY * chunkSize
        val worldX0 = chunkX * chunkSize
        val worldZ0 = chunkZ * chunkSize

        val heightMap = Array(HEIGHT_MAP_RES + 1) { FloatArray(HEIGHT_MAP_RES + 1) }
        val cellSize = chunkSize.toFloat() / HEIGHT_MAP_RES

        for (hx in 0..HEIGHT_MAP_RES) {
            for (hz in 0..HEIGHT_MAP_RES) {
                val worldX = worldX0 + (hx * cellSize).toInt()
                val worldZ = worldZ0 + (hz * cellSize).toInt()
                val seed = worldX.toLong() * 341873128712L + worldZ.toLong() * 132897987541L
                val rand = Random(seed)
                heightMap[hx][hz] = (BASE_GROUND_HEIGHT + rand.nextInt(-2, 3)).toFloat()
            }
        }

        for (x_local in 0 until chunkSize) {
            for (z_local in 0 until chunkSize) {
                val fx = x_local / cellSize
                val fz = z_local / cellSize

                val ix = fx.toInt().coerceIn(0, HEIGHT_MAP_RES)
                val iz = fz.toInt().coerceIn(0, HEIGHT_MAP_RES)

                val h00 = heightMap[ix][iz]
                val h10 = heightMap[(ix + 1).coerceAtMost(HEIGHT_MAP_RES)][iz]
                val h01 = heightMap[ix][(iz + 1).coerceAtMost(HEIGHT_MAP_RES)]
                val h11 = heightMap[(ix + 1).coerceAtMost(HEIGHT_MAP_RES)][(iz + 1).coerceAtMost(HEIGHT_MAP_RES)]

                val dx = fx - ix
                val dz = fz - iz

                val h0 = h00 * (1 - dx) + h10 * dx
                val h1 = h01 * (1 - dx) + h11 * dx
                val surfaceHeight = ((h0 * (1 - dz) + h1 * dz)).toInt()

                for (y_local in 0 until chunkSize) {
                    val worldY = chunkWorldOffsetY + y_local
                    val index = x_local + z_local * chunkSize + y_local * chunkSize * chunkSize

                    if (worldY < surfaceHeight) {
                        blocks[index] = when {
                            worldY < surfaceHeight - GROUND_LAYER_THICKNESS -> STONE_ID
                            worldY < surfaceHeight - 1 -> DIRT_ID
                            else -> GRASS_ID
                        }
                    }
                }
            }
        }

        val featureRand = Random(worldX0.toLong() * 987654321L + worldZ0.toLong() * 123456789L + 7777L)

        for (x_local in 0 until chunkSize) {
            for (z_local in 0 until chunkSize) {
                for (y_local in chunkSize - 1 downTo 1) {
                    val index = x_local + z_local * chunkSize + y_local * chunkSize * chunkSize
                    val belowIndex = index - chunkSize * chunkSize

                    if (blocks.getOrNull(index) == AIR_ID && blocks.getOrNull(belowIndex) == GRASS_ID) {
                        val surfaceY = chunkY * chunkSize + y_local - 1

                        if (featureRand.nextFloat() < 0.01f) {
                            val globalX = chunkX * chunkSize + x_local
                            val globalZ = chunkZ * chunkSize + z_local
                            pendingStructures.add(PendingStructure(globalX, surfaceY, globalZ, StructureType.TREE))
                        }

                        if (featureRand.nextFloat() < 0.2f) {
                            blocks[index] = BlockRegistry.GRASS_PLANT.id
                        }

                        break
                    }
                }
            }
        }
    }

    /**
     * Ця функція викликається після того, як всі чанки, які можуть містити частини структури, вже згенеровані.
     */
    fun populateStructures(
        chunkX: Int,
        chunkY: Int,
        chunkZ: Int,
        blocks: ShortArray,
        chunkSize: Int
    ) {
        val worldX0 = chunkX * chunkSize
        val worldY0 = chunkY * chunkSize
        val worldZ0 = chunkZ * chunkSize

        for (structure in pendingStructures) {
            if (structure.type == StructureType.TREE) {
                val x = structure.globalX
                val y = structure.globalY
                val z = structure.globalZ

                // Перевіряємо, чи частина дерева потрапляє в цей чанк
                val inChunk = x in worldX0 until (worldX0 + chunkSize) &&
                        y in worldY0 until (worldY0 + chunkSize) &&
                        z in worldZ0 until (worldZ0 + chunkSize)

                if (inChunk) {
                    placeTreeAt(blocks, x - worldX0, y - worldY0, z - worldZ0, chunkSize, x, y, z)
                }
            }
        }
    }

    private fun placeTreeAt(
        blocks: ShortArray,
        localX: Int,
        localY: Int,
        localZ: Int,
        chunkSize: Int,
        globalX: Int,
        globalY: Int,
        globalZ: Int
    ) {
        val rand = Random(globalX.toLong() * 11 + globalY.toLong() * 13 + globalZ.toLong() * 17)
        val trunkHeight = rand.nextInt(MIN_TRUNK_HEIGHT, MAX_TRUNK_HEIGHT + 1)
        val leafStartY = localY + trunkHeight - 2
        val leafEndY = localY + trunkHeight + LEAF_RADIUS

        for (h in 1..trunkHeight) {
            val y = localY + h
            if (y in 0 until chunkSize) {
                val index = localX + localZ * chunkSize + y * chunkSize * chunkSize
                if (index in blocks.indices) {
                    blocks[index] = BlockRegistry.MAPLE_LOG.id
                }
            }
        }

        for (ly in leafStartY..leafEndY) {
            if (ly !in 0 until chunkSize) continue

            val radius = when (ly) {
                leafStartY -> LEAF_RADIUS - 1
                leafEndY -> LEAF_RADIUS - 2
                else -> LEAF_RADIUS
            }

            for (lx in localX - radius..localX + radius) {
                for (lz in localZ - radius..localZ + radius) {
                    if (lx in 0 until chunkSize && lz in 0 until chunkSize) {
                        val dx = lx - localX
                        val dz = lz - localZ
                        if (dx * dx + dz * dz <= radius * radius + 1) {
                            val index = lx + lz * chunkSize + ly * chunkSize * chunkSize
                            if (blocks.getOrNull(index) == AIR_ID) {
                                blocks[index] = BlockRegistry.MAPLE_LEAVES.id
                            }
                        }
                    }
                }
            }
        }
    }
}
