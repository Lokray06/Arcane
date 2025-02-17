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

// ----- Point Lights -----
#define MAX_POINT_LIGHTS 10
struct PointLight {
    vec3 position;
    vec3 color;
    float strength;
    float constant;
    float linear;
    float quadratic;
};
uniform int numPointLights;
uniform PointLight pointLights[MAX_POINT_LIGHTS];

// ----- Shadow Map -----
uniform sampler2D shadowMap;

const float PI = 3.14159265359;
uniform float uLightStrength = 0.01f;

// ----- Shadow Calculation Function -----
float calculateShadow(vec4 fragPosLightSpace, vec3 normal, vec3 lightDir)
{
    // Perform perspective divide.
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    // Transform to [0,1] range.
    projCoords = projCoords * 0.5 + 0.5;

    // If outside the shadow map, return no shadow.
    if (projCoords.z > 1.0)
    return 0.0;

    // Obtain closest depth from shadow map.
    float closestDepth = texture(shadowMap, projCoords.xy).r;
    // Current fragment depth from light’s perspective.
    float currentDepth = projCoords.z;
    // Bias to prevent shadow acne.
    float bias = max(0.005 * (1.0 - dot(normal, lightDir)), 0.001);

    // PCF (percentage-closer filtering) for smoother shadows.
    float shadow = 0.0;
    vec2 texelSize = 1.0 / textureSize(shadowMap, 0);
    for (int x = -1; x <= 1; ++x)
    {
        for (int y = -1; y <= 1; ++y)
        {
            float pcfDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
            shadow += currentDepth - bias > pcfDepth ? 1.0 : 0.0;
        }
    }
    shadow /= 9.0;
    return shadow;
}

// ----- PBR Helper Functions -----
float DistributionGGX(vec3 N, vec3 H, float roughness)
{
    float a2 = roughness * roughness * roughness * roughness;
    float NdotH = max(dot(N, H), 0.0);
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
    return GeometrySchlickGGX(max(dot(N, L), 0.0), roughness) *
    GeometrySchlickGGX(max(dot(N, V), 0.0), roughness);
}

vec3 FresnelSchlick(float cosTheta, vec3 F0)
{
    return F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);
}

void main()
{
    // --- Sample Material Textures ---
    vec3 albedo = pow(texture(uAlbedo, fragTexCoord).rgb * uAlbedoColor, vec3(2.2));
    float texMetallic = texture(uMetallic, fragTexCoord).r;
    float metallic = clamp(texMetallic * uMetallicScalar, 0.0, 1.0);
    float texRoughness = texture(uRoughness, fragTexCoord).r;
    float roughness = clamp(texRoughness * uRoughnessScalar, 0.0, 1.0);
    roughness = max(roughness, 0.04);
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
    vec3 F0 = mix(vec3(0.04), albedo, metallic);

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
            vec3 radiance = directionalLights[i].color * directionalLights[i].strength * uLightStrength;

            // --- Shadow Calculation ---
            float shadow = calculateShadow(fragPosLightSpace, N, L);
            // Reduce light contribution if in shadow.
            Lo += (diffuse + specular) * radiance * NdotL * (1.0 - shadow);
        }
    }

    // --- Accumulate Point Light Contributions ---
    for (int i = 0; i < numPointLights; ++i)
    {
        vec3 L = normalize(pointLights[i].position - fragPos);
        float distance = length(pointLights[i].position - fragPos);
        // Calculate attenuation.
        float attenuation = 1.0 / (pointLights[i].constant + pointLights[i].linear * distance +
        pointLights[i].quadratic * (distance * distance));
        float NdotL = max(dot(N, L), 0.0);
        if (NdotL > 0.0)
        {
            vec3 H = normalize(V + L);
            float D = DistributionGGX(N, H, roughness);
            float G = GeometrySmith(N, V, L, roughness);
            vec3 F = FresnelSchlick(max(dot(H, V), 0.0), F0);
            vec3 numerator = D * G * F;
            float denominator = 4.0 * max(dot(N, V), 0.0) * NdotL + 0.001;
            vec3 specular = numerator / denominator;
            vec3 kS = F;
            vec3 kD = (vec3(1.0) - kS) * (1.0 - metallic);
            vec3 diffuse = kD * albedo / PI;
            vec3 radiance = pointLights[i].color * pointLights[i].strength * uLightStrength * attenuation;
            Lo += (diffuse + specular) * radiance * NdotL;
        }
    }

    // --- Ambient Lighting ---
    vec3 ambient = vec3(0.03) * albedo * ao;
    vec3 color = ambient + Lo;
    color = pow(color, vec3(1.0 / 2.2));

    outColor = vec4(color, 1.0);
}
