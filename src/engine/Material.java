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
    public static Material empty = new Material
    (
        new Texture(Type.ALBEDO),
        new Texture(Type.NORMAL),
        new Texture(Type.ROUGHNESS)
    );

    public Material(Texture albedo)
    {
        if(albedo != null)
        {
            this.albedo = albedo;
        }
        else
        {
            System.err.println("Albedo Texture is null");
        }
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
