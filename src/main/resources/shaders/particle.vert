#version 330 core

layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aColor;
layout(location = 2) in float aSize;

out vec3 particleColor;

uniform mat4 projection;
uniform mat4 view;
uniform vec3 cameraPos;

void main() {
    vec4 worldPos = vec4(aPos, 1.0);
    gl_Position = projection * view * worldPos;

    float dist = length(cameraPos - aPos);

    float size = aSize / dist;

    gl_PointSize = clamp(size, 1.0, 100.0); // межі для стабільності

    particleColor = aColor;
}
