#version 460

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inNormal;
layout(location = 2) in vec3 inColor;
layout(location = 3) in vec3 inTexCoord;
layout(location = 4) in vec3 inTangent;
layout(location = 5) in vec3 inBitangent;

out vec3 vNormal;
out vec3 vWorldPosition;
out vec3 vViewSpacePosition;
out vec3 vTexCoord;
out vec3 vTangent;
out vec3 vBitangent;
out vec3 vColor;

#define saturate(x) clamp(x, 0.0, 1.0)
#define PI 3.1415926535897932384626433832795
#define INV_PI 1.0 / PI
#define RCP_4PI 1.0 / (4 * PI)
#define DEFAULT_GAMMA 2.2

const int MAX_POINT_LIGHTS = 4;

const int NUM_CASCADES = 2;
#define TWO_CASCADES

struct DirectionalLight
{
    vec3 color;
    vec3 direction;
    float amount;
};

struct PointLight
{
    vec3 color;
    vec3 position;
    float radius;
};

layout(binding = 0, std140) uniform PerScene
{
    DirectionalLight u_directionalLight;
    PointLight u_pointLights[MAX_POINT_LIGHTS];
    float uTime;
    int uActivePointLights;
    int sunlightActive;
    vec2 resolution;
    vec2 invResolution;
    vec4 uCascadesPlane[NUM_CASCADES];
    mat4 uCascadesMatrix[NUM_CASCADES];
    float uCascadesNear[NUM_CASCADES];
    float uCascadesFar[NUM_CASCADES];
};

layout(binding = 1, std140) uniform PerView
{
    mat4 uViewMatrix;
    mat4 uViewProjMatrix;
    vec4 uEyePos;
};

layout(binding = 2, std140) uniform PerObject
{
    mat4 uModelMatrix;
    mat4 uModelMatrixIT;
    mat4 uModelViewMatrix;
    float uReceiveShadow;
};

vec2 get_shadow_offsets(vec3 N, vec3 L)
{
    float cosAlpha = clamp(dot(N, L), 0.0, 1.0);
    float offsetScaleN = sqrt(1 - cosAlpha * cosAlpha); // sin(acos(L·N))
    float offsetScaleL = offsetScaleN / cosAlpha;    // tan(acos(L·N))
    return vec2(offsetScaleN, min(2, offsetScaleL));
}

// Offsets a position based on slope and normal
vec3 get_biased_position(vec3 pos, float slope_bias, float normal_bias, vec3 normal, vec3 light)
{
    vec2 offsets = get_shadow_offsets(normal, light);
    pos += normal * offsets.x * normal_bias;
    pos += light  * offsets.y * slope_bias;
    return pos;
}

// https://imdoingitwrong.wordpress.com/2011/01/31/light-attenuation/
// https://imdoingitwrong.wordpress.com/2011/02/10/improved-light-attenuation/
float point_light_attenuation(float radius, float intensity, float cutoff, float dist)
{
    float d = max(dist - radius, 0.0);
    float denom = d / radius + 1.0;
    float attenuation = 0.0;
    attenuation = intensity / (denom * denom);
    attenuation = (attenuation - cutoff) / (1.0 - cutoff);
    attenuation = max(attenuation, 0.0);
    return attenuation;
}

// http://blog.selfshadow.com/publications/blending-in-detail/
vec3 blend_normals_unity(vec3 geometric, vec3 detail)
{
    vec3 n1 = geometric;
    vec3 n2 = detail;
    mat3 nBasis = mat3(
    vec3(n1.z, n1.y, -n1.x),
    vec3(n1.x, n1.z, -n1.y),
    vec3(n1.x, n1.y,  n1.z));
    return normalize(n2.x*nBasis[0] + n2.y*nBasis[1] + n2.z*nBasis[2]);
}

// http://the-witness.net/news/2012/02/seamless-cube-map-filtering/
vec3 fix_cube_lookup(vec3 v, float cubeSize, float lod)
{
    float M = max(max(abs(v.x), abs(v.y)), abs(v.z));
    float scale = 1 - exp2(lod) / cubeSize;
    if (abs(v.x) != M) v.x *= scale;
    if (abs(v.y) != M) v.y *= scale;
    if (abs(v.z) != M) v.z *= scale;
    return v;
}

// https://www.unrealengine.com/blog/physically-based-shading-on-mobile
// todo - this is typically precomputed using Monte-Carlo and stored as a 2d LUT.
vec3 env_brdf_approx(vec3 specularColor, float roughness, float NoV)
{
    const vec4 c0 = vec4(-1, -0.0275, -0.572, 0.022);
    const vec4 c1 = vec4(1, 0.0425, 1.04, -0.04);
    vec4 r = roughness * c0 + c1;
    float a004 = min(r.x * r.x, exp2(-9.28 * NoV)) * r.x + r.y;
    vec2 AB = vec2(-1.04, 1.04) * a004 + r.zw;
    return specularColor * AB.x + AB.y;
}

vec3 blend_rnm(vec3 n1, vec3 n2)
{
    vec3 t = n1.xyz*vec3( 2,  2, 2) + vec3(-1, -1,  0);
    vec3 u = n2.xyz*vec3(-2, -2, 2) + vec3( 1,  1, -1);
    vec3 r = t*dot(t, u) - u*t.z;
    return normalize(r);
}

// todo - can compute TBN in vertex shader
vec4 calcNormalMap(vec3 normal, vec3 tangent, vec3 bitangent, vec3 sampledMap)
{
    const mat3 TBN = mat3(tangent, bitangent, normal);
    return vec4(TBN * sampledMap, 1.0);
}

// Converts a Beckmann roughness parameter to a Phong specular power
float roughnessToSpecularPower(in float m)
{
    float m2 = m * m;
    return 2.0 / (m * m) - 2.0;
}

// Converts a Blinn-Phong specular power to a Beckmann roughness parameter
float specularPowerToRoughness(in float s)
{
    return sqrt(2.0 / (s + 2.0));
}

// specular aliasing - based on http://blog.selfshadow.com/2011/07/22/specular-showdown/
// far future todo - can be computed as part of asset compilation pipeline
// http://www.selfshadow.com/talks/rock_solid_shading_v1.pdf
// http://selfshadow.com/sandbox/gloss.html
float geometricAaToksvig(in float roughness, in vec3 normalWS, in float factor)
{
    roughness = max(roughness, 0.05);
    const float normalMapLen = length(normalWS);
    float s = roughnessToSpecularPower(roughness);
    float ft = normalMapLen / mix(s, 1.0, normalMapLen);
    ft = max(ft, 0.01);
    return specularPowerToRoughness(ft * s) * factor + roughness * (1 - factor);
}

uniform vec2 uTexCoordScale = vec2(1,1);

void main()
{
    vec4 worldPosition = uModelMatrix * vec4(inPosition, 1);
    gl_Position = uViewProjMatrix * worldPosition;
    vViewSpacePosition = (uModelMatrix * vec4(inPosition, 1)).xyz;
    vNormal = normalize((uModelMatrix * vec4(inNormal, 0)).xyz);
    vWorldPosition = worldPosition.xyz;
    //vTexCoord = inTexCoord * uTexCoordScale;
    vTangent = (uModelMatrix * vec4(inTangent, 0)).xyz;
    vBitangent = (uModelMatrix * vec4(inBitangent, 0)).xyz;
    vColor = inColor;
}