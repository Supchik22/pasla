#version 330 core
layout (location = 0) in vec3 aPos;

uniform mat4 projection;
uniform mat4 view;

out vec3 WorldPos;

void main()
{
    // Видаляємо компоненти переміщення з View Matrix
    mat4 rotView = mat4(mat3(view));
    gl_Position = projection * rotView * vec4(aPos, 1.0);
    WorldPos = aPos; // Передаємо позицію для обчислення кольору
}