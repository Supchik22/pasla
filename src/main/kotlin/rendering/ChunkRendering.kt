package io.github.supchik22.rendering

import io.github.supchik22.world.BlockRegistry
import io.github.supchik22.world.Chunk
import io.github.supchik22.world.ChunkLoader // Assuming ChunkLoader can provide access to chunks for light calculation
import io.github.supchik22.WorldGenerator
import io.github.supchik22.graphics.GRASS_PLANT_TEXTURE_ID
import io.github.supchik22.graphics.TextureAtlas
import io.github.supchik22.graphics.getTextureIdForBlockFace
import io.github.supchik22.util.Face
import io.github.supchik22.world.ChunkLoader.getBlockAtWorldSafe
import org.joml.Vector3f
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.*
import org.lwjgl.system.MemoryUtil

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.CoroutineContext
import kotlin.math.max // Import max for light calculation

// Ця константа не змінюється
const val GRASS_PLANT_ID: Short = 4


object OpenGLCommandQueue {
    private val queue = ConcurrentLinkedQueue<() -> Unit>()

    fun runOnOpenGLThread(action: () -> Unit) {
        queue.add(action)
    }
    fun processCommands() {
        while (true) {
            val action = queue.poll() ?: break // Отримуємо та видаляємо наступну дію, або виходимо, якщо черга порожня
            action.invoke() // Виконуємо дію
        }
    }
}

object MainDispatcher : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        OpenGLCommandQueue.runOnOpenGLThread {
            block.run()

        }
    }
}

