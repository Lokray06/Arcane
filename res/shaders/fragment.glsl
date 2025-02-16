#version 330 core

// Inputs from the vertex shader.
in vec3 fragPos;
in vec2 fragTexCoord;
in vec3 vNormal;
in vec3 vTangent;
in vec3 vBitangent;
in vec4 fragPosLightSpace; // Light-space coordinate

// Output color.
out vec4 outColor;

// Camera position.
uniform vec3 viewPos;

// ----- Material Maps -----
uniform sampler2D uAlbedo;
uniform sampler2D uNormal;
uniform sampler2D uMetallic;
uniform sampler2D uRoughness;
uniform sampler2D uAO;

// Additional material properties.
uniform vec3 uAlbedoColor;
uniform float uMetallicScalar;
uniform float uRoughnessScalar;
uniform float uNormalMapStrength;

// ----- Skybox Ambient -----
struct SkyboxAmbient {
    samplerCube cubemap;
    float strength;
};
uniform SkyboxAmbient skyboxAmbient;

// ----- Directional Lights -----
#define MAX_DIR_LIGHTS 10
struct DirectionalLight {
    vec3 direction;
    vec3 color;
    float strength;
};
uniform int numDirectionalLights;
uniform DirectionalLight directionalLights[MAX_DIR_LIGHTS];

// ----- Cascaded Shadow Mapping Uniforms -----
#define NUM_CASCADES 4
uniform sampler2D shadowMaps[NUM_CASCADES];
uniform mat4 lightSpaceMatrices[NUM_CASCADES];
uniform float cascadeSplits[NUM_CASCADES];

// Constants and multipliers.
const float PI = 3.14159265359;
float biasMultiplier = 0.00001;
float strengthMultiplier = 1;

// ----- Function: Calculate Skybox Ambient Lighting -----
vec3 GetSkyboxAmbient(vec3 normal) {
    vec3 viewDir = normalize(viewPos - fragPos);
    vec3 reflectDir = reflect(-viewDir, normal);
    vec3 ambientColor = texture(skyboxAmbient.cubemap, reflectDir).rgb;
    return ambientColor * skyboxAmbient.strength;
}

// ----- Function: Shadow Calculation (PCF) -----
// Now uses the perturbed normal (N) for the bias.
float ShadowCalculation(vec4 fragPosLS, sampler2D shadowMap, vec3 N)
{
    vec3 projCoords = fragPosLS.xyz / fragPosLS.w;
    projCoords = projCoords * 0.5 + 0.5;
    if (projCoords.z > 1.0)
    return 1.0;

    float currentDepth = projCoords.z;
    float bias = max(biasMultiplier * (1.0 - dot(normalize(N), normalize(-directionalLights[0].direction))),
                     biasMultiplier);

    float shadow = 0.0;
    vec2 texelSize = 1.0 / vec2(textureSize(shadowMap, 0));
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            float pcfDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
            shadow += currentDepth - bias > pcfDepth ? 0.0 : 1.0;
        }
    }
    shadow /= 9.0;
    return shadow;
}

// ----- PBR Helper Functions -----
float DistributionGGX(vec3 N, vec3 H, float roughness)
{
    float a      = roughness * roughness;
    float a2     = a * a;
    float NdotH  = max(dot(N, H), 0.0);
    float NdotH2 = NdotH * NdotH;

    float nom   = a2;
    float denom = (NdotH2 * (a2 - 1.0) + 1.0);
    denom = PI * denom * denom;

    return nom / max(denom, 0.001);
}

float GeometrySchlickGGX(float NdotV, float roughness)
{
    float r = roughness + 1.0;
    float k = (r * r) / 8.0;
    return NdotV / (NdotV * (1.0 - k) + k);
}

float GeometrySmith(vec3 N, vec3 V, vec3 L, float roughness)
{
    float NdotV = max(dot(N, V), 0.0);
    float NdotL = max(dot(N, L), 0.0);
    float ggx1 = GeometrySchlickGGX(NdotV, roughness);
    float ggx2 = GeometrySchlickGGX(NdotL, roughness);
    return ggx1 * ggx2;
}

vec3 FresnelSchlick(float cosTheta, vec3 F0)
{
    return F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);
}

void main()
{
    // --- Sample Material Textures ---
    vec3 albedo = pow(texture(uAlbedo, fragTexCoord).rgb * uAlbedoColor, vec3(2.2));

    // Sample metallic and roughness, multiply by the scalars, and clamp to [0,1].
    float texMetallic = texture(uMetallic, fragTexCoord).r;
    float metallic = clamp(texMetallic * uMetallicScalar, 0.0, 1.0);

    float texRoughness = texture(uRoughness, fragTexCoord).r;
    float roughness = clamp(texRoughness * uRoughnessScalar, 0.0, 1.0);

    float ao = texture(uAO, fragTexCoord).r;

    // --- Normal Mapping ---
    vec3 normalWorld = normalize(vNormal);
    vec3 tangentNormal = texture(uNormal, fragTexCoord).rgb;
    tangentNormal = tangentNormal * 2.0 - 1.0;
    mat3 TBN = mat3(normalize(vTangent), normalize(vBitangent), normalWorld);
    vec3 mappedNormal = normalize(TBN * tangentNormal);
    vec3 N = normalize(mix(normalWorld, mappedNormal, uNormalMapStrength));

    // --- View Direction ---
    vec3 V = normalize(viewPos - fragPos);

    // --- Compute Reflectance at Normal Incidence ---
    vec3 F0 = vec3(0.04);
    F0 = mix(F0, albedo, metallic);

    // --- Accumulate Directional Light Contributions ---
    vec3 Lo = vec3(0.0);
    for (int i = 0; i < numDirectionalLights; ++i)
    {
        vec3 L = normalize(-directionalLights[i].direction);
        vec3 H = normalize(V + L);
        float NdotL = max(dot(N, L), 0.0);
        float NdotV = max(dot(N, V), 0.0);

        if (NdotL > 0.0)
        {
            float D = DistributionGGX(N, H, roughness);
            float G = GeometrySmith(N, V, L, roughness);
            vec3 F = FresnelSchlick(max(dot(H, V), 0.0), F0);

            vec3 numerator = D * G * F;
            float denominator = 4.0 * NdotV * NdotL + 0.001;
            vec3 specular = numerator / denominator;

            vec3 kS = F;
            vec3 kD = (vec3(1.0) - kS) * (1.0 - metallic);
            vec3 diffuse = kD * albedo / PI;
            vec3 radiance = directionalLights[i].color * directionalLights[i].strength * strengthMultiplier;

            Lo += (diffuse + specular) * radiance * NdotL;
        }
    }

    // --- Cascaded Shadow Mapping ---
    float viewDepth = length(fragPos - viewPos);
    int cascadeIndex = NUM_CASCADES - 1;
    for (int i = 0; i < NUM_CASCADES; i++) {
        if (viewDepth < cascadeSplits[i]) {
            cascadeIndex = i;
            break;
        }
    }
    vec4 fragPosLS_current = lightSpaceMatrices[cascadeIndex] * vec4(fragPos, 1.0);
    float shadow = ShadowCalculation(fragPosLS_current, shadowMaps[cascadeIndex], N);

    // --- Ambient Lighting ---
    vec3 ambient = GetSkyboxAmbient(N);
    vec3 color = (ambient * albedo * ao) + (Lo * shadow);
    color = pow(color, vec3(1.0 / 2.2));

    outColor = vec4(color, 1.0);
}
