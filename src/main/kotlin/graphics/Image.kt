package io.github.supchik22.graphics

import org.lwjgl.stb.STBImage.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

data class Image(val width: Int, val height: Int, val data: ByteBuffer) {
    fun free() {
        stbi_image_free(data)
    }
}

fun loadImage(path: String): Image {
    // Завантажуємо файл з ресурсів як байти
    val inputStream = Thread.currentThread().contextClassLoader.getResourceAsStream(path.removePrefix("/"))
        ?: throw RuntimeException("Resource not found: $path")
    val bytes = inputStream.readBytes()

    // Виділяємо пам'ять і копіюємо байти
    val buffer = MemoryUtil.memAlloc(bytes.size)
    buffer.put(bytes)
    buffer.flip()

    MemoryStack.stackPush().use { stack ->
        val width = stack.mallocInt(1)
        val height = stack.mallocInt(1)
        val channels = stack.mallocInt(1)

        val imageData = stbi_load_from_memory(buffer, width, height, channels, 4)
            ?: throw RuntimeException("Failed to load image: $path")

        MemoryUtil.memFree(buffer)
        return Image(width.get(), height.get(), imageData)
    }
}
