#version 330 core

in vec2 fragTexCoord;

uniform sampler2D uAlbedo;

out vec4 outColor;

void main()
{
    //vec4 albedoColor = texture(uAlbedo, fragTexCoord);

    vec4 albedoColor = vec4(1,1,1,1);

    // Check if the texture was sampled correctly
    //if (albedoColor.a < 0.01) discard;

    outColor = albedoColor;
}
