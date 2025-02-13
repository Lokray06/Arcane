package engine;

import static engine.Texture.Type;

public class Material
{
    public Texture albedo;
    public Texture normal;
    public Texture roughness;
    
    public float specular = 0;
    public float metallic = 0;
    
    //Default empty material
    public Material()
    {
        this.albedo = new Texture(Type.ALBEDO);
        this.normal = new Texture(Type.NORMAL);
        this.roughness = new Texture(Type.ROUGHNESS);
    }
    
    public Material(Texture albedo)
    {
        this.albedo = albedo;
    }
    
    public Material(Texture albedo, Texture normal)
    {
        this.albedo = albedo;
        this.normal = normal;
    }
    
    public Material(Texture albedo, Texture normal, Texture roughness)
    {
        this.albedo = albedo;
        this.normal = normal;
        this.roughness = roughness;
    }
    
    @Override
    public String toString()
    {
        return "Material{" + "albedo=" + albedo + ", normal=" + normal + ", roughness=" + roughness + ", specular=" + specular + ", metallic=" + metallic + '}';
    }
}
