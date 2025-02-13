package engine;

import java.util.ArrayList;
import java.util.List;

public class Scene
{
    private String name;
    
    // Root GameObject of the scene
    public GameObject rootGameObject;
    
    private List<GameObject> gameObjects;
    
    public Scene(String name)
    {
        this.name = name;
        this.gameObjects = new ArrayList<>();
        this.rootGameObject = new GameObject("Root game object");
        // Ensure the root GameObject is always part of the scene
        this.addGameObject(rootGameObject);
    }
    
    // Add a GameObject to the scene
    public void addGameObject(GameObject gameObject)
    {
        gameObjects.add(gameObject);
        // If it's not the root object, make it a child of the root
        if(gameObject != rootGameObject)
        {
            gameObject.parent = rootGameObject;
            rootGameObject.addChild(gameObject); // Ensure it's added to root's children list
        }
    }
    
    // Remove a GameObject from the scene
    public void removeGameObject(GameObject gameObject)
    {
        gameObjects.remove(gameObject);
        // If it's a child of the root, remove it from the root's children list
        if(gameObject.parent == rootGameObject)
        {
            rootGameObject.removeChild(gameObject);
        }
    }
    
    // Get all GameObjects in the scene
    public List<GameObject> getGameObjects()
    {
        return gameObjects;
    }
    
    // Find a GameObject by its ID
    public GameObject getGameObjectById(int id)
    {
        for(GameObject gameObject : gameObjects)
        {
            if(gameObject.getId() == id)
            {
                return gameObject;
            }
        }
        return null;
    }
    
    // Get the root GameObject of the scene
    public GameObject getRootGameObject()
    {
        return rootGameObject;
    }
    
    public String getName()
    {
        return name;
    }
    
    // Call start() for all components in the scene
    public void start()
    {
        for(GameObject gameObject : gameObjects)
        {
            for(Component component : gameObject.getComponents())
            {
                component.start();
            }
        }
    }
    
    //Call fixedUpdate for all the components in the scene
    public void fixedUpdate()
    {
        for(GameObject gameObject : gameObjects)
        {
            for(Component component : gameObject.getComponents())
            {
                component.fixedUpdate();
            }
        }
    }
    
    // Call update() for all components in the scene
    public void update()
    {
        for(GameObject gameObject : gameObjects)
        {
            for(Component component : gameObject.getComponents())
            {
                component.update();
            }
        }
    }
    
    // Optional: Display the scene’s GameObjects in a readable format
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Scene contains the following game objects:\n");
        for(GameObject gameObject : gameObjects)
        {
            sb.append(gameObject.toString()).append("\n");
        }
        return sb.toString();
    }
}
