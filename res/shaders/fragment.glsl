#version 330 core

in vec3 fragNormal;
in vec2 fragTexCoord;
in vec3 fragPos;

out vec4 outColor;

uniform sampler2D uAlbedo;
uniform vec3 viewPos;

struct DirectionalLight {
    vec3 direction;
    vec3 color;
    float strength;
};

#define MAX_DIR_LIGHTS 10
uniform int numDirectionalLights;
uniform DirectionalLight directionalLights[MAX_DIR_LIGHTS];

void main()
{
    vec4 albedoColor = texture(uAlbedo, fragTexCoord);

    // Ambient term (a simple constant ambient)
    vec3 ambient = 0.1 * albedoColor.rgb;

    vec3 norm = normalize(fragNormal);
    vec3 viewDir = normalize(viewPos - fragPos);

    // Initialize lighting contributions.
    vec3 diffuse = vec3(0.0);
    vec3 specular = vec3(0.0);

    // Material properties
    float shininess = 32.0;
    float specularStrength = 0.5;

    // Accumulate contributions from all directional lights.
    for (int i = 0; i < numDirectionalLights; i++) {
        // For directional lights, the light direction is constant across the scene.
        // (Assume the uniform 'direction' is already normalized.)
        vec3 lightDir = normalize(-directionalLights[i].direction);

        // Diffuse shading.
        float diff = max(dot(norm, lightDir), 0.0);
        diffuse += directionalLights[i].color * directionalLights[i].strength * diff;

        // Blinn–Phong specular shading.
        vec3 halfDir = normalize(lightDir + viewDir);
        float spec = pow(max(dot(norm, halfDir), 0.0), shininess);
        specular += directionalLights[i].color * directionalLights[i].strength * spec * specularStrength;
    }

    vec3 lighting = ambient + diffuse + specular;
    vec3 result = albedoColor.rgb * lighting;

    outColor = vec4(result, albedoColor.a);
}
