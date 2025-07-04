package io.github.supchik22.graphics

import io.github.supchik22.util.Face

const val TEXTURE_AIR_ID: Short = 0
const val TEXTURE_DIRT_ID: Short = 1
const val TEXTURE_GRASS_TOP_ID: Short = 2
const val TEXTURE_STONE_ID: Short = 3
const val TEXTURE_GRASS_SIDE_ID: Short = 4
const val GRASS_PLANT_TEXTURE_ID: Short = 5

fun getTextureIdForBlockFace(blockId: Short, face: Face): Short {
    val result = when (blockId) {
        1.toShort() -> when (face) {
            Face.TOP -> TEXTURE_GRASS_TOP_ID
            Face.BOTTOM -> TEXTURE_DIRT_ID
            else -> TEXTURE_GRASS_SIDE_ID
        }
        2.toShort() -> TEXTURE_DIRT_ID
        3.toShort() -> TEXTURE_STONE_ID
        0.toShort() -> TEXTURE_AIR_ID
        4.toShort() -> GRASS_PLANT_TEXTURE_ID

        else -> TEXTURE_DIRT_ID
    }

    if (blockId == 1.toShort()) {
        println("GRASS block — face=$face => textureId=$result")
    }

    return result
}
