#version 460 core

layout(location = 0) in vec3 aPos;
out vec3 TexCoords;
uniform mat4 view;
uniform mat4 projection;
void main()
{
    TexCoords = aPos;
    vec4 pos = projection * view * vec4(aPos, 1.0);
    // Force depth to 1.0 (far plane) so the skybox is rendered behind all geometry.
    gl_Position = pos.xyww;
}
