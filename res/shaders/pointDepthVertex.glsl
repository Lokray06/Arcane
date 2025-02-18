#version 460 core

layout (location = 0) in vec3 inPosition;

uniform mat4 model;

void main() {
    // Multiply by the model matrix (which must be built with left‐handed conventions)
    gl_Position = model * vec4(inPosition, 1.0);
}
