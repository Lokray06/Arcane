#version 330 core
in vec2 fragTexCoord;
out vec4 outColor;

uniform sampler2D debugTexture;

void main() {
    // Sample the depth value and show it in grayscale.
    float depth = texture(debugTexture, fragTexCoord).r;
    outColor = vec4(vec3(depth), 1.0);
}
