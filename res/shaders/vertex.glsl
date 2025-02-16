#version 330 core

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inNormal;
layout(location = 2) in vec2 inTexCoord;
layout(location = 3) in vec3 inTangent; // Tangent attribute

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform mat4 lightSpaceMatrix;

out vec2 fragTexCoord;
out vec3 fragPos;
out vec4 fragPosLightSpace;

// Pass TBN basis vectors to the fragment shader.
out vec3 vNormal;
out vec3 vTangent;
out vec3 vBitangent;

void main() {
    vec4 worldPos = model * vec4(inPosition, 1.0);
    fragPos = worldPos.xyz;
    fragTexCoord = inTexCoord;
    fragPosLightSpace = lightSpaceMatrix * worldPos;

    // Transform normal and tangent into world space.
    vNormal = normalize(mat3(model) * inNormal);
    vTangent = normalize(mat3(model) * inTangent);
    vBitangent = normalize(cross(vNormal, vTangent));

    gl_Position = projection * view * worldPos;
}
