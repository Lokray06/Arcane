#version 460 core
in vec4 FragPos;

uniform vec3 lightPos;
uniform float farPlane;

void main() {
    float lightDistance = length(FragPos.xyz - lightPos);
    // Map to [0,1] range for depth comparison.
    lightDistance = lightDistance / farPlane;
    gl_FragDepth = lightDistance;
}
