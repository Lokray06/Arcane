package engine;

import static engine.Texture.Type;

public class Material {
    public Texture albedoMap;
    public Texture normalMap;
    public Texture metallicMap;
    public Texture roughnessMap;
    public Texture aoMap;
    
    // These are our “scalar” parameters.
    public float roughness = 1f; // non-metallic F₀ (typically ~0.04 for dielectrics)
    public float metallic = 0f; // 0 = dielectric, 1 = metal
    
    // Default empty material – using default textures.
    public static Material empty = new Material(
            new Texture(Type.ALBEDO),
            new Texture(Type.NORMAL),
            new Texture(Type.METALLIC),
            new Texture(Type.ROUGHNESS),
            new Texture(Type.AO)
    );
    
    public Material() {
        this.albedoMap = empty.albedoMap;
        this.normalMap = empty.normalMap;
        this.metallicMap = empty.metallicMap;
        this.roughnessMap = empty.roughnessMap;
        this.aoMap = empty.aoMap;
    }
    
    public Material(Texture albedoMap) {
        this(albedoMap, empty.normalMap, empty.metallicMap, empty.roughnessMap, empty.aoMap);
    }
    
    public Material(Texture albedoMap, Texture normalMap) {
        this(albedoMap, normalMap, empty.metallicMap, empty.roughnessMap, empty.aoMap);
    }
    
    public Material(Texture albedoMap, Texture normalMap, Texture metallicMap) {
        this(albedoMap, normalMap, metallicMap, empty.roughnessMap, empty.aoMap);
    }
    
    public Material(Texture albedoMap, Texture normalMap, Texture metallicMap, Texture roughnessMap) {
        this(albedoMap, normalMap, metallicMap, roughnessMap, empty.aoMap);
    }
    
    public Material(Texture albedoMap, Texture normalMap, Texture metallicMap, Texture roughnessMap, Texture aoMap) {
        this.albedoMap = albedoMap;
        this.normalMap = normalMap;
        this.metallicMap = metallicMap;
        this.roughnessMap = roughnessMap;
        this.aoMap = aoMap;
    }
    
    @Override
    public String toString() {
        return "Material{" +
               "albedoMap=" + albedoMap +
               ", normalMap=" + normalMap +
               ", metallicMap=" + metallicMap +
               ", roughnessMap=" + roughnessMap +
               ", aoMap=" + aoMap +
               ", specular=" + roughness +
               ", metallic=" + metallic +
               '}';
    }
}
