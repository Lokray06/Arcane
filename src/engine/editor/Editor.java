package engine.editor;

import engine.Component;
import engine.Mesh;
import engine.components.MeshRenderer;
import imgui.ImGui;
import imgui.type.ImString;
import engine.GameObject;
import engine.Scene;

import java.lang.reflect.Field;

/**
 * Editor class that renders a left panel with the scene hierarchy
 * and a right panel with the selected GameObject's properties.
 */
public class Editor
{
    private boolean showEditor = true;
    private GameObject selectedGameObject = null;
    
    public void toggle()
    {
        showEditor = !showEditor;
    }
    
    public void render(Scene activeScene) throws IllegalAccessException
    {
        if(!showEditor)
        {
            return;
        }
        
        float displayWidth = ImGui.getIO().getDisplaySizeX();
        float displayHeight = ImGui.getIO().getDisplaySizeY();
        float leftPanelWidth = 300.0f;
        float rightPanelWidth = 300.0f;
        float centerWidth = displayWidth - leftPanelWidth - rightPanelWidth;
        
        // ===== Left Panel: Scene Hierarchy =====
        ImGui.setNextWindowPos(0, 0);
        ImGui.setNextWindowSize(leftPanelWidth, displayHeight);
        ImGui.begin("Scene Hierarchy");
        if(activeScene != null)
        {
            for(GameObject child : activeScene.getRootGameObject().children)
            {
                renderGameObjectNode(child);
            }
        }
        ImGui.end();
        
        // ===== Right Panel: Properties =====
        ImGui.setNextWindowPos(leftPanelWidth + centerWidth, 0);
        ImGui.setNextWindowSize(rightPanelWidth, displayHeight);
        ImGui.begin("Properties");
        if(selectedGameObject != null)
        {
            ImString nameBuffer = new ImString(selectedGameObject.getName(), 256);
            if(ImGui.inputText("Name", nameBuffer))
            {
                selectedGameObject.setName(nameBuffer.get());
            }
            
            ComponentGUIs.transform(selectedGameObject.transform);
            for(Component component : selectedGameObject.getComponents())
            {
                ComponentGUIs.renderComponent(component);
            }
        }
        else
        {
            ImGui.text("No game object selected.");
        }
        ImGui.end();
    }
    
    private void renderGameObjectNode(GameObject go)
    {
        boolean nodeOpen = ImGui.treeNode(go.getName());
        if(ImGui.isItemClicked())
        {
            selectedGameObject = go;
        }
        if(nodeOpen)
        {
            for(GameObject child : go.children)
            {
                renderGameObjectNode(child);
            }
            ImGui.treePop();
        }
    }
    
    public boolean isVisible()
    {
        return showEditor;
    }
    
    public void setVisible(boolean visible)
    {
        this.showEditor = visible;
    }
    
    public GameObject getSelectedGameObject()
    {
        return selectedGameObject;
    }
}
