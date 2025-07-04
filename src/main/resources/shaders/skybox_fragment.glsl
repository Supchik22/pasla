#version 330 core
out vec4 FragColor;

in vec3 WorldPos;

uniform vec3 skyColorTop;    // Колір неба в зеніті
uniform vec3 skyColorBottom; // Колір неба біля горизонту

void main()
{
    // Нормалізуємо WorldPos, щоб отримати вектор напрямку
    vec3 normalizedDir = normalize(WorldPos);

    // Використовуємо y-координату для визначення змішування кольорів.
    // y-координата буде в діапазоні [-1, 1]. Перетворимо її в [0, 1].
    float blendFactor = (normalizedDir.y + 1.0) / 2.0;

    // Змішуємо кольори
    FragColor = vec4(mix(skyColorBottom, skyColorTop, blendFactor), 1.0);
}