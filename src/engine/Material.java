package engine;

import org.joml.Vector3f;

import static engine.Texture.Type;

/**
 * Represents a material used for rendering a mesh.
 * <p>
 * A Material contains texture maps (albedo, normal, metallic, roughness, and ambient occlusion)
 * along with scalar parameters such as metallic and roughness factors.
 * </p>
 */
public class Material {
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
    
    /** The ambient occlusion texture map. */
    public Texture aoMap;
    
    /**
     * A default empty material using default textures.
     */
    public static Material empty = new Material(
            new Texture(Type.ALBEDO),
            new Texture(Type.NORMAL),
            new Texture(Type.METALLIC),
            new Texture(Type.ROUGHNESS),
            new Texture(Type.AO),
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
    }
    
    /**
     * Constructs a material with a specified albedo texture.
     *
     * @param albedoMap the albedo texture map.
     */
    public Material(Texture albedoMap) {
        this(albedoMap, empty.normalMap, empty.metallicMap, empty.roughnessMap, empty.aoMap, 0, 1);
    }
    
    /**
     * Constructs a material with specified albedo texture, metallic, and roughness values.
     *
     * @param albedoMap the albedo texture map.
     * @param metallic  the metallic factor.
     * @param roughness the roughness factor.
     */
    public Material(Texture albedoMap, float metallic, float roughness) {
        this(albedoMap, empty.normalMap, empty.metallicMap, empty.roughnessMap, empty.aoMap, metallic, roughness);
    }
    
    /**
     * Constructs a material with specified albedo and normal textures.
     *
     * @param albedoMap the albedo texture map.
     * @param normalMap the normal texture map.
     */
    public Material(Texture albedoMap, Texture normalMap) {
        this(albedoMap, normalMap, empty.metallicMap, empty.roughnessMap, empty.aoMap, 0, 1);
    }
    
    /**
     * Constructs a material with specified albedo, normal, and metallic textures.
     *
     * @param albedoMap   the albedo texture map.
     * @param normalMap   the normal texture map.
     * @param metallicMap the metallic texture map.
     */
    public Material(Texture albedoMap, Texture normalMap, Texture metallicMap) {
        this(albedoMap, normalMap, metallicMap, empty.roughnessMap, empty.aoMap, 0, 1);
    }
    
    /**
     * Constructs a material with specified albedo, normal, metallic, and roughness textures.
     *
     * @param albedoMap    the albedo texture map.
     * @param normalMap    the normal texture map.
     * @param metallicMap  the metallic texture map.
     * @param roughnessMap the roughness texture map.
     */
    public Material(Texture albedoMap, Texture normalMap, Texture metallicMap, Texture roughnessMap) {
        this(albedoMap, normalMap, metallicMap, roughnessMap, empty.aoMap, 0, 1);
    }
    
    /**
     * Constructs a material with specified albedo, normal, metallic, roughness, and AO textures.
     *
     * @param albedoMap    the albedo texture map.
     * @param normalMap    the normal texture map.
     * @param metallicMap  the metallic texture map.
     * @param roughnessMap the roughness texture map.
     * @param aoMap        the ambient occlusion texture map.
     */
    public Material(Texture albedoMap, Texture normalMap, Texture metallicMap, Texture roughnessMap, Texture aoMap) {
        this(albedoMap, normalMap, metallicMap, roughnessMap, aoMap, 0, 1);
    }
    
    /**
     * Constructs a material with all texture maps and scalar parameters.
     *
     * @param albedoMap    the albedo texture map.
     * @param normalMap    the normal texture map.
     * @param metallicMap  the metallic texture map.
     * @param roughnessMap the roughness texture map.
     * @param aoMap        the ambient occlusion texture map.
     * @param metallic     the metallic factor.
     * @param roughness    the roughness factor.
     */
    public Material(Texture albedoMap, Texture normalMap, Texture metallicMap, Texture roughnessMap, Texture aoMap, float metallic, float roughness) {
        this.albedoMap = albedoMap;
        this.normalMap = normalMap;
        this.metallicMap = metallicMap;
        this.roughnessMap = roughnessMap;
        this.aoMap = aoMap;
        this.metallic = metallic;
        this.roughness = roughness;
    }
    
    /**
     * Returns a string representation of the material.
     *
     * @return a string describing the material properties.
     */
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
