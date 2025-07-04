package io.github.supchik22.rendering


import io.github.supchik22.BlockRegistry
import io.github.supchik22.Chunk
import io.github.supchik22.ChunkLoader.getBlockAtWorldSafe
import io.github.supchik22.WorldGenerator
import io.github.supchik22.graphics.GRASS_PLANT_TEXTURE_ID
import io.github.supchik22.graphics.TextureAtlas
import io.github.supchik22.graphics.getTextureIdForBlockFace
import io.github.supchik22.util.Face
import org.joml.Vector3f
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.*
import org.lwjgl.system.MemoryUtil

val GRASS_PLANT_ID: Short = 4

class ChunkRendering(
    private val chunk: Chunk,
    private val worldGenerator: WorldGenerator,
    private val textureAtlas: TextureAtlas,
    private val lod: Int = 1
) {
    // Меш для суцільних блоків (земля, камінь, і т.д.)
    private var solidVaoId: Int = 0
    private var solidVboId: Int = 0
    private var solidEboId: Int = 0
    private var solidVertexCount: Int = 0

    // Окремий меш для прозорих/нестандартних блоків (трава, квіти)
    private var transparentVaoId: Int = 0
    private var transparentVboId: Int = 0
    private var transparentEboId: Int = 0
    private var transparentVertexCount: Int = 0


    init {
        // Генеруємо ID для обох наборів буферів
        solidVaoId = glGenVertexArrays()
        solidVboId = glGenBuffers()
        solidEboId = glGenBuffers()

        transparentVaoId = glGenVertexArrays()
        transparentVboId = glGenBuffers()
        transparentEboId = glGenBuffers()

        setupMeshes()
    }

    private fun setupMeshes() {
        // Списки для геометрії двох мешів
        val solidVertices = mutableListOf<Float>()
        val solidIndices = mutableListOf<Int>()

        val transparentVertices = mutableListOf<Float>()
        val transparentIndices = mutableListOf<Int>()

        val blocks = chunk.getBlocks()
        val chunkSize = Chunk.Companion.CHUNK_SIZE
        val baseChunkPos = Vector3f(
            chunk.pos.x * chunkSize.toFloat(),
            chunk.pos.y * chunkSize.toFloat(),
            chunk.pos.z * chunkSize.toFloat()
        )

        val faceIndices = intArrayOf(0, 1, 2, 0, 2, 3) // Стандартні індекси для квадрата з двох трикутників
        val uvUnitSize = textureAtlas.getUvUnitSize()

        for (x in 0 until chunkSize) {
            for (y in 0 until chunkSize) {
                for (z in 0 until chunkSize) {
                    val blockId = blocks[x + z * chunkSize + y * chunkSize * chunkSize]
                    if (blockId == 0.toShort()) continue

                    val isFaceVisible = { dx: Int, dy: Int, dz: Int ->
                        val nx = x + dx
                        val ny = y + dy
                        val nz = z + dz

                        if (nx in 0 until chunkSize && ny in 0 until chunkSize && nz in 0 until chunkSize) {
                            val neighborBlockId = blocks[nx + nz * chunkSize + ny * chunkSize * chunkSize]
                            !BlockRegistry.isSolid(neighborBlockId) || neighborBlockId == GRASS_PLANT_ID // Трава не закриває грані
                        } else {
                            val worldX = chunk.pos.x * chunkSize + nx
                            val worldY = chunk.pos.y * chunkSize + ny
                            val worldZ = chunk.pos.z * chunkSize + nz
                            val neighborBlock = getBlockAtWorldSafe(worldX.toInt(), worldY.toInt(), worldZ.toInt())
                            neighborBlock == null || !BlockRegistry.isSolid(neighborBlock)
                        }
                    }

                    val blockWorldX = baseChunkPos.x + x
                    val blockWorldY = baseChunkPos.y + y
                    val blockWorldZ = baseChunkPos.z + z

                    // UV-координати
                    val u0 = { u: Float -> u }
                    val u1 = { u: Float -> u + uvUnitSize }
                    val v0 = { v: Float -> v }
                    val v1 = { v: Float -> v + uvUnitSize }

                    // --- РОЗПОДІЛ ГЕОМЕТРІЇ ---
                    if (blockId == GRASS_PLANT_ID) {
                        // Додаємо геометрію трави до прозорого мешу
                        val (u, v) = textureAtlas.getUvForTexture(GRASS_PLANT_TEXTURE_ID)
                        val currentVertexOffset = transparentVertices.size / 5

                        val centerX = blockWorldX + 0.5f
                        val centerY = blockWorldY
                        val centerZ = blockWorldZ + 0.5f
                        val size = 0.5f
                        val height = 1.0f

                        // Площина 1 (діагональ)
                        transparentVertices.addAll(listOf(
                            centerX - size, centerY,          centerZ - size, u0(u), v1(v),
                            centerX + size, centerY,          centerZ + size, u1(u), v1(v),
                            centerX + size, centerY + height, centerZ + size, u1(u), v0(v),
                            centerX - size, centerY + height, centerZ - size, u0(u), v0(v)
                        ))
                        transparentIndices.addAll(faceIndices.map { it + currentVertexOffset })

                        // Площина 2 (перехресна)
                        val currentVertexOffset2 = transparentVertices.size / 5
                        transparentVertices.addAll(listOf(
                            centerX - size, centerY,          centerZ + size, u0(u), v1(v),
                            centerX + size, centerY,          centerZ - size, u1(u), v1(v),
                            centerX + size, centerY + height, centerZ - size, u1(u), v0(v),
                            centerX - size, centerY + height, centerZ + size, u0(u), v0(v)
                        ))
                        transparentIndices.addAll(faceIndices.map { it + currentVertexOffset2 })

                    } else {
                        // Додаємо геометрію суцільних блоків до основного мешу
                        // TOP face (+Y)
                        if (isFaceVisible(0, 1, 0)) {
                            val (u, v) = textureAtlas.getUvForTexture(getTextureIdForBlockFace(blockId, Face.TOP))
                            val currentVertexOffset = solidVertices.size / 5
                            solidVertices.addAll(listOf(
                                blockWorldX,      blockWorldY + 1f, blockWorldZ,      u0(u), v0(v),
                                blockWorldX + 1f, blockWorldY + 1f, blockWorldZ,      u1(u), v0(v),
                                blockWorldX + 1f, blockWorldY + 1f, blockWorldZ + 1f, u1(u), v1(v),
                                blockWorldX,      blockWorldY + 1f, blockWorldZ + 1f, u0(u), v1(v)
                            ))
                            solidIndices.addAll(faceIndices.map { it + currentVertexOffset })
                        }
                        // BOTTOM face (-Y)
                        if (isFaceVisible(0, -1, 0)) {
                            val (u, v) = textureAtlas.getUvForTexture(getTextureIdForBlockFace(blockId, Face.BOTTOM))
                            val currentVertexOffset = solidVertices.size / 5
                            solidVertices.addAll(listOf(
                                blockWorldX,      blockWorldY, blockWorldZ + 1f, u0(u), v1(v),
                                blockWorldX + 1f, blockWorldY, blockWorldZ + 1f, u1(u), v1(v),
                                blockWorldX + 1f, blockWorldY, blockWorldZ,      u1(u), v0(v),
                                blockWorldX,      blockWorldY, blockWorldZ,      u0(u), v0(v)
                            ))
                            solidIndices.addAll(faceIndices.map { it + currentVertexOffset })
                        }
                        // NORTH face (-Z)
                        if (isFaceVisible(0, 0, -1)) {
                            val (u, v) = textureAtlas.getUvForTexture(getTextureIdForBlockFace(blockId, Face.NORTH))
                            val currentVertexOffset = solidVertices.size / 5
                            solidVertices.addAll(listOf(
                                blockWorldX,      blockWorldY,      blockWorldZ, u0(u), v0(v),
                                blockWorldX + 1f, blockWorldY,      blockWorldZ, u1(u), v0(v),
                                blockWorldX + 1f, blockWorldY + 1f, blockWorldZ, u1(u), v1(v),
                                blockWorldX,      blockWorldY + 1f, blockWorldZ, u0(u), v1(v)
                            ))
                            solidIndices.addAll(faceIndices.map { it + currentVertexOffset })
                        }
                        // SOUTH face (+Z)
                        if (isFaceVisible(0, 0, 1)) {
                            val (u, v) = textureAtlas.getUvForTexture(getTextureIdForBlockFace(blockId, Face.SOUTH))
                            val currentVertexOffset = solidVertices.size / 5
                            solidVertices.addAll(listOf(
                                blockWorldX + 1f, blockWorldY,      blockWorldZ + 1f, u0(u), v0(v),
                                blockWorldX,      blockWorldY,      blockWorldZ + 1f, u1(u), v0(v),
                                blockWorldX,      blockWorldY + 1f, blockWorldZ + 1f, u1(u), v1(v),
                                blockWorldX + 1f, blockWorldY + 1f, blockWorldZ + 1f, u0(u), v1(v)
                            ))
                            solidIndices.addAll(faceIndices.map { it + currentVertexOffset })
                        }
                        // WEST face (-X)
                        if (isFaceVisible(-1, 0, 0)) {
                            val (u, v) = textureAtlas.getUvForTexture(getTextureIdForBlockFace(blockId, Face.WEST))
                            val currentVertexOffset = solidVertices.size / 5
                            solidVertices.addAll(listOf(
                                blockWorldX, blockWorldY,      blockWorldZ + 1f, u0(u), v0(v),
                                blockWorldX, blockWorldY,      blockWorldZ,      u1(u), v0(v),
                                blockWorldX, blockWorldY + 1f, blockWorldZ,      u1(u), v1(v),
                                blockWorldX, blockWorldY + 1f, blockWorldZ + 1f, u0(u), v1(v)
                            ))
                            solidIndices.addAll(faceIndices.map { it + currentVertexOffset })
                        }
                        // EAST face (+X)
                        if (isFaceVisible(1, 0, 0)) {
                            val (u, v) = textureAtlas.getUvForTexture(getTextureIdForBlockFace(blockId, Face.EAST))
                            val currentVertexOffset = solidVertices.size / 5
                            solidVertices.addAll(listOf(
                                blockWorldX + 1f, blockWorldY,      blockWorldZ,      u0(u), v0(v),
                                blockWorldX + 1f, blockWorldY,      blockWorldZ + 1f, u1(u), v0(v),
                                blockWorldX + 1f, blockWorldY + 1f, blockWorldZ + 1f, u1(u), v1(v),
                                blockWorldX + 1f, blockWorldY + 1f, blockWorldZ,      u0(u), v1(v)
                            ))
                            solidIndices.addAll(faceIndices.map { it + currentVertexOffset })
                        }
                    }
                }
            }
        }

        // Створюємо та заповнюємо буфери для обох мешів
        solidVertexCount = createAndBufferMesh(solidVaoId, solidVboId, solidEboId, solidVertices, solidIndices)
        transparentVertexCount = createAndBufferMesh(transparentVaoId, transparentVboId, transparentEboId, transparentVertices, transparentIndices)
    }

    private fun createAndBufferMesh(vaoId: Int, vboId: Int, eboId: Int, vertices: List<Float>, indices: List<Int>): Int {
        if (vertices.isEmpty()) return 0

        glBindVertexArray(vaoId)

        // VBO
        glBindBuffer(GL_ARRAY_BUFFER, vboId)
        val verticesBuffer = MemoryUtil.memAllocFloat(vertices.size)
        verticesBuffer.put(vertices.toFloatArray()).flip()
        glBufferData(GL_ARRAY_BUFFER, verticesBuffer, GL_STATIC_DRAW)
        MemoryUtil.memFree(verticesBuffer)

        // EBO
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId)
        val indicesBuffer = MemoryUtil.memAllocInt(indices.size)
        indicesBuffer.put(indices.toIntArray()).flip()
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW)
        MemoryUtil.memFree(indicesBuffer)

        // Vertex Attributes
        val stride = 5 * Float.SIZE_BYTES
        // Position attribute
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0)
        glEnableVertexAttribArray(0)
        // Texture coordinate attribute
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, (3 * Float.SIZE_BYTES).toLong())
        glEnableVertexAttribArray(1)

        glBindVertexArray(0)

        // Кількість індексів для відрисовки
        return indices.size
    }

    fun render() {
        // 1. Рендеримо суцільні об'єкти
        if (solidVertexCount > 0) {
            glDisable(GL_CULL_FACE) // Рослини часто двосторонні
            glBindVertexArray(solidVaoId)
            glDrawElements(GL_TRIANGLES, solidVertexCount, GL_UNSIGNED_INT, 0)
            glEnable(GL_CULL_FACE)
        }

        // 2. Рендеримо прозорі об'єкти (після суцільних)
        // Тут можна додати специфічні налаштування OpenGL, наприклад glDisable(GL_CULL_FACE)
        if (transparentVertexCount > 0) {

            glDisable(GL_CULL_FACE) // Рослини часто двосторонні

            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)


            glBindVertexArray(transparentVaoId)
            glDrawElements(GL_TRIANGLES, transparentVertexCount, GL_UNSIGNED_INT, 0)

            glEnable(GL_CULL_FACE)
            glDisable(GL_BLEND)
        }

        glBindVertexArray(0)
    }

    fun cleanup() {
        // Видаляємо всі буфери
        glDeleteVertexArrays(solidVaoId)
        glDeleteBuffers(solidVboId)
        glDeleteBuffers(solidEboId)

        glDeleteVertexArrays(transparentVaoId)
        glDeleteBuffers(transparentVboId)
        glDeleteBuffers(transparentEboId)
    }
}