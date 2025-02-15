package engine;

import static engine.Texture.Type;

public class Material {
    public Texture albedo;
    public Texture normal;
    public Texture roughness;
    
    // These are our “scalar” parameters.
    public float specular = 0.5f; // non-metallic F₀ (typically ~0.04 for dielectrics)
    public float metallic = 0.0f; // 0 = dielectric, 1 = metal
    
    // Default empty material – using default textures.
    public static Material empty = new Material(
            new Texture(Type.ALBEDO),
            new Texture(Type.NORMAL),
            new Texture(Type.ROUGHNESS)
    );
    
    public Material() {
        this.albedo = empty.albedo;
        this.normal = empty.normal;
        this.roughness = empty.roughness;
    }
    
    public Material(Texture albedo) {
        this(albedo, empty.normal, empty.roughness);
    }
    
    public Material(Texture albedo, Texture normal) {
        this(albedo, normal, empty.roughness);
    }
    
    public Material(Texture albedo, Texture normal, Texture roughness) {
        this.albedo = albedo;
        this.normal = normal;
        this.roughness = roughness;
    }
    
    @Override
    public String toString() {
        return "Material{" +
               "albedo=" + albedo +
               ", normal=" + normal +
               ", roughness=" + roughness +
               ", specular=" + specular +
               ", metallic=" + metallic +
               '}';
    }
}
