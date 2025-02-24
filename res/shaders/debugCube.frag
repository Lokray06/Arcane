#version 330 core
in vec3 vLocalDir;
out vec4 FragColor;

uniform samplerCube prefilterMap;
uniform float debugMip; // Set this to 0.0 for the sharpest reflection.

void main()
{
    vec3 dir = normalize(vLocalDir);
    vec3 color = textureLod(prefilterMap, dir, debugMip).rgb;
    FragColor = vec4(color, 1.0);
}
