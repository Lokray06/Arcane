package engine;

import org.joml.Vector3f;

import static engine.Texture.Type;

/**
 * Represents a material used for rendering a mesh.
 * <p>
 * A Material contains texture maps (albedo, normal, metallic, roughness, height, and ambient occlusion)
 * along with scalar parameters such as metallic and roughness factors.
 * Additionally, it now includes scaleX and scaleY for texture tiling.
 * </p>
 */
public class Material {
    public String name = "UnnamedMaterial";
    
    /** The albedo (diffuse) texture map. */
    public Texture albedoMap;
    /** The albedo color multiplier. */
    public Vector3f albedoColor = new Vector3f(1);
    
    /** The normal map texture. */
    public Texture normalMap;
    /** Scalar multiplier for the normal map strength. */
    public float normalMapStrength = 1;
    
    /** The metallic texture map. */
    public Texture metallicMap;
    /** Scalar multiplier for the metallic property. */
    public float metallic;
    
    /** The roughness texture map. */
    public Texture roughnessMap;
    /** Scalar multiplier for the roughness property. */
    public float roughness;
    
    /** The height map texture. */
    public Texture heightMap;
    /** Scalar multiplier for the height displacement property. */
    public float heightScale;
    
    /** The ambient occlusion texture map. */
    public Texture aoMap;
    
    /**
     * Tiling factors for texture coordinates.
     * Set scaleX and scaleY to values greater than 1 to tile textures.
     * For example, a value of 10 will tile the texture 10 times.
     */
    public float scaleX = 1.0f;
    public float scaleY = 1.0f;
    
    /**
     * A default empty material using default textures.
     */
    public static Material empty = new Material(
            new Texture(Type.ALBEDO),
            new Texture(Type.NORMAL),
            new Texture(Type.METALLIC),
            new Texture(Type.ROUGHNESS),
            new Texture(Type.AO),
            new Texture(Type.HEIGHT),
            0,
            1
    );
    
    /**
     * Default constructor that assigns default textures.
     */
    public Material() {
        this.albedoMap = empty.albedoMap;
        this.normalMap = empty.normalMap;
        this.metallicMap = empty.metallicMap;
        this.roughnessMap = empty.roughnessMap;
        this.aoMap = empty.aoMap;
        this.heightMap = empty.heightMap;
    }
    
    /**
     * Constructs a material with a specified albedo texture.
     */
    public Material(Texture albedoMap) {
        this(albedoMap, empty.normalMap, empty.metallicMap, empty.roughnessMap, empty.aoMap, 0, 1);
    }
    
    /**
     * Constructs a material with specified albedo texture, metallic, and roughness values.
     */
    public Material(Texture albedoMap, float metallic, float roughness) {
        this(albedoMap, empty.normalMap, empty.metallicMap, empty.roughnessMap, empty.aoMap, empty.heightMap, metallic, roughness);
    }
    
    /**
     * Constructs a material with specified albedo and normal textures.
     */
    public Material(Texture albedoMap, Texture normalMap) {
        this(albedoMap, normalMap, empty.metallicMap, empty.roughnessMap, empty.aoMap, empty.heightMap, 0, 1);
    }
    
    /**
     * Constructs a material with specified albedo, normal, and metallic textures.
     */
    public Material(Texture albedoMap, Texture normalMap, Texture metallicMap) {
        this(albedoMap, normalMap, metallicMap, empty.roughnessMap, empty.aoMap, empty.heightMap, 0, 1);
    }
    
    /**
     * Constructs a material with specified albedo, normal, metallic, and roughness textures.
     */
    public Material(Texture albedoMap, Texture normalMap, Texture metallicMap, Texture roughnessMap) {
        this(albedoMap, normalMap, metallicMap, roughnessMap, empty.aoMap, empty.heightMap, 0, 1);
    }
    
    /**
     * Constructs a material with specified albedo, normal, metallic, roughness, and AO textures.
     */
    public Material(Texture albedoMap, Texture normalMap, Texture metallicMap, Texture roughnessMap, Texture aoMap) {
        this(albedoMap, normalMap, metallicMap, roughnessMap, aoMap, empty.heightMap, 0, 1);
    }
    
    /**
     * Constructs a material with all texture maps and scalar parameters.
     */
    public Material(Texture albedoMap, Texture normalMap, Texture metallicMap, Texture roughnessMap, Texture aoMap, float metallic, float roughness) {
        this(albedoMap, normalMap, metallicMap, roughnessMap, aoMap, empty.heightMap, metallic, roughness);
    }
    
    /**
     * Constructs a material with all texture maps and scalar parameters.
     */
    public Material(Texture albedoMap, Texture normalMap, Texture metallicMap, Texture roughnessMap, Texture aoMap, Texture heightMap, float metallic, float roughness) {
        this.albedoMap = albedoMap;
        this.normalMap = normalMap;
        this.metallicMap = metallicMap;
        this.roughnessMap = roughnessMap;
        this.aoMap = aoMap;
        this.heightMap = heightMap;
        this.metallic = metallic;
        this.roughness = roughness;
    }
    
    /**
     * Returns a string representation of the material.
     */
    @Override
    public String toString() {
        return "Material{" +
               "albedoMap=" + albedoMap +
               ", normalMap=" + normalMap +
               ", metallicMap=" + metallicMap +
               ", roughnessMap=" + roughnessMap +
               ", heightMap=" + heightMap +
               ", aoMap=" + aoMap +
               ", metallic=" + metallic +
               ", roughness=" + roughness +
               ", scaleX=" + scaleX +
               ", scaleY=" + scaleY +
               '}';
    }
}
