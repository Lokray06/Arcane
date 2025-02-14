#version 330 core

in vec3 fragNormal;
in vec2 fragTexCoord;

out vec4 outColor;

uniform sampler2D uAlbedo;

void main()
{
    vec4 albedoColor = texture(uAlbedo, fragTexCoord);
    outColor = albedoColor;
}
