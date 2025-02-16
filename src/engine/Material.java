package engine;

import org.joml.Vector3f;

import static engine.Texture.Type;

public class Material {
    public Texture albedoMap; // Albedo texture
    public Vector3f albedoColor = new Vector3f(1); //Multiplies the albedo map / texture
    
    public Texture normalMap; // Normal texture
    public float normalMapStrength; // Scalar multiplier for the normal map strength
    
    public Texture metallicMap; // Metallic texture (Where is and where isn't metallic)
    public float metallic = 1f; // Scalar multiplier for the metallic map strength
    
    public Texture roughnessMap; // Roughness texture (Where is and where isn't shiny)
    public float roughness = 1; // Scalar multiplier fot the roughness texture
    
    public Texture aoMap; // AO texture
    
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
