#version 460 core
in vec4 FragPos;

uniform vec3 lightPos; // Ensure you pass the actual point light's world position here.
uniform float farPlane;

void main() {
    // Compute distance from the fragment to the light position.
    float lightDistance = length(FragPos.xyz - lightPos);
    // Normalize the distance to the [0, 1] range for depth comparison.
    lightDistance = lightDistance / farPlane;
    gl_FragDepth = lightDistance;
}
