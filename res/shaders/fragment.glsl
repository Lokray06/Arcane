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
float strengthMultiplier = 0.01;

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

// Use roughness directly (clamped to a minimum) so that specular grows when smooth.
float DistributionGGX(vec3 N, vec3 H, float roughness)
{
    float a2    = roughness * roughness * roughness * roughness;
    float NdotH = max (dot (N, H), 0.0);
    float denom = (NdotH * NdotH * (a2 - 1.0) + 1.0);
    return a2 / (PI * denom * denom);
}

// Optionally, also clamp roughness here for consistency.
float GeometrySchlickGGX(float NdotV, float roughness)
{
    float r = (roughness + 1.0);
    float k = (r * r) / 8.0;
    return NdotV / (NdotV * (1.0 - k) + k);
}

float GeometrySmith(vec3 N, vec3 V, vec3 L, float roughness)
{
    return GeometrySchlickGGX (max (dot (N, L), 0.0), roughness) *
    GeometrySchlickGGX (max (dot (N, V), 0.0), roughness);
}

vec3 FresnelSchlick(float cosTheta, vec3 F0)
{
    return F0 + (1.0 - F0) * pow (1.0 - cosTheta, 5.0);
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
    // Ensure roughness never goes below 0.04:
    roughness = max(roughness, 0.04);

    float ao = texture(uAO, fragTexCoord).r;

    // --- Normal Mapping ---
    // Get the interpolated world-space normal.
    vec3 normalWorld = normalize(vNormal);

    // Sample the normal from the normal map and remap from [0,1] to [-1,1].
    vec3 tangentNormal = texture(uNormal, fragTexCoord).rgb;
    tangentNormal = tangentNormal * 2.0 - 1.0;
    // Uncomment the next line if your normal map is using an opposite Y convention.
    // tangentNormal.y = -tangentNormal.y;

    // Re-orthogonalize the tangent relative to the normal.
    vec3 T = normalize(vTangent - dot(vTangent, normalWorld) * normalWorld);
    // Recompute the bitangent so that T, B, N form an orthonormal basis.
    vec3 B = normalize(cross(normalWorld, T));

    // Construct the TBN matrix with T, B, and N as columns.
    mat3 TBN = mat3(T, B, normalWorld);

    // Transform the normal from tangent space to world space.
    vec3 mappedNormal = normalize(TBN * tangentNormal);

    // Mix between the original geometry normal and the mapped normal based on strength.
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
