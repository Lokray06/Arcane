#version 460 core

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inNormal;    // Vertex normal.
layout(location = 2) in vec2 inTexCoords; // Texture coordinates.

uniform mat4 model;
uniform mat4 lightSpaceMatrix;

// Uniforms for displacement.
uniform sampler2D uHeightMap;
uniform float uHeightScale;

// Uniforms for tiling the height map.
uniform float uScaleX;
uniform float uScaleY;

void main() {
    // Apply scaling to the texture coordinates.
    vec2 scaledTexCoords = inTexCoords * vec2(uScaleX, uScaleY);

    // Sample the grayscale height using the scaled texture coordinates.
    float height = texture(uHeightMap, scaledTexCoords).r;

    // Displace the vertex position along its normal.
    vec3 displacedPos = inPosition + inNormal * (height * uHeightScale);

    // Transform the displaced position.
    gl_Position = lightSpaceMatrix * model * vec4(displacedPos, 1.0);
}
