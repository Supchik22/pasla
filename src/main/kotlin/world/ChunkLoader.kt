package io.github.supchik22.world

import io.github.supchik22.WorldGenerator
import io.github.supchik22.graphics.TextureAtlas
import io.github.supchik22.rendering.ChunkRendering
import io.github.supchik22.util.ChunkPos


import org.joml.Vector3f
import org.joml.Vector3i
import java.lang.Math.floorDiv
import java.lang.Math.floorMod
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.floor
import kotlin.random.Random

lateinit var globalWorldGenerator: WorldGenerator
lateinit var globalTextureAtlas: TextureAtlas

object ChunkLoader {

    private val loadedChunks = ConcurrentHashMap<ChunkPos, Chunk>()
    private val chunkRenderings = ConcurrentHashMap<ChunkPos, ChunkRendering>()

    private const val WORLD_SEED = 12345L
    private val random = Random(WORLD_SEED)

    const val RENDER_DISTANCE_CHUNKS = 4
    const val MAX_WORLD_HEIGHT_CHUNKS = 20

    fun initialize(worldGenerator: WorldGenerator, textureAtlas: TextureAtlas) {
        globalWorldGenerator = worldGenerator
        globalTextureAtlas = textureAtlas
    }

    fun loadChunk(chunkPos: ChunkPos, lod: Int = 1): Chunk {
        return loadedChunks.computeIfAbsent(chunkPos) {
            println("Generating new chunk at: $chunkPos (LOD $lod)")
            val newChunk = Chunk(Vector3f(chunkPos.x.toFloat(), chunkPos.y.toFloat(), chunkPos.z.toFloat()))

            globalWorldGenerator.generateChunkContent(
                chunkPos.x,
                chunkPos.y,
                chunkPos.z,
                newChunk.getBlocks(),
                Chunk.Companion.CHUNK_SIZE
            )

            val newChunkRendering = ChunkRendering(newChunk, globalWorldGenerator, globalTextureAtlas, lod)
            chunkRenderings[chunkPos] = newChunkRendering
            newChunk
        }
    }

    fun updateLoadedChunks(observerPosition: Vector3f) {
        val chunkSizeWorld = Chunk.Companion.CHUNK_SIZE * 2f
        val observerChunkX = (observerPosition.x / chunkSizeWorld).toInt()
        val observerChunkY = (observerPosition.y / chunkSizeWorld).toInt()
        val observerChunkZ = (observerPosition.z / chunkSizeWorld).toInt()

        val chunksToKeep = mutableSetOf<ChunkPos>()

        for (xOffset in -RENDER_DISTANCE_CHUNKS..RENDER_DISTANCE_CHUNKS) {
            for (zOffset in -RENDER_DISTANCE_CHUNKS..RENDER_DISTANCE_CHUNKS) {
                for (yChunkOffset in 0 until MAX_WORLD_HEIGHT_CHUNKS) {
                    val chunkX = observerChunkX + xOffset
                    val chunkY = yChunkOffset
                    val chunkZ = observerChunkZ + zOffset
                    val chunkPos = ChunkPos(chunkX, chunkY, chunkZ)

                    // Відстань у чанках
                    val dx = chunkX - observerChunkX
                    val dz = chunkZ - observerChunkZ
                    val distance2D = Math.sqrt((dx * dx + dz * dz).toDouble())

                    val lod = calculateLodForDistance(distance2D)
                    loadChunk(chunkPos, lod)
                    chunksToKeep.add(chunkPos)
                }
            }
        }

        val chunksToUnload = loadedChunks.keys.filter { it !in chunksToKeep }
        for (chunkPos in chunksToUnload) {
            unloadChunk(chunkPos)
        }
    }

    private fun calculateLodForDistance(distance: Double): Int {
        return when {
            distance < 5 -> 1
            distance < 10 -> 2
            distance < 20 -> 4
            else -> 8
        }
    }


    fun unloadChunk(chunkPos: ChunkPos) {
        println("Unloading chunk at: $chunkPos")
        loadedChunks.remove(chunkPos)
        chunkRenderings.remove(chunkPos)?.cleanup()
    }

    fun getAllChunkRenderings(): Collection<ChunkRendering> {
        return chunkRenderings.values
    }

    fun cleanupAllChunks() {
        println("Cleaning up all loaded chunks...")
        for (chunkPos in loadedChunks.keys) {
            unloadChunk(chunkPos)
        }
        loadedChunks.clear()
        chunkRenderings.clear()
    }

    fun getBlockAtWorld(x: Int, y: Int, z: Int): Short {
        val chunkSize = Chunk.Companion.CHUNK_SIZE
        val chunkX = floorDiv(x, chunkSize)
        val chunkY = floorDiv(y, chunkSize)
        val chunkZ = floorDiv(z, chunkSize)

        val chunkPos = ChunkPos(chunkX, chunkY, chunkZ)
        val chunk = loadedChunks[chunkPos] ?: return 0

        val localX = floorMod(x, chunkSize)
        val localY = floorMod(y, chunkSize)
        val localZ = floorMod(z, chunkSize)

        return chunk.getBlocks()[localX + localZ * chunkSize + localY * chunkSize * chunkSize]
    }
    fun getBlockAtWorldSafe(x: Int, y: Int, z: Int): Short? {
        val chunkSize = Chunk.Companion.CHUNK_SIZE
        val chunkX = floorDiv(x, chunkSize)
        val chunkY = floorDiv(y, chunkSize)
        val chunkZ = floorDiv(z, chunkSize)

        val chunkPos = ChunkPos(chunkX, chunkY, chunkZ)
        val chunk = loadedChunks[chunkPos] ?: return null

        val localX = floorMod(x, chunkSize)
        val localY = floorMod(y, chunkSize)
        val localZ = floorMod(z, chunkSize)

        return chunk.getBlocks()[localX + localZ * chunkSize + localY * chunkSize * chunkSize]
    }

    fun getChunkContainingPosition(pos: Vector3f): Chunk? {
        val chunkX = (pos.x / Chunk.CHUNK_SIZE).toInt()
        val chunkY = (pos.y / Chunk.CHUNK_SIZE).toInt()
        val chunkZ = (pos.z / Chunk.CHUNK_SIZE).toInt()

        return loadedChunks[ChunkPos(chunkX, chunkY, chunkZ)]
    }

    fun getBlockAtGlobalPos(globalPos: Vector3i): Short? {
        val chunkPos = ChunkPos(
            floor(globalPos.x.toFloat() / Chunk.CHUNK_SIZE).toInt(),
            floor(globalPos.y.toFloat() / Chunk.CHUNK_SIZE).toInt(),
            floor(globalPos.z.toFloat() / Chunk.CHUNK_SIZE).toInt()
        )

        val chunk = loadedChunks[chunkPos] ?: return null // Повернути null, якщо чанк не завантажений

        val localX = globalPos.x - chunk.pos.x.toInt()
        val localY = globalPos.y - chunk.pos.y.toInt()
        val localZ = globalPos.z - chunk.pos.z.toInt()

        return chunk.getBlock(localX, localY, localZ)
    }
}
