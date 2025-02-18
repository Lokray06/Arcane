#version 460 core

layout (triangles) in;
layout (triangle_strip, max_vertices = 18) out;

uniform mat4 shadowMatrices[6]; // These matrices must be built using lookAtLH with the canonical up vectors

out vec4 FragPos; // Pass the vertex position in world space

void main() {
    // For each cubemap face, transform the triangle
    for (int face = 0; face < 6; ++face) {
        for (int i = 0; i < 3; ++i) {
            // Pass along the world-space position (computed in the vertex shader)
            FragPos = gl_in[i].gl_Position;
            gl_Position = shadowMatrices[face] * FragPos;
            EmitVertex();
        }
        EndPrimitive();
    }
}
