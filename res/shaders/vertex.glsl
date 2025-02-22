#version 330 core

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aTexCoords;
layout (location = 3) in vec3 aTangent;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform mat4 lightSpaceMatrix;

out vec3 FragPos;           // World-space position
out vec2 TexCoords;         // Texture coordinates
out vec4 FragPosLightSpace; // Position in light space

// Pass TBN basis vectors to the fragment shader.
out vec3 vNormal;
out vec3 vTangent;
out vec3 vBitangent;

void main()
{
    vec4 worldPos = model * vec4(aPos, 1.0);
    FragPos = worldPos.xyz;
    TexCoords = aTexCoords;
    FragPosLightSpace = lightSpaceMatrix * worldPos;

    // Transform normals and tangents using mat3(model)
    vNormal = normalize(mat3(model) * aNormal);
    vTangent = normalize(mat3(model) * aTangent);
    // Revert to the old ordering for the bitangent.
    vBitangent = normalize(cross(vNormal, vTangent));

    gl_Position = projection * view * worldPos;
}
