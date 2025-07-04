package io.github.supchik22.rendering

import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.system.MemoryUtil
import java.awt.Color

enum class CubeFace {
    FRONT, BACK, TOP, BOTTOM, RIGHT, LEFT
}


class Mesh(
    val vertices: FloatArray, // Позиції (x,y,z)
    val colors: FloatArray,   // Кольори (r,g,b)
    val indices: IntArray     // Індекси (для уникнення дублювання вершин)
) {
    var vboId: Int = 0
    var cboId: Int = 0 // Color Buffer Object ID
    var iboId: Int = 0 // Index Buffer Object ID
    var vertexCount: Int = 0

    fun create() {
        vertexCount = indices.size

        // Створення VBO для позицій
        vboId = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, vboId)
        val verticesBuffer = MemoryUtil.memAllocFloat(vertices.size)
        verticesBuffer.put(vertices).flip()
        glBufferData(GL_ARRAY_BUFFER, verticesBuffer, GL_STATIC_DRAW)
        MemoryUtil.memFree(verticesBuffer)

        // Створення CBO для кольорів
        cboId = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, cboId)
        val colorsBuffer = MemoryUtil.memAllocFloat(colors.size)
        colorsBuffer.put(colors).flip()
        glBufferData(GL_ARRAY_BUFFER, colorsBuffer, GL_STATIC_DRAW)
        MemoryUtil.memFree(colorsBuffer)

        // Створення IBO для індексів
        iboId = glGenBuffers()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, iboId)
        val indicesBuffer = MemoryUtil.memAllocInt(indices.size)
        indicesBuffer.put(indices).flip()
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW)
        MemoryUtil.memFree(indicesBuffer)

        glBindBuffer(GL_ARRAY_BUFFER, 0) // Відв'язати буфери
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    fun render() {
        // Увімкнути масиви вершин та кольорів
        glEnableClientState(GL_VERTEX_ARRAY)
        glEnableClientState(GL_COLOR_ARRAY)

        // Прив'язати VBO та CBO
        glBindBuffer(GL_ARRAY_BUFFER, vboId)
        glVertexPointer(3, GL_FLOAT, 0, 0L) // 3 компоненти (x,y,z) на вершину, 0 зміщення

        glBindBuffer(GL_ARRAY_BUFFER, cboId)
        glColorPointer(3, GL_FLOAT, 0, 0L) // 3 компоненти (r,g,b) на колір, 0 зміщення

        // Прив'язати IBO та намалювати
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, iboId)
        glDrawElements(GL_QUADS, vertexCount, GL_UNSIGNED_INT, 0L)

        // Відв'язати буфери
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        // Вимкнути масиви вершин та кольорів
        glDisableClientState(GL_COLOR_ARRAY)
        glDisableClientState(GL_VERTEX_ARRAY)
    }

    fun cleanup() {
        glDeleteBuffers(vboId)
        glDeleteBuffers(cboId)
        glDeleteBuffers(iboId)
    }
}

// Допоміжна функція для додавання даних граней до списків
fun addFaceData(
    faceVertices: FloatArray,
    faceColors: FloatArray,
    faceIndices: IntArray,
    currentOffset: Int,
    blockColor: Color,
    verticesList: MutableList<Float>,
    colorsList: MutableList<Float>,
    indicesList: MutableList<Int>
) {
    verticesList.addAll(faceVertices.asIterable())
    for (i in 0 until faceColors.size / 3) {
        colorsList.add(blockColor.red.toFloat() / 255f * faceColors[i * 3])
        colorsList.add(blockColor.green.toFloat() / 255f * faceColors[i * 3 + 1])
        colorsList.add(blockColor.blue.toFloat() / 255f * faceColors[i * 3 + 2])
    }
    indicesList.addAll(faceIndices.map { it + currentOffset }.asIterable())
}

// Дані для куба (8 вершин, 6 граней)
// Координати куба: від -1 до 1
val CUBE_VERTICES = arrayOf(
    // Front face (Z=1)
    floatArrayOf(-1f, -1f, 1f, 1f, -1f, 1f, 1f, 1f, 1f, -1f, 1f, 1f),
    // Back face (Z=-1)
    floatArrayOf(-1f, -1f, -1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f, -1f, -1f),
    // Top face (Y=1)
    floatArrayOf(-1f, 1f, -1f, -1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, -1f),
    // Bottom face (Y=-1)
    floatArrayOf(-1f, -1f, -1f, 1f, -1f, -1f, 1f, -1f, 1f, -1f, -1f, 1f),
    // Right face (X=1)
    floatArrayOf(1f, -1f, -1f, 1f, 1f, -1f, 1f, 1f, 1f, 1f, -1f, 1f),
    // Left face (X=-1)
    floatArrayOf(-1f, -1f, -1f, -1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f, -1f)
)

// Індекси для кожної грані (кожна грань - це 4 вершини, що формують 2 трикутники або 1 квад)
// Використовуємо порядок GL_QUADS (0,1,2,3)
val CUBE_INDICES = intArrayOf(0, 1, 2, 3)

// Кольори для кожної грані (для імітації освітлення)
val FACE_COLORS = arrayOf(
    floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f), // Front
    floatArrayOf(0.6f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f), // Back
    floatArrayOf(0.9f, 0.9f, 0.9f, 0.9f, 0.9f, 0.9f, 0.9f, 0.9f, 0.9f, 0.9f, 0.9f, 0.9f), // Top
    floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f), // Bottom
    floatArrayOf(0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f), // Right
    floatArrayOf(0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f)  // Left
)