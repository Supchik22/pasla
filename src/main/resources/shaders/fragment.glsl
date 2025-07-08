#version 330 core

in vec2 TexCoord;
in float Light;

uniform sampler2D ourTexture;
out vec4 FragColor;

void main()
{
    vec4 texColor = texture(ourTexture, TexCoord);

    if (texColor.a < 0.1)
    discard;

    FragColor = vec4(texColor.rgb * Light, texColor.a);
}
