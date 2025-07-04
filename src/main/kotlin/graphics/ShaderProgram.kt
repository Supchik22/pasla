package io.github.supchik22.graphics
import org.joml.Matrix4f
import org.lwjgl.opengl.GL20.*
import org.lwjgl.system.MemoryStack
import java.nio.FloatBuffer


// Допоміжна функція для завантаження тексту з файлу
private fun loadResource(fileName: String): String {
    // ДОДАЙТЕ СЛЕШ НА ПОЧАТКУ ШЛЯХУ!
    return object {}.javaClass.getResource("/$fileName")?.readText() // <<< ЗМІНА ТУТ
        ?: throw RuntimeException("Resource '$fileName' not found")
}

class ShaderProgram(vertexShaderPath: String, fragmentShaderPath: String) {
    private val programId: Int = glCreateProgram()
    private var vertexShaderId: Int = 0
    private var fragmentShaderId: Int = 0
    private val uniforms: MutableMap<String, Int> = mutableMapOf()

    init {
        if (programId == 0) throw RuntimeException("Could not create ShaderProgram")

        // 1. Завантаження та компіляція вершинного шейдера
        vertexShaderId = createShader(loadResource(vertexShaderPath), GL_VERTEX_SHADER)
        glAttachShader(programId, vertexShaderId)

        // 2. Завантаження та компіляція фрагментного шейдера
        fragmentShaderId = createShader(loadResource(fragmentShaderPath), GL_FRAGMENT_SHADER)
        glAttachShader(programId, fragmentShaderId)

        // 3. Лінкування програми
        glLinkProgram(programId)
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            throw RuntimeException("Error linking ShaderProgram: ${glGetProgramInfoLog(programId, 1024)}")
        }

        // 4. Валідація програми (корисно для налагодження)
        glValidateProgram(programId)
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == 0) {
            System.err.println("Warning validating ShaderProgram: ${glGetProgramInfoLog(programId, 1024)}")
        }

        // 5. Отримання locations для uniform-змінних
        createUniform("projection")
        createUniform("view")
        createUniform("model")
        createUniform("ourTexture") // Для текстур
    }

    private fun createShader(shaderCode: String, shaderType: Int): Int {
        val shaderId = glCreateShader(shaderType)
        if (shaderId == 0) throw RuntimeException("Error creating shader. Type: $shaderType")

        glShaderSource(shaderId, shaderCode)
        glCompileShader(shaderId)

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            throw RuntimeException("Error compiling shader. Type: $shaderType. Info: ${glGetShaderInfoLog(shaderId, 1024)}")
        }
        return shaderId
    }

    fun createUniform(uniformName: String) {
        val uniformLocation = glGetUniformLocation(programId, uniformName)
        if (uniformLocation < 0) {
            // Це може бути нормально, якщо шейдер не використовує цей uniform
            System.err.println("Warning: Could not find uniform: $uniformName")
        }
        uniforms[uniformName] = uniformLocation
    }


    fun setUniform(uniformName: String, value: Int) {
        glUniform1i(uniforms[uniformName]!!, value)
    }

    fun setUniform(uniformName: String, value: Matrix4f) {
        MemoryStack.stackPush().use { stack ->
            val fb: FloatBuffer = stack.mallocFloat(16)
            value.get(fb)
            glUniformMatrix4fv(uniforms[uniformName]!!, false, fb)
        }
    }

    fun use() {
        glUseProgram(programId)
    }

    fun detach() {
        glUseProgram(0)
    }

    fun cleanup() {
        detach()
        if (programId != 0) {
            glDetachShader(programId, vertexShaderId)
            glDetachShader(programId, fragmentShaderId)
            glDeleteShader(vertexShaderId)
            glDeleteShader(fragmentShaderId)
            glDeleteProgram(programId)
        }
    }
}