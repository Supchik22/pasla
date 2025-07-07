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
import kotlin.random.Random


lateinit var globalWorldGenerator: WorldGenerator
lateinit var globalTextureAtlas: TextureAtlas

object ChunkLoader {

    private val loadedChunks = ConcurrentHashMap<ChunkPos, Chunk>()
    private val chunkRenderings = ConcurrentHashMap<ChunkPos, ChunkRendering>()

    private val chunkPool = ArrayDeque<Chunk>(totalChunks()) // Оновлено розмір пулу

    private const val WORLD_SEED = 12345L
    private val random = Random(WORLD_SEED)

    const val RENDER_DISTANCE_CHUNKS = 7


    final fun totalChunks(): Int {
        val totalRangeChunks = (2 * RENDER_DISTANCE_CHUNKS + 1)
        return totalRangeChunks * totalRangeChunks * totalRangeChunks // Тепер враховуємо Y-вісь як X та Z
    }

    fun initialize(worldGenerator: WorldGenerator, textureAtlas: TextureAtlas) {
        globalWorldGenerator = worldGenerator
        globalTextureAtlas = textureAtlas
    }

    fun areChunksLoadedAround(position: Vector3f, radiusChunks: Int = 1): Boolean {
        val chunkSize = Chunk.CHUNK_SIZE
        val centerChunkX = Math.floorDiv(position.x.toInt(), chunkSize)
        val centerChunkY = Math.floorDiv(position.y.toInt(), chunkSize)
        val centerChunkZ = Math.floorDiv(position.z.toInt(), chunkSize)

        for (dx in -radiusChunks..radiusChunks) {
            for (dy in -radiusChunks..radiusChunks) {
                for (dz in -radiusChunks..radiusChunks) {
                    val checkPos = ChunkPos(centerChunkX + dx, centerChunkY + dy, centerChunkZ + dz)
                    if (!loadedChunks.containsKey(checkPos)) {
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun obtainChunk(pos: ChunkPos): Chunk {
        val chunk = chunkPool.removeFirstOrNull()
            ?: Chunk(Vector3f())

        chunk.pos.set(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())
        chunk.clearBlocks() // обнуляє блоки
        return chunk
    }

    fun loadChunk(chunkPos: ChunkPos, lod: Int = 1): Chunk {
        return loadedChunks.computeIfAbsent(chunkPos) {
            val newChunk = obtainChunk(chunkPos)

            globalWorldGenerator.generateChunkContent(
                chunkPos.x, chunkPos.y, chunkPos.z,
                newChunk.getBlocks(),
                Chunk.CHUNK_SIZE
            )

            if (!newChunk.isEmpty()) {
                val newChunkRendering = ChunkRendering(newChunk, globalWorldGenerator, globalTextureAtlas, lod)
                chunkRenderings[chunkPos] = newChunkRendering
            }

            newChunk
        }
    }

    fun updateLoadedChunks(observerPosition: Vector3f) {
        val chunkSizeWorld = Chunk.Companion.CHUNK_SIZE.toFloat()
        val observerChunkX = floorDiv(observerPosition.x.toInt(), chunkSizeWorld.toInt())
        val observerChunkY = floorDiv(observerPosition.y.toInt(), chunkSizeWorld.toInt())
        val observerChunkZ = floorDiv(observerPosition.z.toInt(), chunkSizeWorld.toInt())

        val chunksToKeep = mutableSetOf<ChunkPos>()

        for (xOffset in -RENDER_DISTANCE_CHUNKS..RENDER_DISTANCE_CHUNKS) {
            for (zOffset in -RENDER_DISTANCE_CHUNKS..RENDER_DISTANCE_CHUNKS) {
                // Змінено: тепер Y-вісь також використовує RENDER_DISTANCE_CHUNKS
                for (yOffset in -RENDER_DISTANCE_CHUNKS..RENDER_DISTANCE_CHUNKS) {
                    val chunkX = observerChunkX + xOffset
                    val chunkY = observerChunkY + yOffset // Тепер завантажуємо чанки відносно observerChunkY
                    val chunkZ = observerChunkZ + zOffset
                    val chunkPos = ChunkPos(chunkX, chunkY, chunkZ)

                    // Відстань у чанках (3D відстань для більш точного LOD)
                    val dx = chunkX - observerChunkX
                    val dy = chunkY - observerChunkY
                    val dz = chunkZ - observerChunkZ
                    val distance3D = Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble())

                    val lod = calculateLodForDistance(distance3D) // Використовуємо 3D відстань для LOD
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
        val chunk = loadedChunks.remove(chunkPos)
        if (chunk != null) {
            // Збільшуємо розмір пулу, якщо необхідно, або встановлюємо розумний ліміт
            if (chunkPool.size < totalChunks()) { // Пул не переповнений
                chunkPool.addLast(chunk)
            }
        }
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
        val chunkSize = Chunk.Companion.CHUNK_SIZE
        val chunkX = floorDiv(pos.x.toInt(), chunkSize)
        val chunkY = floorDiv(pos.y.toInt(), chunkSize)
        val chunkZ = floorDiv(pos.z.toInt(), chunkSize)

        return loadedChunks[ChunkPos(chunkX, chunkY, chunkZ)]
    }

    fun getBlockAtGlobalPos(globalPos: Vector3i): Short? {
        val chunkSize = Chunk.Companion.CHUNK_SIZE
        val chunkX = floorDiv(globalPos.x, chunkSize)
        val chunkY = floorDiv(globalPos.y, chunkSize)
        val chunkZ = floorDiv(globalPos.z, chunkSize)

        val chunkPos = ChunkPos(chunkX, chunkY, chunkZ)
        val chunk = loadedChunks[chunkPos] ?: return null

        val localX = floorMod(globalPos.x, chunkSize)
        val localY = floorMod(globalPos.y, chunkSize)
        val localZ = floorMod(globalPos.z, chunkSize)

        return chunk.getBlock(localX, localY, localZ)
    }

    fun isBlockSolidAt(x: Int, y: Int, z: Int): Boolean {
        val chunkX = x.floorDiv(Chunk.CHUNK_SIZE)
        val chunkY = y.floorDiv(Chunk.CHUNK_SIZE)
        val chunkZ = z.floorDiv(Chunk.CHUNK_SIZE)

        val chunk = loadedChunks[ChunkPos(chunkX, chunkY, chunkZ)] ?: return false

        val localX = x.mod(Chunk.CHUNK_SIZE)
        val localY = y.mod(Chunk.CHUNK_SIZE)
        val localZ = z.mod(Chunk.CHUNK_SIZE)

        val blockId = chunk.getBlock(localX, localY, localZ)
        return BlockRegistry.isSolid(blockId)
    }
    fun setBlock(x: Int, y: Int, z: Int, blockId: Short) {
        val chunkSize = Chunk.CHUNK_SIZE
        val chunkX = floorDiv(x, chunkSize)
        val chunkY = floorDiv(y, chunkSize)
        val chunkZ = floorDiv(z, chunkSize)

        val chunkPos = ChunkPos(chunkX, chunkY, chunkZ)

        val chunk = loadedChunks[chunkPos] ?: loadChunk(chunkPos)

        val localX = floorMod(x, chunkSize)
        val localY = floorMod(y, chunkSize)
        val localZ = floorMod(z, chunkSize)

        val index = localX + localZ * chunkSize + localY * chunkSize * chunkSize
        val blocks = chunk.getBlocks()

        if (index in blocks.indices) {
            blocks[index] = blockId
            chunk.markDirty()
        }
    }
}