#version 330 core

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aTexCoords;
layout (location = 3) in vec3 aTangent;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform mat4 lightSpaceMatrix;

// New uniforms for height mapping.
uniform sampler2D uHeightMap;
uniform float uHeightScale; // Controls the displacement amount.
uniform float scaleX;
uniform float scaleY;

out vec3 FragPos;           // World-space position.
out vec2 TexCoords;         // Texture coordinates.
out vec4 FragPosLightSpace; // Position in light space.

// Pass TBN basis vectors to the fragment shader.
out vec3 vNormal;
out vec3 vTangent;
out vec3 vBitangent;

void main()
{
    TexCoords = aTexCoords * vec2(scaleX, scaleY);
    // Sample the height from the height map (assumed grayscale, so red channel is sufficient)
    float height = texture(uHeightMap, TexCoords).r;
    // Displace the vertex position along its normal.
    vec3 displacedPos = aPos + aNormal * (height * uHeightScale);

    // Transform the displaced position to world space.
    vec4 worldPos = model * vec4(displacedPos, 1.0);
    FragPos = worldPos.xyz;
    FragPosLightSpace = lightSpaceMatrix * worldPos;

    // Transform normals and tangents using the model matrix.
    vNormal = normalize(mat3(model) * aNormal);
    vTangent = normalize(mat3(model) * aTangent);
    // Calculate bitangent.
    vBitangent = normalize(cross(vNormal, vTangent));

    gl_Position = projection * view * worldPos;
}
