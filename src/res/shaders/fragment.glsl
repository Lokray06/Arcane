#version 330 core
in vec3 fragNormal;
in vec2 fragTexCoord;

out vec4 outColor;

void main()
{
    // A very basic shading: convert normals to colors.
    outColor = vec4(fragNormal * 0.5 + 0.5, 1.0);
}