class ChunkRendering(
    val chunk: Chunk,
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

    // Скоуп для корутин, які виконуватимуть обчислювальні операції з генерації мешу
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var meshGenerationJob: Job? = null

    init {
        // Генеруємо ID для обох наборів буферів. Це безпечно робити тут, але завантаження даних
        // повинно відбуватися в OpenGL потоці.
        solidVaoId = glGenVertexArrays()
        solidVboId = glGenBuffers()
        solidEboId = glGenBuffers()

        transparentVaoId = glGenVertexArrays()
        transparentVboId = glGenBuffers()
        transparentEboId = glGenBuffers()

        // Запускаємо асинхронну генерацію мешу
        startMeshGeneration()
    }

    /**
     * Запускає асинхронну генерацію даних мешу.
     * Скасовує попередню генерацію, якщо вона ще не завершена.
     */
    fun startMeshGeneration() {
        meshGenerationJob?.cancel() // Скасовуємо попередню задачу генерації, якщо така є
        meshGenerationJob = scope.launch {
            try {
                // Виконуємо обчислення мешу у фоновому потоці (Dispatchers.Default)
                val (newSolidVertices, newSolidIndices, newTransparentVertices, newTransparentIndices) = generateMeshAsync()

                // Після того, як дані мешу згенеровані, переходимо в основний потік
                // (або потік OpenGL) для завантаження цих даних у GPU буфери.
                withContext(MainDispatcher) {
                    // Важливо: перевіряємо, чи корутина не була скасована під час очікування.
                    ensureActive()
                    updateOpenGLBuffers(newSolidVertices, newSolidIndices, newTransparentVertices, newTransparentIndices)
                }
            } catch (e: CancellationException) {
                // Завдання було скасовано (наприклад, чанк вивантажено)
                println("Chunk mesh generation cancelled for chunk ${chunk.pos}")
            } catch (e: Exception) {
                // Обробка інших помилок під час генерації мешу
                System.err.println("Error during chunk mesh generation for chunk ${chunk.pos}: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Асинхронно генерує дані мешу для чанка.
     * Ця функція виконується у фоновому потоці.
     *
     * @return Об'єкт [MeshData], що містить вершини та індекси для обох типів мешів.
     */
    private suspend fun generateMeshAsync(): MeshData = withContext(Dispatchers.Default) {
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
                    // Перевіряємо, чи потрібно скасувати корутину (наприклад, якщо чанк вийшов з діапазону)
                    ensureActive()

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
                            // Якщо сусідній блок null (за межами завантажених чанків) або не суцільний
                            neighborBlock == null || !BlockRegistry.isSolid(neighborBlock)
                        }
                    }

                    val blockWorldX = baseChunkPos.x + x
                    val blockWorldY = baseChunkPos.y + y
                    val blockWorldZ = baseChunkPos.z + z

                    // UV-координати функції
                    val u0 = { u: Float -> u }
                    val u1 = { u: Float -> u + uvUnitSize }
                    val v0 = { v: Float -> v }
                    val v1 = { v: Float -> v + uvUnitSize }

                    // --- РОЗПОДІЛ ГЕОМЕТРІЇ ---
                    if (blockId == GRASS_PLANT_ID) {
                        // Додаємо геометрію трави до прозорого мешу
                        val (u, v) = textureAtlas.getUvForTexture(GRASS_PLANT_TEXTURE_ID)
                        val currentVertexOffset = transparentVertices.size / 6 // Stride is now 6 (pos + tex + light)

                        val centerX = blockWorldX + 0.5f
                        val centerY = blockWorldY
                        val centerZ = blockWorldZ + 0.5f
                        val size = 0.5f
                        val height = 1.0f

                        // Light value for grass (consider average light around the block center)
                        val lightValue = getLightValue(blockWorldX.toInt(), blockWorldY.toInt(), blockWorldZ.toInt())

                        // Площина 1 (діагональ)
                        transparentVertices.addAll(listOf(
                            centerX - size, centerY,          centerZ - size, u0(u), v1(v), lightValue,
                            centerX + size, centerY,          centerZ + size, u1(u), v1(v), lightValue,
                            centerX + size, centerY + height, centerZ + size, u1(u), v0(v), lightValue,
                            centerX - size, centerY + height, centerZ - size, u0(u), v0(v), lightValue
                        ))
                        transparentIndices.addAll(faceIndices.map { it + currentVertexOffset })

                        // Площина 2 (перехресна)
                        val currentVertexOffset2 = transparentVertices.size / 6 // Stride is now 6
                        transparentVertices.addAll(listOf(
                            centerX - size, centerY,          centerZ + size, u0(u), v1(v), lightValue,
                            centerX + size, centerY,          centerZ - size, u1(u), v1(v), lightValue,
                            centerX + size, centerY + height, centerZ - size, u1(u), v0(v), lightValue,
                            centerX - size, centerY + height, centerZ + size, u0(u), v0(v), lightValue
                        ))
                        transparentIndices.addAll(faceIndices.map { it + currentVertexOffset2 })

                    } else {
                        // Додаємо геометрію суцільних блоків до основного мешу
                        // TOP face (+Y)
                        if (isFaceVisible(0, 1, 0)) {
                            val (u, v) = textureAtlas.getUvForTexture(getTextureIdForBlockFace(blockId, Face.TOP))
                            val currentVertexOffset = solidVertices.size / 6 // Stride is now 6
                            val lightValue = getLightValue(blockWorldX.toInt(), blockWorldY.toInt() + 1, blockWorldZ.toInt())
                            solidVertices.addAll(listOf(
                                blockWorldX,      blockWorldY + 1f, blockWorldZ,      u0(u), v0(v), lightValue,
                                blockWorldX + 1f, blockWorldY + 1f, blockWorldZ,      u1(u), v0(v), lightValue,
                                blockWorldX + 1f, blockWorldY + 1f, blockWorldZ + 1f, u1(u), v1(v), lightValue,
                                blockWorldX,      blockWorldY + 1f, blockWorldZ + 1f, u0(u), v1(v), lightValue
                            ))
                            solidIndices.addAll(faceIndices.map { it + currentVertexOffset })
                        }
                        // BOTTOM face (-Y)
                        if (isFaceVisible(0, -1, 0)) {
                            val (u, v) = textureAtlas.getUvForTexture(getTextureIdForBlockFace(blockId, Face.BOTTOM))
                            val currentVertexOffset = solidVertices.size / 6 // Stride is now 6
                            val lightValue = getLightValue(blockWorldX.toInt(), blockWorldY.toInt() - 1, blockWorldZ.toInt())
                            solidVertices.addAll(listOf(
                                blockWorldX,      blockWorldY, blockWorldZ + 1f, u0(u), v1(v), lightValue,
                                blockWorldX + 1f, blockWorldY, blockWorldZ + 1f, u1(u), v1(v), lightValue,
                                blockWorldX + 1f, blockWorldY, blockWorldZ,      u1(u), v0(v), lightValue,
                                blockWorldX,      blockWorldY, blockWorldZ,      u0(u), v0(v), lightValue
                            ))
                            solidIndices.addAll(faceIndices.map { it + currentVertexOffset })
                        }
                        // NORTH face (-Z)
                        if (isFaceVisible(0, 0, -1)) {
                            val (u, v) = textureAtlas.getUvForTexture(getTextureIdForBlockFace(blockId, Face.NORTH))
                            val currentVertexOffset = solidVertices.size / 6 // Stride is now 6
                            val lightValue = getLightValue(blockWorldX.toInt(), blockWorldY.toInt(), blockWorldZ.toInt() - 1)
                            solidVertices.addAll(listOf(
                                blockWorldX,      blockWorldY,      blockWorldZ, u0(u), v0(v), lightValue,
                                blockWorldX + 1f, blockWorldY,      blockWorldZ, u1(u), v0(v), lightValue,
                                blockWorldX + 1f, blockWorldY + 1f, blockWorldZ, u1(u), v1(v), lightValue,
                                blockWorldX,      blockWorldY + 1f, blockWorldZ, u0(u), v1(v), lightValue
                            ))
                            solidIndices.addAll(faceIndices.map { it + currentVertexOffset })
                        }
                        // SOUTH face (+Z)
                        if (isFaceVisible(0, 0, 1)) {
                            val (u, v) = textureAtlas.getUvForTexture(getTextureIdForBlockFace(blockId, Face.SOUTH))
                            val currentVertexOffset = solidVertices.size / 6 // Stride is now 6
                            val lightValue = getLightValue(blockWorldX.toInt(), blockWorldY.toInt(), blockWorldZ.toInt() + 1)
                            solidVertices.addAll(listOf(
                                blockWorldX + 1f, blockWorldY,      blockWorldZ + 1f, u0(u), v0(v), lightValue,
                                blockWorldX,      blockWorldY,      blockWorldZ + 1f, u1(u), v0(v), lightValue,
                                blockWorldX,      blockWorldY + 1f, blockWorldZ + 1f, u1(u), v1(v), lightValue,
                                blockWorldX + 1f, blockWorldY + 1f, blockWorldZ + 1f, u0(u), v1(v), lightValue
                            ))
                            solidIndices.addAll(faceIndices.map { it + currentVertexOffset })
                        }
                        // WEST face (-X)
                        if (isFaceVisible(-1, 0, 0)) {
                            val (u, v) = textureAtlas.getUvForTexture(getTextureIdForBlockFace(blockId, Face.WEST))
                            val currentVertexOffset = solidVertices.size / 6 // Stride is now 6
                            val lightValue = getLightValue(blockWorldX.toInt() - 1, blockWorldY.toInt(), blockWorldZ.toInt())
                            solidVertices.addAll(listOf(
                                blockWorldX, blockWorldY,      blockWorldZ + 1f, u0(u), v0(v), lightValue,
                                blockWorldX, blockWorldY,      blockWorldZ,      u1(u), v0(v), lightValue,
                                blockWorldX, blockWorldY + 1f, blockWorldZ,      u1(u), v1(v), lightValue,
                                blockWorldX, blockWorldY + 1f, blockWorldZ + 1f, u0(u), v1(v), lightValue
                            ))
                            solidIndices.addAll(faceIndices.map { it + currentVertexOffset })
                        }
                        // EAST face (+X)
                        if (isFaceVisible(1, 0, 0)) {
                            val (u, v) = textureAtlas.getUvForTexture(getTextureIdForBlockFace(blockId, Face.EAST))
                            val currentVertexOffset = solidVertices.size / 6 // Stride is now 6
                            val lightValue = getLightValue(blockWorldX.toInt() + 1, blockWorldY.toInt(), blockWorldZ.toInt())
                            solidVertices.addAll(listOf(
                                blockWorldX + 1f, blockWorldY,      blockWorldZ,      u0(u), v0(v), lightValue,
                                blockWorldX + 1f, blockWorldY,      blockWorldZ + 1f, u1(u), v0(v), lightValue,
                                blockWorldX + 1f, blockWorldY + 1f, blockWorldZ + 1f, u1(u), v1(v), lightValue,
                                blockWorldX + 1f, blockWorldY + 1f, blockWorldZ,      u0(u), v1(v), lightValue
                            ))
                            solidIndices.addAll(faceIndices.map { it + currentVertexOffset })
                        }
                    }
                }
            }
        }
        MeshData(solidVertices, solidIndices, transparentVertices, transparentIndices)
    }

    /**
     * Приватний клас даних для повернення згенерованих даних мешу.
     */
    private data class MeshData(
        val solidVertices: List<Float>,
        val solidIndices: List<Int>,
        val transparentVertices: List<Float>,
        val transparentIndices: List<Int>
    )

    /**
     * Завантажує згенеровані дані мешу в буфери OpenGL.
     * Ця функція ПОВИННА бути викликана в потоці, який має доступ до контексту OpenGL.
     *
     * @param solidVertices Список вершин для суцільних блоків.
     * @param solidIndices Список індексів для суцільних блоків.
     * @param transparentVertices Список вершин для прозорих блоків.
     * @param transparentIndices Список індексів для прозорих блоків.
     */
    private fun updateOpenGLBuffers(
        solidVertices: List<Float>,
        solidIndices: List<Int>,
        transparentVertices: List<Float>,
        transparentIndices: List<Int>
    ) {
        // Очищаємо старі дані буферів перед оновленням
        cleanupBuffersData()

        solidVertexCount = createAndBufferMesh(solidVaoId, solidVboId, solidEboId, solidVertices, solidIndices)
        transparentVertexCount = createAndBufferMesh(transparentVaoId, transparentVboId, transparentEboId, transparentVertices, transparentIndices)
    }

    /**
     * Створює та заповнює буфери Vertex Array Object (VAO), Vertex Buffer Object (VBO) та Element Buffer Object (EBO).
     *
     * @param vaoId ID VAO.
     * @param vboId ID VBO.
     * @param eboId ID EBO.
     * @param vertices Список координат вершин та текстурних координат.
     * @param indices Список індексів вершин.
     * @return Кількість індексів для відрисовки.
     */
    private fun createAndBufferMesh(vaoId: Int, vboId: Int, eboId: Int, vertices: List<Float>, indices: List<Int>): Int {
        if (vertices.isEmpty()) return 0

        glBindVertexArray(vaoId)

        // VBO (Vertex Buffer Object)
        glBindBuffer(GL_ARRAY_BUFFER, vboId)
        val verticesBuffer = MemoryUtil.memAllocFloat(vertices.size)
        verticesBuffer.put(vertices.toFloatArray()).flip()
        glBufferData(GL_ARRAY_BUFFER, verticesBuffer, GL_STATIC_DRAW) // Завантажуємо дані в VBO
        MemoryUtil.memFree(verticesBuffer) // Звільняємо пам'ять ByteBuffer

        // EBO (Element Buffer Object)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId)
        val indicesBuffer = MemoryUtil.memAllocInt(indices.size)
        indicesBuffer.put(indices.toIntArray()).flip()
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW) // Завантажуємо дані в EBO
        MemoryUtil.memFree(indicesBuffer) // Звільняємо пам'ять ByteBuffer

        // Vertex Attributes (Пояснення, як інтерпретувати дані у VBO)
        val stride = 6 * Float.SIZE_BYTES // 3D позиція (3 floats) + 2D текстурні координати (2 floats) + 1D світло (1 float) = 6 floats
        // Position attribute (layout location = 0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0)
        glEnableVertexAttribArray(0)
        // Texture coordinate attribute (layout location = 1)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, (3 * Float.SIZE_BYTES).toLong())
        glEnableVertexAttribArray(1)
        // Light attribute (layout location = 2) - NEW!
        glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, (5 * Float.SIZE_BYTES).toLong()) // Offset is after 3 pos + 2 tex
        glEnableVertexAttribArray(2)

        glBindVertexArray(0) // Відв'язуємо VAO

        // Кількість індексів для відрисовки
        return indices.size
    }

    /**
     * Очищає лише дані буферів (VBO, EBO), але не VAO.
     * Це потрібно перед повторним завантаженням нових даних мешу.
     */
    private fun cleanupBuffersData() {
        // Видаляємо старі буфери даних
        glDeleteBuffers(solidVboId)
        glDeleteBuffers(solidEboId)
        glDeleteBuffers(transparentVboId)
        glDeleteBuffers(transparentEboId)

        // Генеруємо нові ID для буферів. Це важливо, оскільки glBufferData
        // не завжди перевизначає пам'ять, якщо буфер був видалений.
        solidVboId = glGenBuffers()
        solidEboId = glGenBuffers()
        transparentVboId = glGenBuffers()
        transparentEboId = glGenBuffers()
    }

    /**
     * Рендерить меш чанка.
     * Ця функція повинна викликатися в головному потоці рендерингу.
     */
    fun render() {
        // Якщо чанк відмічено як "брудний" — оновлюємо його меш
        if (chunk.isDirty) {
            chunk.isDirty = false
            startMeshGeneration()
        }

        // 1. Рендеримо суцільні об'єкти
        if (solidVertexCount > 0) {
            glEnable(GL_CULL_FACE)
            glCullFace(GL_FRONT)
            glBindVertexArray(solidVaoId)
            glDrawElements(GL_TRIANGLES, solidVertexCount, GL_UNSIGNED_INT, 0)
            glCullFace(GL_BACK)
        }

        // 2. Рендеримо прозорі об'єкти
        if (transparentVertexCount > 0) {
            glDisable(GL_CULL_FACE)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

            glBindVertexArray(transparentVaoId)
            glDrawElements(GL_TRIANGLES, transparentVertexCount, GL_UNSIGNED_INT, 0)

            glDisable(GL_BLEND)
            glEnable(GL_CULL_FACE) // Re-enable culling after transparent objects
        }

        glBindVertexArray(0)
    }

    /**
     * Retrieves the combined light level (sky light and block light) for a given world coordinate.
     * This needs to safely query the ChunkLoader for the block's light.
     */
    private fun getLightValue(worldX: Int, worldY: Int, worldZ: Int): Float {
        val chunkX = Math.floorDiv(worldX, Chunk.CHUNK_SIZE)
        val chunkY = Math.floorDiv(worldY, Chunk.CHUNK_SIZE)
        val chunkZ = Math.floorDiv(worldZ, Chunk.CHUNK_SIZE)

        val localX = Math.floorMod(worldX, Chunk.CHUNK_SIZE)
        val localY = Math.floorMod(worldY, Chunk.CHUNK_SIZE)
        val localZ = Math.floorMod(worldZ, Chunk.CHUNK_SIZE)

        // Ensure you have a getChunkAt method in ChunkLoader
        val targetChunk = ChunkLoader.getChunkAt(Vector3f(chunkX.toFloat(), chunkY.toFloat(), chunkZ.toFloat()))

        return if (targetChunk != null && targetChunk.isInBounds(localX, localY, localZ)) {
            val blockLight = targetChunk.getBlockLight(localX, localY, localZ)
            val skyLight = targetChunk.getSkyLight(localX, localY, localZ)

            // Combine block light and sky light. Max of the two, or a more complex blending.
            // Normalize to a float between 0.0 and 1.0 (assuming light values are 0-15)
            // Added an ambient minimum light to prevent full blackness in shadows
            (max(blockLight, skyLight) / 15.0f) * 0.7f + 0.3f
        } else {
            // If chunk is not loaded or out of bounds, return a default ambient light
            0.5f // Default ambient light for unloaded areas
        }
    }


    /**
     * Повністю очищає всі ресурси OpenGL та скасовує активні корутини.
     * Повинна викликатися, коли об'єкт ChunkRendering більше не потрібен (наприклад, при вивантаженні чанка).
     */
    fun cleanup() {
        meshGenerationJob?.cancel() // Скасовуємо будь-яку активну роботу з генерації мешу
        scope.cancel() // Скасовуємо скоуп і всі корутини в ньому

        // Видаляємо всі VAO
        glDeleteVertexArrays(solidVaoId)
        glDeleteVertexArrays(transparentVaoId)

        // Видаляємо всі VBO та EBO
        cleanupBuffersData()
    }
}