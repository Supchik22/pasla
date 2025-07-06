package io.github.supchik22.world.entity

import io.github.supchik22.Camera
import io.github.supchik22.InputHandler
import io.github.supchik22.phyc.AABB
import io.github.supchik22.util.Ray
import io.github.supchik22.world.BlockRegistry
import io.github.supchik22.world.Chunk
import io.github.supchik22.world.ChunkLoader
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.GLFW_KEY_A
import org.lwjgl.glfw.GLFW.GLFW_KEY_D
import org.lwjgl.glfw.GLFW.GLFW_KEY_P
import org.lwjgl.glfw.GLFW.GLFW_KEY_S
import org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE
import org.lwjgl.glfw.GLFW.GLFW_KEY_W
import org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT
import org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT


class Player : PhysicalEntity() {

    override var pos = Vector3f(0f,200f,0f)

    val camera = Camera()
    val cameraHeightOffset = 1.6f

    public var loaded = false;

    private var lastMouseX: Double = 0.0
    private var lastMouseY: Double = 0.0
    private var firstMouseForCamera: Boolean = true

    init {
        InputHandler.addMouseMoveCallback { window, xpos, ypos ->
            if (firstMouseForCamera) {
                lastMouseX = xpos
                lastMouseY = ypos
                firstMouseForCamera = false
            }

            val dx = (xpos - lastMouseX).toFloat()
            val dy = (lastMouseY - ypos).toFloat()



            lastMouseX = xpos
            lastMouseY = ypos

            val mouseSensitivity = 0.1f

            camera.yaw += dx * mouseSensitivity
            camera.pitch += dy * mouseSensitivity

            if (camera.pitch > 89.0f) camera.pitch = 89.0f
            if (camera.pitch < -89.0f) camera.pitch = -89.0f

            camera.updateCameraVectors()
        }
    }

    var last_frame_loaded = false

    override fun updatePhysics(deltaTime: Float) {

        if ((loaded) && !last_frame_loaded ) {
            teleport(Vector3f(0f,500f,0f))
        }
        last_frame_loaded = loaded
        if (!loaded) {return}
        super.updatePhysics(deltaTime)

        val moveSpeed = 5.0f
        val jumpStrength = 5.0f


        val accelerationFactor = 17.0f
        var decelerationFactor = 1f
        if (onGround) {
            decelerationFactor = 9.0f
        } else { decelerationFactor = 3.0f }


        val movementInput = Vector3f(0f, 0f, 0f)

        if (InputHandler.isKeyDown(GLFW_KEY_W)) {
            movementInput.z += 1.0f
        }
        if (InputHandler.isKeyDown(GLFW_KEY_S)) {
            movementInput.z -= 1.0f
        }
        if (InputHandler.isKeyDown(GLFW_KEY_A)) {
            movementInput.x -= 1.0f
        }
        if (InputHandler.isKeyDown(GLFW_KEY_D)) {
            movementInput.x += 1.0f
        }

        if (movementInput.lengthSquared() > 0) {
            movementInput.normalize()
        }

        val cameraFrontHorizontal = Vector3f()
        camera.getFront(cameraFrontHorizontal)
        cameraFrontHorizontal.y = 0f
        cameraFrontHorizontal.normalize()

        val cameraRightHorizontal = Vector3f()
        camera.getRight(cameraRightHorizontal)
        cameraRightHorizontal.y = 0f
        cameraRightHorizontal.normalize()

        // Calculate the target horizontal velocity
        val targetVelocityX = (cameraFrontHorizontal.x * movementInput.z + cameraRightHorizontal.x * movementInput.x) * moveSpeed
        val targetVelocityZ = (cameraFrontHorizontal.z * movementInput.z + cameraRightHorizontal.z * movementInput.x) * moveSpeed

        // --- Apply smoothing to horizontal velocity ---
        // Якщо є ввід руху (movementInput не нульовий), використовуємо accelerationFactor
        if (movementInput.lengthSquared() > 0) {
            velocity.x = velocity.x + (targetVelocityX - velocity.x) * accelerationFactor * deltaTime
            velocity.z = velocity.z + (targetVelocityZ - velocity.z) * accelerationFactor * deltaTime
        } else {
            // Якщо вводу немає, поступово зменшуємо швидкість до нуля (симуляція тертя)
            velocity.x = velocity.x + (0f - velocity.x) * decelerationFactor * deltaTime
            velocity.z = velocity.z + (0f - velocity.z) * decelerationFactor * deltaTime

            // Додаткова перевірка, щоб уникнути "тремтіння", коли швидкість майже нульова
            if (velocity.lengthSquared() < 0.01f * 0.01f) { // Якщо швидкість дуже мала
                velocity.x = 0f
                velocity.z = 0f
            }
        }


        // --- Handle Jump ---
        if (InputHandler.isKeyDown(GLFW_KEY_SPACE) && onGround) {
            velocity.y = jumpStrength
        }
        if (InputHandler.justMouseButtonDown(GLFW_MOUSE_BUTTON_RIGHT)) {
            val ray = Ray(camera.position, camera.front)
            val result = ray.cast(
                maxDistance = 5f,
                isBlockSolid = { x, y, z -> ChunkLoader.isBlockSolidAt(x, y, z) }
            )

            if (result != null) {
                val blockPos = result.adjacentAir.pos
                val bx = blockPos.x.toInt()
                val by = blockPos.y.toInt()
                val bz = blockPos.z.toInt()

                ChunkLoader.setBlock(bx, by, bz, BlockRegistry.STONE.id)

                // Створюємо AABB для новозставленого блоку
                val blockAABB = AABB(
                    bx.toFloat(), by.toFloat(), bz.toFloat(),
                    bx + 1f, by + 1f, bz + 1f
                )

                // Отримуємо AABB гравця
                val playerAABB = getAABB()

                // Якщо блок перетинається з гравцем, виштовхуємо гравця вгору
                if (playerAABB.intersects(blockAABB)) {
                    pos.y = by + 1f
                    velocity.y = 0f
                }
            }
        }
        if (InputHandler.justMouseButtonDown(GLFW_MOUSE_BUTTON_LEFT)) {
            val ray = Ray(camera.position, camera.front)
            val result = ray.cast(
                maxDistance = 5f,
                isBlockSolid = { x, y, z -> ChunkLoader.isBlockSolidAt(x, y, z) }
            )

            if (result != null) {
                val blockPos = result.hitBlock.pos
                val bx = blockPos.x.toInt()
                val by = blockPos.y.toInt()
                val bz = blockPos.z.toInt()

                ChunkLoader.setBlock(bx, by, bz, 0) // 0 = повітря (або BlockRegistry.AIR.id)
            }
        }



        // --- Update Camera Position to Follow Player ---
        camera.position.set(pos.x, pos.y + cameraHeightOffset, pos.z)
    }
}