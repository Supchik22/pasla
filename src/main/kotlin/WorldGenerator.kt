package io.github.supchik22

import io.github.supchik22.rendering.GRASS_PLANT_ID
import kotlin.random.Random

class WorldGenerator {

    private val BASE_GROUND_HEIGHT = 10
    private val GROUND_LAYER_THICKNESS = 3

    private val AIR_ID: Short = 0
    private val DIRT_ID: Short = 2
    private val GRASS_ID: Short = 1
    private val STONE_ID: Short = 3

    private val HEIGHT_MAP_RES = 5

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
                val seed = worldX * 341873128712 + worldZ * 132897987541
                val rand = Random(seed)
                heightMap[hx][hz] = (BASE_GROUND_HEIGHT + rand.nextInt(-2, 2)).toFloat()
            }
        }

        // --- 2. Основний рельєф ---
        for (x_local in 0 until chunkSize) {
            for (z_local in 0 until chunkSize) {
                val fx = x_local / cellSize
                val fz = z_local / cellSize

                val ix = fx.toInt().coerceIn(0, HEIGHT_MAP_RES - 1)
                val iz = fz.toInt().coerceIn(0, HEIGHT_MAP_RES - 1)
                val dx = fx - ix
                val dz = fz - iz

                val h00 = heightMap[ix][iz]
                val h10 = heightMap[ix + 1][iz]
                val h01 = heightMap[ix][iz + 1]
                val h11 = heightMap[ix + 1][iz + 1]

                val h0 = h00 * (1 - dx) + h10 * dx
                val h1 = h01 * (1 - dx) + h11 * dx
                val surfaceHeight = ((h0 * (1 - dz) + h1 * dz)).toInt()

                for (y_local in 0 until chunkSize) {
                    val worldY = chunkWorldOffsetY + y_local
                    val index = x_local + z_local * chunkSize + y_local * chunkSize * chunkSize

                    if (index < blocks.size) {
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
        // --- 3. Генерація рослин (трави) ---
        if (chunkY == 0) { // тільки на нижньому шарі (поверхня)
            val rand = Random(worldX0 * 341873128712 + worldZ0 * 132897987541 + 9999)

            for (x_local in 0 until chunkSize) {
                for (z_local in 0 until chunkSize) {
                    for (y_local in chunkSize - 2 downTo 0) { // шукаємо верхній блок
                        val indexBelow = x_local + z_local * chunkSize + y_local * chunkSize * chunkSize
                        val indexAbove = indexBelow + chunkSize * chunkSize

                        if (blocks[indexBelow] == GRASS_ID && blocks[indexAbove] == AIR_ID) {
                            // 20% шанс посадити траву
                            if (rand.nextFloat() < 0.2f) {
                                blocks[indexAbove] = GRASS_PLANT_ID
                            }
                            break // перестаємо шукати далі в колонці
                        }
                    }
                }
            }
        }


    }


}
