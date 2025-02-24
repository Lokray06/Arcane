#version 330 core
layout (location = 0) in vec3 aPos;
out vec3 vLocalDir;

uniform mat4 view;
uniform mat4 projection;

void main()
{
    // Remove translation so the cubemap appears static.
    mat4 viewNoTrans = mat4(mat3(view));
    vLocalDir = aPos;
    gl_Position = projection * viewNoTrans * vec4(aPos, 1.0);
}
