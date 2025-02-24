#version 330 core

in vec3 FragPos;
in vec2 TexCoords;
in vec4 FragPosLightSpace;
in vec3 vNormal;
// vTangent and vBitangent are no longer used since we rebuild TBN in the fragment shader

out vec4 FragColor;

uniform vec3 viewPos;
uniform float lightStrength = 0.01;

// Material maps and parameters
uniform sampler2D uAlbedo;
uniform sampler2D uNormal;
uniform sampler2D uMetallic;
uniform sampler2D uRoughness;
uniform sampler2D uAO;

uniform vec3 uAlbedoColor;
uniform float uMetallicScalar;
uniform float uRoughnessScalar;
// Use a value in [0, 1] for normal map influence (1.0 = full effect)
uniform float uNormalMapStrength;

// IBL Environment Maps
uniform samplerCube irradianceMap;  // Diffuse IBL
uniform samplerCube prefilterMap;   // Specular IBL
uniform sampler2D brdfLUT;          // BRDF LUT for Fresnel-Schlick

// Directional lights
#define MAX_DIR_LIGHTS 10
struct DirectionalLight {
    vec3 direction;
    vec3 color;
    float strength;
};
uniform int numDirectionalLights;
uniform DirectionalLight directionalLights[MAX_DIR_LIGHTS];

// Shadow map for directional light
uniform sampler2D shadowMap;

const float PI = 3.14159265359;

// ----- Directional Shadow Calculation (PCF) -----
float calculateShadow(vec4 fragPosLightSpace, vec3 normal, vec3 lightDir)
{
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;
    if (projCoords.z > 1.0)
    return 0.0;
    float closestDepth = texture(shadowMap, projCoords.xy).r;
    float currentDepth = projCoords.z;
    float bias = max(0.005 * (1.0 - dot(normal, lightDir)), 0.001);
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

// --- PBR helper functions ---

float DistributionGGX(vec3 N, vec3 H, float roughness)
{
    float a2 = roughness * roughness * roughness * roughness;
    float NdotH = max(dot(N, H), 0.0);
    float denom = (NdotH * NdotH * (a2 - 1.0) + 1.0);
    return a2 / (PI * denom * denom);
}

float GeometrySchlickGGX(float NdotV, float roughness)
{
    float r = roughness + 1.0;
    float k = (r * r) / 8.0;
    return NdotV / (NdotV * (1.0 - k) + k);
}

float GeometrySmith(vec3 N, vec3 V, vec3 L, float roughness)
{
    float NdotL = max(dot(N, L), 0.0);
    float NdotV = max(dot(N, V), 0.0);
    return GeometrySchlickGGX(NdotV, roughness) * GeometrySchlickGGX(NdotL, roughness);
}

vec3 FresnelSchlick(float cosTheta, vec3 F0)
{
    return F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);
}

// --- New Normal Mapping Implementation ---
// This function rebuilds the TBN matrix using screen-space derivatives.
vec3 getNormalFromMap()
{
    // Sample the tangent-space normal and remap from [0,1] to [-1,1]
    vec3 tangentNormal = texture(uNormal, TexCoords).rgb;
    tangentNormal = tangentNormal * 2.0 - 1.0;

    // Compute partial derivatives of the fragment position and texture coordinates.
    vec3 Q1 = dFdx(FragPos);
    vec3 Q2 = dFdy(FragPos);
    vec2 st1 = dFdx(TexCoords);
    vec2 st2 = dFdy(TexCoords);

    // Use the interpolated normal from the vertex shader.
    vec3 N = normalize(vNormal);
    // Compute tangent vector.
    vec3 T = normalize(Q1 * st2.t - Q2 * st1.t);
    // Compute bitangent with a negative sign to match the tutorial’s handedness.
    vec3 B = -normalize(cross(N, T));
    // Construct the TBN matrix.
    mat3 TBN = mat3(T, B, N);

    // Transform the sampled normal to world space.
    return normalize(TBN * tangentNormal);
}

void main()
{
    // --- Material ---
    vec3 albedo = texture(uAlbedo, TexCoords).rgb * uAlbedoColor;
    float metallic = texture(uMetallic, TexCoords).r * uMetallicScalar;
    float roughness = texture(uRoughness, TexCoords).r * uRoughnessScalar;
    float ao = texture(uAO, TexCoords).r;
    roughness = clamp(roughness, 0.0, 1.0);

    // --- Normal Mapping ---
    // Blend between the vertex normal and the derivative-based mapped normal.
    vec3 normalFromMap = getNormalFromMap();
    vec3 N = normalize(mix(vNormal, normalFromMap, uNormalMapStrength));

    // --- View Direction ---
    vec3 V = normalize(viewPos - FragPos);
    vec3 F0 = mix(vec3(0.04), albedo, metallic);

    vec3 Lo = vec3(0.0);

    // IBL: Diffuse Indirect Lighting
    vec3 irradiance = texture(irradianceMap, N).rgb;
    vec3 diffuseIBL = irradiance * albedo;

    // IBL: Specular Indirect Lighting
    vec3 R = reflect(-V, N);
    float roughnessLevel = roughness * 4.0;
    vec3 prefilteredColor = textureLod(prefilterMap, R, roughnessLevel).rgb;
    vec2 brdf = texture(brdfLUT, vec2(max(dot(N, V), 0.0), roughness)).rg;

    vec3 specularIBL = prefilteredColor * (F0 * brdf.x + brdf.y);

    // --- Directional Lights ---
    for (int i = 0; i < numDirectionalLights; ++i)
    {
        vec3 L = normalize(-directionalLights[i].direction);
        vec3 H = normalize(V + L);

        float NdotL = max(dot(N, L), 0.0);
        float D = DistributionGGX(N, H, roughness);
        float G = GeometrySmith(N, V, L, roughness);
        vec3 F = FresnelSchlick(max(dot(H, V), 0.0), F0);

        vec3 numerator = D * G * F;
        float denominator = 4.0 * max(dot(N, V), 0.0) * NdotL + 0.001;
        vec3 specular = numerator / denominator;

        vec3 kS = F;
        vec3 kD = (vec3(1.0) - kS) * (1.0 - metallic);
        vec3 diffuse = kD * albedo / PI;
        vec3 radiance = directionalLights[i].color * directionalLights[i].strength * lightStrength;

        float shadow = calculateShadow(FragPosLightSpace, N, L);
        Lo += (diffuse + specular) * radiance * NdotL * (1.0 - shadow);
    }

    vec3 ambient = (diffuseIBL + specularIBL) * ao;
    vec3 color = Lo + ambient;

    // HDR tonemapping and gamma correction
    color = color / (color + vec3(1.0));
    color = pow(color, vec3(1.0 / 2.2));

    FragColor = vec4(color, 1.0);

}
