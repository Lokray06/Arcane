#version 330 core

in vec3 fragNormal;
in vec2 fragTexCoord;
// fragPos is the world‐space position of the fragment.
in vec3 fragPos;

out vec4 outColor;

uniform sampler2D uAlbedo;
uniform vec3 viewPos;

struct DirectionalLight {
    vec3 direction;
    vec3 color;
    float strength;
};

struct SkyboxAmbient {
    samplerCube cubemap;
    float strength;
};
uniform SkyboxAmbient skyboxAmbient;

#define MAX_DIR_LIGHTS 10
uniform int numDirectionalLights;
uniform DirectionalLight directionalLights[MAX_DIR_LIGHTS];

// --- Cascaded Shadow Mapping Uniforms ---
#define NUM_CASCADES 4

// Each cascade has its own shadow map.
uniform sampler2D shadowMaps[NUM_CASCADES];
// And its own light-space transform matrix.
uniform mat4 lightSpaceMatrices[NUM_CASCADES];
// The cascade split distances (in view-space units, for example).
uniform float cascadeSplits[NUM_CASCADES];

float biasMultiplier = 0.00001;
float strengthMultiplier = 0.0001;

// 3x3 Percentage Closer Filtering (PCF) shadow calculation.
float ShadowCalculation(vec4 fragPosLightSpace, sampler2D shadowMap)
{
    // Transform to normalized device coordinates.
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    // Transform from NDC [-1,1] to [0,1].
    projCoords = projCoords * 0.5 + 0.5;

    // If outside the light's frustum, return no shadow.
    if(projCoords.z > 1.0)
    return 1.0;

    float currentDepth = projCoords.z;
    float bias = max(biasMultiplier * (1.0 - dot(normalize(fragNormal), normalize(-directionalLights[0].direction))),
                     biasMultiplier);

    float shadow = 0.0;
    vec2 texelSize = 1.0 / vec2(textureSize(shadowMap, 0));
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            float closestDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
            shadow += currentDepth - bias > closestDepth ? 0.0 : 1.0;
        }
    }
    shadow /= 9.0;
    return shadow;
}

vec3 GetSkyboxAmbient(vec3 normal) {
    // Calculate the reflection direction based on normal and view direction
    vec3 viewDir = normalize(viewPos - fragPos); // Direction from fragment to camera
    vec3 reflectDir = reflect(-viewDir, normalize(normal));   // Reflection direction
    vec3 ambientColor = texture(skyboxAmbient.cubemap, reflectDir).rgb;
    return ambientColor * skyboxAmbient.strength;
}

void main()
{
    vec4 albedoColor = texture(uAlbedo, fragTexCoord);
    vec3 ambient = GetSkyboxAmbient(fragNormal);

    vec3 norm = normalize(fragNormal);
    vec3 viewDir = normalize(viewPos - fragPos);

    vec3 diffuse = vec3(0.0);
    vec3 specular = vec3(0.0);

    float shininess = 32.0;
    float specularStrength = 0.5;

    // Accumulate lighting from all directional lights.
    for (int i = 0; i < numDirectionalLights; i++) {
        vec3 lightDir = normalize(-directionalLights[i].direction);
        float diff = max(dot(norm, lightDir), 0.0);
        diffuse += directionalLights[i].color * directionalLights[i].strength * strengthMultiplier * diff;
        vec3 halfDir = normalize(lightDir + viewDir);
        float spec = pow(max(dot(norm, halfDir), 0.0), shininess);
        specular += directionalLights[i].color * directionalLights[i].strength * strengthMultiplier * spec * specularStrength;
    }

    // --- Cascade Selection ---
    // Here we simply choose the cascade based on the view-space depth.
    float viewDepth = length(fragPos - viewPos);
    int cascadeIndex = NUM_CASCADES - 1;
    for (int i = 0; i < NUM_CASCADES; i++) {
        if (viewDepth < cascadeSplits[i]) {
            cascadeIndex = i;
            break;
        }
    }

    // Compute light-space position for the chosen cascade.
    vec4 fragPosLS = lightSpaceMatrices[cascadeIndex] * vec4(fragPos, 1.0);
    float shadow = ShadowCalculation(fragPosLS, shadowMaps[cascadeIndex]);

    vec3 lighting = ambient + (diffuse + specular) * shadow;
    vec3 result = albedoColor.rgb * lighting;
    outColor = vec4(result, albedoColor.a);
}