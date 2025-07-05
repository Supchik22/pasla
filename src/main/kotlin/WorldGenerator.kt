package io.github.supchik22

import kotlin.random.Random

class WorldGenerator {

    private val BASE_GROUND_HEIGHT = 100
    private val GROUND_LAYER_THICKNESS = 3

    private val AIR_ID: Short = 0
    private val DIRT_ID: Short = 2
    private val GRASS_ID: Short = 1
    private val STONE_ID: Short = 3

    private val HEIGHT_MAP_RES = 5

    // --- Tree specific parameters ---
    private val MIN_TRUNK_HEIGHT = 4
    private val MAX_TRUNK_HEIGHT = 6
    private val LEAF_RADIUS = 2 // How far leaves extend from the trunk horizontally

    fun generateChunkContent(chunkX: Int, chunkY: Int, chunkZ: Int, blocks: ShortArray, chunkSize: Int) {
        blocks.fill(AIR_ID)

        val chunkWorldOffsetY = chunkY * chunkSize
        val worldX0 = chunkX * chunkSize
        val worldZ0 = chunkZ * chunkSize

        // --- 1. Генеруємо мапу висот ---
        val heightMap = Array(HEIGHT_MAP_RES + 1) { FloatArray(HEIGHT_MAP_RES + 1) }
        val cellSize = chunkSize.toFloat() / HEIGHT_MAP_RES

        for (hx in 0..HEIGHT_MAP_RES) {
            for (hz in 0..HEIGHT_MAP_RES) {
                val worldX = worldX0 + (hx * cellSize).toInt()
                val worldZ = worldZ0 + (hz * cellSize).toInt()
                // Use a combination of worldX and worldZ for a consistent, seed-based random height
                // The large prime numbers help distribute the seed values
                val seed = worldX.toLong() * 341873128712L + worldZ.toLong() * 132897987541L
                val rand = Random(seed)
                heightMap[hx][hz] = (BASE_GROUND_HEIGHT + rand.nextInt(-2, 3)).toFloat() // Adjusted range for variation
            }
        }

        // --- 2. Основний рельєф ---
        for (x_local in 0 until chunkSize) {
            for (z_local in 0 until chunkSize) {
                val fx = x_local / cellSize
                val fz = z_local / cellSize

                val ix = fx.toInt().coerceIn(0, HEIGHT_MAP_RES) // Coerce to prevent out of bounds
                val iz = fz.toInt().coerceIn(0, HEIGHT_MAP_RES)

                // Adjust bounds for interpolation if ix or iz are at the max index
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

                    if (index < blocks.size) { // Always good to have this check
                        if (worldY < surfaceHeight) {
                            blocks[index] = when {
                                worldY < surfaceHeight - GROUND_LAYER_THICKNESS -> STONE_ID
                                worldY < surfaceHeight - 1 -> DIRT_ID
                                else -> GRASS_ID
                            }
                        } else {
                            blocks[index] = AIR_ID
                        }
                    }
                }
            }
        }
        // --- 3. Генерація рослин (трави та дерев) ---
        // Trees and plants should typically only generate if this is the "surface" chunk (Y=0, or Y at actual ground level).
        // Let's refine this to check the actual worldY of the surface.
        // It's more robust to generate features when blocks[indexBelow] is GRASS_ID and blocks[indexAbove] is AIR_ID,
        // which your current loop handles well.

        // Use the chunk's world coordinates for the random seed to ensure consistent tree placement
        val featureRand = Random(worldX0.toLong() * 987654321L + worldZ0.toLong() * 123456789L + 7777L) // Different seed for features

