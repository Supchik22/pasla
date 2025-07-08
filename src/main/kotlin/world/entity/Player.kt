package io.github.supchik22.world.entity

import io.github.supchik22.Camera
import io.github.supchik22.InputHandler
import io.github.supchik22.phyc.AABB
import io.github.supchik22.rendering.ParticleSystem
import io.github.supchik22.util.GameTime
import io.github.supchik22.util.Ray
import io.github.supchik22.world.BlockRegistry
import io.github.supchik22.world.ChunkLoader
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.*
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class Player : PhysicalEntity() {

    override var pos = Vector3f(0f,200f,0f)

    val camera = Camera()
    val cameraHeightOffset = 1.6f

    public var loaded = false;

    private var lastMouseX: Double = 0.0
    private var lastMouseY: Double = 0.0
    private var firstMouseForCamera: Boolean = true

    // Змінено currentSpeed на вектор
    private val currentSpeed = Vector3f(0f, 0f, 0f)


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
    fun limitAirControl(
        currentVel: Vector3f,
        targetVel: Vector3f,
        maxAngleDeg: Float,
        deltaTime: Float,
        airAcceleration: Float
    ): Vector3f {
        val currentDir = Vector3f(currentVel.x, 0f, currentVel.z)
        val targetDir = Vector3f(targetVel.x, 0f, targetVel.z)

        if (currentDir.lengthSquared() < 0.001f) {
            return Vector3f(targetVel)
        }

        currentDir.normalize()
        targetDir.normalize()

        val dot = currentDir.dot(targetDir).coerceIn(-1f, 1f)
        val angle = Math.toDegrees(acos(dot.toDouble())).toFloat()

        if (angle <= maxAngleDeg) {
            val newVel = Vector3f()
            newVel.x = currentVel.x + (targetVel.x - currentVel.x) * airAcceleration * deltaTime
            newVel.z = currentVel.z + (targetVel.z - currentVel.z) * airAcceleration * deltaTime
            newVel.y = currentVel.y
            return newVel
        }

        val maxAngleRad = Math.toRadians(maxAngleDeg.toDouble()).toFloat()
        val axis = Vector3f(0f, 1f, 0f)
        val sinA = sin(maxAngleRad)
        val cosA = cos(maxAngleRad)

        val cross = Vector3f()
        axis.cross(currentDir, cross)

        val rotatedDir = Vector3f(
            currentDir.x * cosA + cross.x * sinA,
            0f,
            currentDir.z * cosA + cross.z * sinA
        ).normalize()

        val speed = Vector3f(currentVel.x, 0f, currentVel.z).length()

        val limitedVel = Vector3f(rotatedDir).mul(speed)
        limitedVel.y = currentVel.y

        limitedVel.x += (targetVel.x - limitedVel.x) * airAcceleration * deltaTime
        limitedVel.z += (targetVel.z - limitedVel.z) * airAcceleration * deltaTime

        return limitedVel
    }
    override fun render() {
        val target = Vector3f(pos.x, camera.position.y, pos.z)
        val lerpSpeed = 10f  // Чим більше — тим швидше камера "наздожене" гравця

        camera.position.lerp(target, (GameTime.getDeltaTime() * lerpSpeed).toFloat())

    }

    override fun updatePhysics(deltaTime: Float) {
        if ((loaded) && !last_frame_loaded ) {
            teleport(Vector3f(0f,500f,0f))
        }
        last_frame_loaded = loaded
        if (!loaded) return
        super.updatePhysics(deltaTime)

        if (onGround && velocity.length() > 5f) {

            val random = Random.Default
            if (random.nextBoolean()) {
                val vx = random.nextFloat() * 0.4f - 0.2f   // Від -0.2 до 0.2
                val vy = random.nextFloat() * 0.5f + 0.2f   // Від 0.2 до 0.7 (вгору)
                val vz = random.nextFloat() * 0.4f - 0.2f   // Від -0.2 до 0.2


                ParticleSystem.spawn(pos, Vector3f(vx,0.3f,vz),1f, Vector3f(0f,1f,0f),24f)
            }

        }

        val walkSpeed = 3.0f
        val sprintSpeed = 6.0f
        val crouchSpeed = 1.5f
        val jumpStrength = 5.0f
        val acceleration = 20.0f
        val airAcceleration = 4.0f
        val friction = if (onGround) 12.0f else 2.0f

        val isSprinting = InputHandler.isKeyDown(GLFW_KEY_LEFT_SHIFT)
        val isCrouching = InputHandler.isKeyDown(GLFW_KEY_LEFT_CONTROL)

        val targetSpeed = when {
            isCrouching -> crouchSpeed
            isSprinting -> sprintSpeed
            else -> walkSpeed
        }

        val input = Vector3f(
            (if (InputHandler.isKeyDown(GLFW_KEY_D)) 1f else 0f) - (if (InputHandler.isKeyDown(GLFW_KEY_A)) 1f else 0f),
            0f,
            (if (InputHandler.isKeyDown(GLFW_KEY_W)) 1f else 0f) - (if (InputHandler.isKeyDown(GLFW_KEY_S)) 1f else 0f)
        )

        if (input.lengthSquared() > 0f) input.normalize()

        val camForward = Vector3f()
        val camRight = Vector3f()
        camera.getFront(camForward)
        camera.getRight(camRight)

        camForward.y = 0f
        camRight.y = 0f
        camForward.normalize()
        camRight.normalize()

        val desiredDirection = Vector3f()
        desiredDirection.add(Vector3f(camForward).mul(input.z))
        desiredDirection.add(Vector3f(camRight).mul(input.x))

        if (desiredDirection.lengthSquared() > 0f) desiredDirection.normalize()

        val targetVelocity = Vector3f(desiredDirection).mul(targetSpeed)

        // Плавна інтерполяція currentSpeed (вектор)
        currentSpeed.lerp(targetVelocity, 10f * deltaTime)

        if (input.lengthSquared() > 0f) {
            if (onGround) {
                velocity.x += (currentSpeed.x - velocity.x) * acceleration * deltaTime
                velocity.z += (currentSpeed.z - velocity.z) * acceleration * deltaTime
            } else {
                val maxAirTurnAngle = 45f
                val limitedVel = limitAirControl(velocity, currentSpeed, maxAirTurnAngle, deltaTime, airAcceleration)
                velocity.x = limitedVel.x
                velocity.z = limitedVel.z
            }
        } else {
            if (onGround) {
                velocity.x *= (1f - deltaTime * friction)
                velocity.z *= (1f - deltaTime * friction)

                if (velocity.lengthSquared() < 0.01f) {
                    velocity.x = 0f
                    velocity.z = 0f
                }
            } else {
                val airFriction = 1f
                velocity.x *= (1f - deltaTime * airFriction)
                velocity.z *= (1f - deltaTime * airFriction)
            }
        }

        if (InputHandler.isKeyDown(GLFW_KEY_SPACE) && onGround) {
            velocity.y = jumpStrength
        }

        if (onGround && input.lengthSquared() > 0f) {
            val bobbingAmount = 0.02f
            val bobbingSpeed = 10f
            val offsetY = sin(System.currentTimeMillis() / 100.0 * bobbingSpeed) * bobbingAmount
            camera.position.y = pos.y + cameraHeightOffset + offsetY.toFloat()
        } else {
            camera.position.y = pos.y + cameraHeightOffset
        }


        handleBlockInteraction()


    }

    private fun handleBlockInteraction() {
        val ray = Ray(camera.position, camera.front)
        if (InputHandler.justMouseButtonDown(GLFW_MOUSE_BUTTON_RIGHT)) {
            val result = ray.cast(5f) { x, y, z -> ChunkLoader.isBlockSolidAt(x, y, z) }
            result?.let {
                val (bx, by, bz) = listOf(
                    it.adjacentAir.pos.x.toInt(),
                    it.adjacentAir.pos.y.toInt(),
                    it.adjacentAir.pos.z.toInt()
                )
                ChunkLoader.setBlock(bx, by, bz, BlockRegistry.TORCH.id)
                if (getAABB().intersects(AABB(bx.toFloat(), by.toFloat(), bz.toFloat(), bx + 1f, by + 1f, bz + 1f))) {
                    pos.y = by + 1f
                    velocity.y = 0f
                }
            }
        }

        if (InputHandler.justMouseButtonDown(GLFW_MOUSE_BUTTON_LEFT)) {
            val result = ray.cast(5f) { x, y, z -> ChunkLoader.isBlockSolidAt(x, y, z) }
            result?.let {
                val (bx, by, bz) = listOf(
                    it.hitBlock.pos.x.toInt(),
                    it.hitBlock.pos.y.toInt(),
                    it.hitBlock.pos.z.toInt()
                )
                ChunkLoader.setBlock(bx, by, bz, 0)
            }
        }
    }

}
