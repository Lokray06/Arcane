package engine.utils;

import engine.Component;
import engine.GameObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code GameObjectManager} class manages all {@link engine.GameObject} instances.
 * <p>
 * It assigns unique IDs to new game objects and provides methods to add and retrieve game objects.
 * </p>
 */
public class GameObjectManager {
    /** The last used game object ID. */
    public static int lastId = 0;
    /** The list storing all game objects. */
    private static List<GameObject> gameObjects = new ArrayList<>();
    
    /**
     * Returns the list of all game objects.
     *
     * @return the list of game objects.
     */
    public static List<GameObject> getGameObjects() {
        return gameObjects;
    }
    
    /**
     * Adds a game object to the manager and increments the last used ID.
     *
     * @param gameObject the game object to add.
     */
    public static void putGameObject(GameObject gameObject) {
        gameObjects.add(gameObject);
        GameObjectManager.lastId++;
    }
    
    /**
     * Retrieves a game object by its unique ID.
     *
     * @param id the ID of the game object.
     * @return the game object with the matching ID, or {@code null} if not found.
     */
    public static GameObject getGameObjectByID(int id) {
        GameObject gameObjectToReturn = null;
        for (GameObject gameObject : gameObjects) {
            if (id == gameObject.getId()) {
                gameObjectToReturn = gameObject;
                break;
            }
        }
        return gameObjectToReturn;
    }
    
    /**
     * Returns a string representation of all game objects managed,
     * including their IDs, names, and attached component types.
     *
     * @return a formatted string of game object details.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        for (GameObject gameObject : gameObjects) {
            sb.append(gameObject.getId()).append(" - ").append(gameObject.getName()).append("\n");
            
            // Assuming GameObject has a method to retrieve components
            List<Component> components = gameObject.getComponents();
            for (Component component : components) {
                sb.append("    - ").append(component.getClass().getSimpleName()).append("\n");
            }
        }
        
        return sb.toString();
    }
}