        for (x_local in 0 until chunkSize) {
            for (z_local in 0 until chunkSize) {
                // Iterate downwards from the top of the chunk to find the highest grass block
                for (y_local in chunkSize - 1 downTo 0) { // Start from top of chunk
                    val index = x_local + z_local * chunkSize + y_local * chunkSize * chunkSize

                    // Ensure we are not out of bounds when checking below/above
                    if (index >= blocks.size || index < 0) continue

                    // Check if the current block is air and the block below is grass
                    if (blocks[index] == AIR_ID && y_local > 0 && blocks[index - chunkSize * chunkSize] == GRASS_ID) {
                        // Found a potential surface block (y_local - 1 is grass, y_local is air)

                        // If it's the very top of the chunk and block below is grass, it's also a surface
                        val surfaceBlockYLocal = y_local - 1 // The y_local of the grass block

                        // Small plants (grass/flower)
                        if (featureRand.nextFloat() < 0.2f) { // 20% chance for a plant
                            // This depends on BlockRegistry.GRASS_PLANT being a valid ID.
                            // Ensure it's not the same as GRASS_ID (surface grass block).
                            blocks[index] = BlockRegistry.GRASS_PLANT.id // Place the plant in the air block above grass
                        }

                        // Trees
                        if (featureRand.nextFloat() < 0.01f) { // ~1% chance for a tree (adjust as needed)
                            // Pass the discovered grass block's local coordinates and the chunk's total size
                            generateTree(blocks, x_local, surfaceBlockYLocal, z_local, chunkSize)
                        }

                        break // Found the surface for this (x_local, z_local) column, move to next column
                    }
                }
            }
        }
    }

    /**
     * Generates a simple tree at the specified local chunk coordinates.
     * The tree is placed starting from the top of a GRASS_ID block.
     * This function performs boundary checks to ensure the tree stays within the chunk.
     */
    private fun generateTree(blocks: ShortArray, x: Int, y: Int, z: Int, chunkSize: Int) {
        val rand = Random(x.toLong() * 11 + y.toLong() * 13 + z.toLong() * 17) // Seed for this specific tree

        val trunkHeight = rand.nextInt(MIN_TRUNK_HEIGHT, MAX_TRUNK_HEIGHT + 1)
        val leafStartHeight = y + trunkHeight - 2 // Leaves start a bit below the very top of the trunk
        val leafEndHeight = y + trunkHeight + LEAF_RADIUS

        // --- Place Trunk ---
        for (h in 1..trunkHeight) {
            val currentY = y + h
            if (currentY < chunkSize) { // Check if trunk is within chunk Y bounds
                val index = x + z * chunkSize + currentY * chunkSize * chunkSize
                blocks[index] = BlockRegistry.MAPLE_LOG.id
            } else {
                // Trunk goes beyond chunk height, stop placing
                break
            }
        }

        // --- Place Leaves ---
        for (ly in leafStartHeight..leafEndHeight) {
            if (ly < 0 || ly >= chunkSize) continue // Ensure leaf layer is within chunk Y bounds

            // Calculate current radius based on height, making canopy somewhat rounded
            // Simpler: a fixed radius for now. More complex: varying radius per height
            val currentLeafRadius = when {
                ly == leafStartHeight -> LEAF_RADIUS - 1 // Smaller at bottom
                ly == leafEndHeight -> LEAF_RADIUS - 2 // Smaller at top
                else -> LEAF_RADIUS
            }

            for (lx in x - currentLeafRadius..x + currentLeafRadius) {
                for (lz in z - currentLeafRadius..z + currentLeafRadius) {
                    // Check if leaf position is within chunk X and Z bounds
                    if (lx >= 0 && lx < chunkSize && lz >= 0 && lz < chunkSize) {
                        // Check distance for a somewhat spherical/rounded shape
                        val distSq = (lx - x) * (lx - x) + (lz - z) * (lz - z)
                        if (distSq <= currentLeafRadius * currentLeafRadius + 1) { // +1 for slight squareness
                            val index = lx + lz * chunkSize + ly * chunkSize * chunkSize
                            // Place leaf only if it's air to avoid replacing trunk or ground
                            if (blocks[index] == AIR_ID) {
                                blocks[index] = BlockRegistry.MAPLE_LEAVES.id
                            }
                        }
                    }
                }
            }
        }
    }
}