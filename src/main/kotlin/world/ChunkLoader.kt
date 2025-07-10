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
import kotlin.math.sqrt
import kotlin.random.Random

lateinit var globalWorldGenerator: WorldGenerator
lateinit var globalTextureAtlas: TextureAtlas

object ChunkLoader {

    private val loadedChunks = ConcurrentHashMap<ChunkPos, Chunk>()
    private val chunkRenderings = ConcurrentHashMap<ChunkPos, ChunkRendering>()
    private val chunkPool = ArrayDeque<Chunk>(totalChunks())

    private const val WORLD_SEED = 12345L
    private val random = Random(WORLD_SEED)

    const val RENDER_DISTANCE_CHUNKS = 10

    fun totalChunks(): Int {
        val totalRangeChunks = (2 * RENDER_DISTANCE_CHUNKS + 1)
        return totalRangeChunks * totalRangeChunks * totalRangeChunks
    }

    fun initialize(worldGenerator: WorldGenerator, textureAtlas: TextureAtlas) {
        globalWorldGenerator = worldGenerator
        globalTextureAtlas = textureAtlas
    }

    fun areChunksLoadedAround(position: Vector3f, radiusChunks: Int = 1): Boolean {
        val chunkSize = Chunk.CHUNK_SIZE
        val centerChunkX = floorDiv(position.x.toInt(), chunkSize)
        val centerChunkY = floorDiv(position.y.toInt(), chunkSize)
        val centerChunkZ = floorDiv(position.z.toInt(), chunkSize)

        for (dx in -radiusChunks..radiusChunks) {
            for (dy in -radiusChunks..radiusChunks) {
                for (dz in -radiusChunks..radiusChunks) {
                    val checkPos = ChunkPos(centerChunkX + dx, centerChunkY + dy, centerChunkZ + dz)
                    if (!loadedChunks.containsKey(checkPos)) return false
                }
            }
        }
        return true
    }

    private fun obtainChunk(pos: ChunkPos): Chunk {
        val chunk = chunkPool.removeFirstOrNull() ?: Chunk(Vector3f())
        chunk.pos.set(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())
        chunk.clearBlocks()
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
        val chunkSize = Chunk.CHUNK_SIZE
        val observerChunkX = floorDiv(observerPosition.x.toInt(), chunkSize)
        val observerChunkY = floorDiv(observerPosition.y.toInt(), chunkSize)
        val observerChunkZ = floorDiv(observerPosition.z.toInt(), chunkSize)

        val chunksToKeep = mutableSetOf<ChunkPos>()

        for (xOffset in -RENDER_DISTANCE_CHUNKS..RENDER_DISTANCE_CHUNKS) {
            for (zOffset in -RENDER_DISTANCE_CHUNKS..RENDER_DISTANCE_CHUNKS) {
                for (yOffset in -RENDER_DISTANCE_CHUNKS..RENDER_DISTANCE_CHUNKS) {
                    val chunkX = observerChunkX + xOffset
                    val chunkY = observerChunkY + yOffset
                    val chunkZ = observerChunkZ + zOffset
                    val chunkPos = ChunkPos(chunkX, chunkY, chunkZ)

                    val dx = chunkX - observerChunkX
                    val dy = chunkY - observerChunkY
                    val dz = chunkZ - observerChunkZ
                    val distance3D = sqrt((dx * dx + dy * dy + dz * dz).toDouble())

                    val lod = calculateLodForDistance(distance3D)
                    loadChunk(chunkPos, lod)
                    chunksToKeep.add(chunkPos)
                }
            }
        }

        val chunksToUnload = loadedChunks.keys.filter { it !in chunksToKeep }
        for (chunkPos in chunksToUnload) unloadChunk(chunkPos)
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
        if (chunk != null && chunkPool.size < totalChunks()) {
            chunkPool.addLast(chunk)
        }
        chunkRenderings.remove(chunkPos)?.cleanup()
    }

    fun getAllChunkRenderings(): Collection<ChunkRendering> = chunkRenderings.values

    fun cleanupAllChunks() {
        println("Cleaning up all loaded chunks...")
        for (chunkPos in loadedChunks.keys) unloadChunk(chunkPos)
        loadedChunks.clear()
        chunkRenderings.clear()
    }

