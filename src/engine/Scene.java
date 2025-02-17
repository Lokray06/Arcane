package engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a scene containing a collection of GameObjects.
 * <p>
 * A Scene has a root GameObject that acts as the parent for all top-level GameObjects.
 * When a new GameObject is added and is not the root, it is automatically linked as a child of the root.
 * </p>
 */
public class Scene {
    /** The name of the scene. */
    private String name;
    
    /** The root GameObject of the scene. */
    public GameObject rootGameObject;
    
    /** A list of all top-level GameObjects in the scene. */
    private List<GameObject> gameObjects;
    
    /**
     * Constructs a new Scene with the specified name.
     * <p>
     * A root GameObject named "Root game object" is created and automatically added to the scene.
     * </p>
     *
     * @param name the name of the scene.
     */
    public Scene(String name) {
        this.name = name;
        this.gameObjects = new ArrayList<>();
        this.rootGameObject = new GameObject("Root game object");
        // Ensure the root GameObject is always part of the scene.
        this.addGameObject(rootGameObject);
    }
    
    /**
     * Adds a GameObject to the scene.
     * <p>
     * Only GameObjects without a parent (top-level objects) are allowed.
     * If the GameObject is not the root, its parent is set to the root and it is added as a child.
     * </p>
     *
     * @param gameObject the GameObject to add.
     */
    public void addGameObject(GameObject gameObject) {
        // Only add the object if it has no parent.
        if (gameObject.parent != null) {
            System.out.println("Warning: GameObject '" + gameObject.getName() + "' already has a parent and will not be added as a top-level object.");
            return;
        }
        
        gameObjects.add(gameObject);
        
        // Automatically set its parent to the root if it is not already the root.
        if (gameObject != rootGameObject) {
            gameObject.parent = rootGameObject;
            rootGameObject.addChild(gameObject);
        }
    }
    
    /**
     * Removes a GameObject from the scene.
     * <p>
     * If the removed GameObject is a child of the root, it is also removed from the root's children list.
     * </p>
     *
     * @param gameObject the GameObject to remove.
     */
    public void removeGameObject(GameObject gameObject) {
        gameObjects.remove(gameObject);
        if (gameObject.parent == rootGameObject) {
            rootGameObject.removeChild(gameObject);
        }
    }
    
    /**
     * Retrieves all top-level GameObjects in the scene.
     *
     * @return the list of GameObjects in the scene.
     */
    public List<GameObject> getGameObjects() {
        return gameObjects;
    }
    
    /**
     * Finds a GameObject in the scene by its unique identifier.
     *
     * @param id the ID of the GameObject.
     * @return the GameObject with the matching ID, or null if not found.
     */
    public GameObject getGameObjectById(int id) {
        for (GameObject gameObject : gameObjects) {
            if (gameObject.getId() == id) {
                return gameObject;
            }
        }
        return null;
    }
    
    /**
     * Retrieves the root GameObject of the scene.
     *
     * @return the root GameObject.
     */
    public GameObject getRootGameObject() {
        return rootGameObject;
    }
    
    /**
     * Retrieves the name of the scene.
     *
     * @return the scene's name.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns a string representation of the scene, listing all GameObjects.
     *
     * @return a string that describes the scene's GameObjects.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Scene contains the following game objects:\n");
        for (GameObject gameObject : gameObjects) {
            sb.append(gameObject.toString()).append("\n");
        }
        return sb.toString();
    }
}
