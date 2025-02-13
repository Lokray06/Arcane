package engine.utils;

import engine.Component;
import engine.GameObject;

import java.util.ArrayList;
import java.util.List;

public class GameObjectManager
{
    public static int lastId = 0;
    private static List<GameObject> gameObjects = new ArrayList<>();
    
    public static List<GameObject> getGameObjects()
    {
        return gameObjects;
    }
    
    public static void putGameObject(GameObject gameObject)
    {
        gameObjects.add(gameObject);
        GameObjectManager.lastId++;
    }
    
    public static GameObject getGameObjectByID(int id)
    {
        GameObject gameObjectToReturn = null;
        for(GameObject gameObject : gameObjects)
        {
            if(id == gameObject.getId())
            {
                gameObjectToReturn = gameObject;
                break;
            }
        }
        return gameObjectToReturn;
    }
    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        for(GameObject gameObject : gameObjects)
        {
            sb.append(gameObject.getId()).append(" - ").append(gameObject.getName()).append("\n");
            
            // Assuming GameObject has a method to retrieve components
            List<Component> components = gameObject.getComponents();
            for(Component component : components)
            {
                sb.append("    - ").append(component.getClass().getSimpleName()).append("\n");
            }
        }
        
        return sb.toString();
    }
}
