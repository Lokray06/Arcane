package engine.editor;

import engine.Component;
import engine.Material;
import engine.Mesh;
import engine.components.Camera;
import engine.components.MeshRenderer;
import engine.components.Transform;
import imgui.ImGui;

import java.util.Set;

import static engine.editor.GUIStyle.*;

public class ComponentGUIs
{
    public final static Set<String> ARCANE_ENGINE_NATIVE_COMPONENTS = Set.of(
            "Camera", "LightAmbient", "LightDirectional", "LightPoint", "MeshRenderer", "TextRenderer", "Rigidbody"
    );
    
    
    public static void renderComponent(Component component) throws IllegalAccessException
    {
        // Not an Arcane engine component
        if(!ARCANE_ENGINE_NATIVE_COMPONENTS.contains(component.getClass().getSimpleName()))
        {
            if(ImGui.treeNode(component.getClass().getSimpleName()))
            {
                ImGui.textColored(GUIStyle.warning, "TODO");
                ImGui.treePop();
            }
        }
        // Is and Arcane engine component
        else
        {
            if(ImGui.treeNode(component.getClass().getSimpleName()))
            {
                if(component.getClass().getSimpleName().equals("Camera"))
                {
                    camera(component);
                }
                else if(component.getClass().getSimpleName().equals("MeshRenderer"))
                {
                    meshRenderer(component);
                }
                
                ImGui.treePop();
            }
        }
    }
    
    // --- Transform ---
    public static void transform(Component component)
    {
        Transform transform = (Transform) component;
        
        if(ImGui.treeNode("Transform"))
        {
            
            // Transform: Position
            float[] pos = {transform.position.x, transform.position.y, transform.position.z};
            if(ImGui.dragFloat3("Position", pos, 0.1f))
            {
                transform.position.set(pos[0], pos[1], pos[2]);
            }
            
            // Transform: Scale
            float[] scale = {transform.scale.x, transform.scale.y, transform.scale.z};
            if(ImGui.dragFloat3("Scale", scale, 0.1f))
            {
                transform.scale.set(scale[0], scale[1], scale[2]);
            }
            
            // Transform: Rotation
            float[] rotation = {transform.rotation.x, transform.rotation.y, transform.rotation.z};
            if(ImGui.dragFloat3("Rotation", rotation, 0.1f))
            {
                transform.rotation.set(rotation[0], rotation[1], rotation[2]);
            }
            
            ImGui.treePop(); // Close the collapsible node
        }
    }
    
    // --- Camera ---
    private static void camera(Component cameraComponent)
    {
        Camera camera = (Camera) cameraComponent;
        
        
    }
    
    // --- Camera ---
    private static void meshRenderer(Component meshRendererComponent)
    {
        MeshRenderer meshRenderer = (MeshRenderer) meshRendererComponent;
        
        Mesh mesh = meshRenderer.mesh;
        if(mesh != null)
        {
            ImGui.textColored(accent, "Mesh: " + mesh.meshName);
        }
        else
        {
            ImGui.textColored(warning, "Mesh: " + null);
        }
        
        Material material = meshRenderer.material;
        if(material != null)
        {
            ImGui.textColored(accent, "Material: " + material.name);
            
            //Metallic
            float[] metallicValue = { material.metallic };
            if (ImGui.sliderFloat("Metallic", metallicValue, 0.0f, 1.0f))
                material.metallic = metallicValue[0];
            
            //Roughness
            float[] roughnessValue = { material.roughness };
            if (ImGui.sliderFloat("Roughness", roughnessValue, 0.0f, 1.0f))
                material.roughness = roughnessValue[0];
        }
        else
        {
            ImGui.textColored(warning, "material: " + null);
        }
    }
}