    fun getBlockAtWorld(x: Int, y: Int, z: Int): Short {
        val chunkSize = Chunk.CHUNK_SIZE
        val chunkX = floorDiv(x, chunkSize)
        val chunkY = floorDiv(y, chunkSize)
        val chunkZ = floorDiv(z, chunkSize)
        val chunk = loadedChunks[ChunkPos(chunkX, chunkY, chunkZ)] ?: return BlockRegistry.AIR.id

        val localX = floorMod(x, chunkSize)
        val localY = floorMod(y, chunkSize)
        val localZ = floorMod(z, chunkSize)
        return chunk.getBlock(localX, localY, localZ)
    }
    fun getBlockAtWorldSafe(v: Vector3i): Short? {
        return getBlockAtWorldSafe(v.x,v.y,v.z)
    }
    fun getBlockAtWorldSafe(x: Int, y: Int, z: Int): Short? {
        val chunkSize = Chunk.CHUNK_SIZE
        val chunkX = floorDiv(x, chunkSize)
        val chunkY = floorDiv(y, chunkSize)
        val chunkZ = floorDiv(z, chunkSize)
        val chunk = loadedChunks[ChunkPos(chunkX, chunkY, chunkZ)] ?: return null

        val localX = floorMod(x, chunkSize)
        val localY = floorMod(y, chunkSize)
        val localZ = floorMod(z, chunkSize)

        return if (chunk.isInBounds(localX, localY, localZ)) chunk.getBlock(localX, localY, localZ) else null
    }


    fun getChunkContainingPosition(pos: Vector3f): Chunk? {
        val chunkSize = Chunk.CHUNK_SIZE
        return loadedChunks[ChunkPos(floorDiv(pos.x.toInt(), chunkSize), floorDiv(pos.y.toInt(), chunkSize), floorDiv(pos.z.toInt(), chunkSize))]
    }

    fun getBlockAtGlobalPos(globalPos: Vector3i): Short? {
        val chunkSize = Chunk.CHUNK_SIZE
        val chunkPos = ChunkPos(floorDiv(globalPos.x, chunkSize), floorDiv(globalPos.y, chunkSize), floorDiv(globalPos.z, chunkSize))
        val chunk = loadedChunks[chunkPos] ?: return null

        val localX = floorMod(globalPos.x, chunkSize)
        val localY = floorMod(globalPos.y, chunkSize)
        val localZ = floorMod(globalPos.z, chunkSize)
        return chunk.getBlock(localX, localY, localZ)
    }

    fun isBlockSolidAt(x: Int, y: Int, z: Int): Boolean {
        return BlockRegistry.isSolid(getBlockAtWorld(x, y, z))
    }

    fun setBlock(x: Int, y: Int, z: Int, blockId: Short) {
        val oldBlockId = getBlockAtWorld(x, y, z)
        if (oldBlockId == blockId) return // Нічого не робити

        val chunkSize = Chunk.CHUNK_SIZE
        val chunkX = floorDiv(x, chunkSize)
        val chunkY = floorDiv(y, chunkSize)
        val chunkZ = floorDiv(z, chunkSize)
        val chunkPos = ChunkPos(chunkX, chunkY, chunkZ)
        val chunk = loadedChunks[chunkPos] ?: return

        val localX = floorMod(x, chunkSize)
        val localY = floorMod(y, chunkSize)
        val localZ = floorMod(z, chunkSize)

        // Встановити новий блок
        chunk.setBlock(localX, localY, localZ, blockId)
        chunk.markDirty()

        // Оновити меш сусідів, якщо блок знаходиться на межі чанка
        if (localX == 0 || localX == chunkSize - 1 || localY == 0 || localY == chunkSize - 1 || localZ == 0 || localZ == chunkSize - 1) {
            for (dx in -1..1) for (dy in -1..1) for (dz in -1..1) {
                if (dx == 0 && dy == 0 && dz == 0) continue
                val neighborPos = ChunkPos(chunkX + dx, chunkY + dy, chunkZ + dz)
                chunkRenderings[neighborPos]?.startMeshGeneration()
            }
        }
    }

    fun getChunkAt(pos: Vector3f): Chunk? = getChunkContainingPosition(pos)
}