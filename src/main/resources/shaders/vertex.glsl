#version 330 core

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec2 aTexCoord;
layout (location = 2) in float aLight;

out vec2 TexCoord;
out float Light;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

uniform vec3 cameraPos;
uniform float curvatureStrength = 0.0000;

void main()
{
    vec3 worldPos = vec3(model * vec4(aPos, 1.0));
    vec2 offset = worldPos.xz - cameraPos.xz;

    float dist = length(offset);
    worldPos.y -= dist * dist * curvatureStrength;

    gl_Position = projection * view * vec4(worldPos, 1.0);
    TexCoord = aTexCoord;
    Light = aLight;
}
