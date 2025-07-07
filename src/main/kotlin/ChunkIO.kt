package io.github.supchik22

import io.github.supchik22.util.ChunkPos
import io.github.supchik22.world.Chunk
import org.joml.Vector3f
import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.DataOutputStream
import java.io.FileInputStream

fun saveChunkToFile(chunk: Chunk) {
//    val dir = File("world/chunks")
//    dir.mkdirs()
//
//    val file = File(dir, "${chunk.pos.x}_${chunk.pos.y}_${chunk.pos.z}.chunk")
//    FileOutputStream(file).use { fos ->
//        DataOutputStream(fos).use { dos ->
//            val blocks = chunk.getBlocks()
//            for (block in blocks) {
//                dos.writeShort(block.toInt())
//            }
//        }
//    }
}

fun loadChunkFromFile(pos: ChunkPos): Chunk? {
    val file = File("world/chunks/${pos.x}_${pos.y}_${pos.z}.chunk")
    if (!file.exists()) return null

    val chunk = Chunk(Vector3f(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat()))
    val blocks = chunk.getBlocks()

    FileInputStream(file).use { fis ->
        DataInputStream(fis).use { dis ->
            for (i in blocks.indices) {
                blocks[i] = dis.readShort()
            }
        }
    }

    return chunk
}