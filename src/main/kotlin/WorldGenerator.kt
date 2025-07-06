package io.github.supchik22

import io.github.supchik22.world.BlockRegistry
import noise.FastNoiseLite
import kotlin.random.Random

class WorldGenerator {

    private val BASE_GROUND_HEIGHT = 100
    private val GROUND_LAYER_THICKNESS = 3

    private val AIR_ID: Short = 0
    private val DIRT_ID: Short = 2
    private val GRASS_ID: Short = 1
    private val STONE_ID: Short = 3

    private val HEIGHT_MAP_RES = 5

    private val amplitude = 20f       // висота горбів
    private val frequency = 0.01f     // частота шуму

    val noiseLite = FastNoiseLite().apply {
        SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2)
        SetFractalType(FastNoiseLite.FractalType.FBm)
        SetFractalOctaves(4)
        SetFrequency(frequency)
    }

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
                val noise = noiseLite.GetNoise(worldX.toFloat(), worldZ.toFloat()) // ∈ [-1, 1]
                heightMap[hx][hz] = BASE_GROUND_HEIGHT + noise * amplitude
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
                for (y_local in chunkSize - 1 downTo 0) {
                    val index = x_local + z_local * chunkSize + y_local * chunkSize * chunkSize
                    if (index !in blocks.indices) continue

                    val blockAtCurrentPos = blocks[index]
                    val blockBelowCurrentPos = blocks.getOrNull(index - chunkSize * chunkSize)

                    if (blockAtCurrentPos == AIR_ID && blockBelowCurrentPos == GRASS_ID) {
                        if (featureRand.nextFloat() < 0.2f) {
                            blocks[index] = BlockRegistry.GRASS_PLANT.id
                        }
                        break
                    }
                }
            }
        }
    }
}
