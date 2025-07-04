#version 330 core

layout (location = 0) in vec3 aPos;  // Позиція вершини
layout (location = 1) in vec2 aTexCoord; // UV-координати

out vec2 TexCoord; // Передаємо UV-координати у фрагментний шейдер

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

void main()
{
    gl_Position = projection * view * model * vec4(aPos, 1.0);
    TexCoord = aTexCoord; // Передаємо UV-координати
}