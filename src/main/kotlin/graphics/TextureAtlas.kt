package io.github.supchik22.graphics

import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.glActiveTexture
import org.lwjgl.opengl.GL30.glGenerateMipmap
import org.lwjgl.stb.STBImage
import org.lwjgl.stb.STBImage.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.math.ceil
import kotlin.math.sqrt

// Клас, що представляє атлас текстур та надає UV-координати для кожної текстури в ньому
class TextureAtlas(
    private val textureFilePaths: List<String>,
    private val textureSize: Int = 16
) {
    var textureId: Int = 0
    private var atlasWidth: Int = 0
    private var atlasHeight: Int = 0
    private var numTexturesX: Int = 0
    private var numTexturesY: Int = 0

    private val textureUvMap = mutableMapOf<Int, Pair<Float, Float>>()

    init {
        loadAtlas()
    }

    private fun loadAtlas() {
        if (textureFilePaths.isEmpty()) {
            throw IllegalArgumentException("Texture file paths cannot be empty for TextureAtlas.")
        }

        val numTextures = textureFilePaths.size
        val sqrtNumTextures = ceil(sqrt(numTextures.toDouble())).toInt()
        numTexturesX = sqrtNumTextures
        numTexturesY = sqrtNumTextures
        atlasWidth = numTexturesX * textureSize
        atlasHeight = numTexturesY * textureSize

        val atlasByteBuffer = ByteBuffer.allocateDirect(atlasWidth * atlasHeight * 4) // RGBA

        var currentX = 0
        var currentY = 0
        var textureIndex = 0

        for (path in textureFilePaths) {
            var imageBuffer: ByteBuffer? = null // Оголошуємо тут, щоб можна було звільнити
            try {
                // Використовуємо окремий MemoryStack тільки для widthBuf/heightBuf/channelsBuf
                MemoryStack.stackPush().use { stack ->
                    val widthBuf = stack.mallocInt(1)
                    val heightBuf = stack.mallocInt(1)
                    val channelsBuf = stack.mallocInt(1)

                    val resourcePath = if (path.startsWith("/")) path else "/$path"
                    val resourceStream = TextureAtlas::class.java.getResourceAsStream(resourcePath)
                        ?: throw RuntimeException("Failed to load texture file: $resourcePath. Resource stream is null (file not found).")

                    val imageBytes: ByteArray
                    try {
                        imageBytes = resourceStream.readBytes()
                    } catch (e: IOException) {
                        throw RuntimeException("Failed to read bytes from texture file: $resourcePath", e)
                    } finally {
                        resourceStream.close()
                    }

                    // --- ЗМІНА ТУТ: Використовуємо memAlloc для виділення на кучі замість stack.malloc ---
                    val imageDataBuffer = memAlloc(imageBytes.size) // Виділяємо на кучі LWJGL
                    imageDataBuffer.put(imageBytes).flip()
                    // --- Кінець зміни ---

                    imageBuffer = STBImage.stbi_load_from_memory(imageDataBuffer, widthBuf, heightBuf, channelsBuf, 4)
                        ?: throw RuntimeException("Failed to load texture file: $resourcePath. Reason: ${STBImage.stbi_failure_reason()}")

                    // --- ЗВІЛЬНЕННЯ imageDataBuffer: Після використання його потрібно звільнити вручну ---
                    memFree(imageDataBuffer)
                    // --- Кінець звільнення ---

                    val imgWidth = widthBuf.get(0)
                    val imgHeight = heightBuf.get(0)

                    if (imgWidth != textureSize || imgHeight != textureSize) {
                        throw IllegalArgumentException("Texture $path has size ${imgWidth}x${imgHeight}, but expected $textureSize x $textureSize.")
                    }

                    // Копіюємо дані зображення в буфер атласу
                    for (y in 0 until imgHeight) {
                        for (x in 0 until imgWidth) {
                            val pixelIndex = (y * imgWidth + x) * 4
                            val atlasPixelIndex = ((currentY + y) * atlasWidth + (currentX + x)) * 4
                            atlasByteBuffer.put(atlasPixelIndex, imageBuffer.get(pixelIndex))
                            atlasByteBuffer.put(atlasPixelIndex + 1, imageBuffer.get(pixelIndex + 1))
                            atlasByteBuffer.put(atlasPixelIndex + 2, imageBuffer.get(pixelIndex + 2))
                            atlasByteBuffer.put(atlasPixelIndex + 3, imageBuffer.get(pixelIndex + 3))
                        }
                    }
                } // stack.use{} блок закінчується тут, MemoryStack звільняється

            } finally {
                // Завжди звільняємо imageBuffer, який повертає stbi_load_from_memory
                if (imageBuffer != null) {
                    STBImage.stbi_image_free(imageBuffer)
                }
            }


            // Зберігаємо UV-координати для цієї текстури
            val u = currentX.toFloat() / atlasWidth.toFloat()
            val v = currentY.toFloat() / atlasHeight.toFloat()
            textureUvMap[textureIndex] = Pair(u, v)

            textureIndex++
            currentX += textureSize
            if (currentX >= atlasWidth) {
                currentX = 0
                currentY += textureSize
            }
        }
        atlasByteBuffer.flip()

        textureId = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, textureId)

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, atlasWidth, atlasHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, atlasByteBuffer)
        glGenerateMipmap(GL_TEXTURE_2D)

        glBindTexture(GL_TEXTURE_2D, 0)
    }

    fun getUvForTexture(textureId: Short): Pair<Float, Float> {
        val u = textureUvMap[textureId.toInt()]?.first ?: 0f
        val v = textureUvMap[textureId.toInt()]?.second ?: 0f
        return Pair(u, v)
    }

    fun getUvUnitSize(): Float = textureSize.toFloat() / atlasWidth.toFloat()

    fun bind() {
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, textureId)
    }

    fun unbind() {
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    fun cleanup() {
        glDeleteTextures(textureId)
    }
}