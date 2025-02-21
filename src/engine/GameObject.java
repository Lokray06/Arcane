package engine;

import engine.components.Transform;
import engine.utils.GameObjectManager;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents an object within the game scene.
 * <p>
 * A GameObject can have components that define its behavior,
 * a transform that defines its position, rotation, and scale relative to the scene origin,
 * and a hierarchy of child GameObjects.
 * </p>
 */
public class GameObject {
    /** Unique identifier for the GameObject. */
    private final int id;
    /** Name of the GameObject. */
    private String name;
    /** Transform representing position, rotation, and scale relative to the scene origin. */
    public Transform transform;
    
    /** List of components attached to this GameObject. */
    private List<Component> components;
    /** List of child GameObjects. */
    public List<GameObject> children;
    /** Parent GameObject, if any. */
    public GameObject parent;
    
    /**
     * Constructs a new GameObject with the specified name.
     * <p>
     * This constructor initializes a new Transform with default values,
     * an empty list of components, and an empty list of children.
     * The GameObject is also registered with the GameObjectManager.
     * </p>
     *
     * @param name the name of the GameObject.
     */
    public GameObject(String name) {
        this.id = GameObjectManager.lastId;
        this.name = name;
        this.components = new ArrayList<>();
        this.children = new ArrayList<>();
        this.transform = new Transform(new Vector3f(0), new Vector3f(1), new Vector3f(0));
        this.parent = null;
        GameObjectManager.putGameObject(this);
    }
    
    /**
     * Constructs a new GameObject with the specified name and transform.
     * <p>
     * This constructor initializes an empty list of components and children,
     * and registers the GameObject with the GameObjectManager.
     * </p>
     *
     * @param name the name of the GameObject.
     * @param transform the Transform for the GameObject.
     */
    public GameObject(String name, Transform transform) {
        this.id = GameObjectManager.lastId;
        this.name = name;
        this.components = new ArrayList<>();
        this.children = new ArrayList<>();
        this.transform = transform;
        GameObjectManager.putGameObject(this);
    }
    
    /**
     * Retrieves the name of the GameObject.
     *
     * @return the GameObject's name.
     */
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Adds a child GameObject to this GameObject.
     * <p>
     * The child's parent is set to this GameObject, and its transform is linked
     * to this GameObject's transform.
     * </p>
     *
     * @param child the child GameObject to add.
     */
    public void addChild(GameObject child) {
        if (child != null) {
            child.parent = this;
            // Link the child's transform to its parent's transform.
            child.transform.setParent(this.transform);
            children.add(child);
        }
    }
    
    /**
     * Removes a child GameObject from this GameObject.
     * <p>
     * If the child is successfully removed, its parent is set to null.
     * </p>
     *
     * @param child the child GameObject to remove.
     */
    public void removeChild(GameObject child) {
        if (children.remove(child)) {
            child.parent = null; // Clear the parent reference.
        }
    }
    
    /**
     * Recursively prints the hierarchy of this GameObject and its children.
     * <p>
     * Each level of hierarchy is indented using the provided prefix.
     * </p>
     *
     * @param prefix the string prefix used for indentation.
     */
    public void printHierarchy(String prefix) {
        System.out.println(prefix + name);
        for (GameObject child : children) {
            child.printHierarchy(prefix + "    ");
        }
    }
    
    /**
     * Adds a component of the specified class to this GameObject.
     * <p>
     * The component is instantiated using its no-argument constructor,
     * and its GameObject reference is set before it is added.
     * </p>
     *
     * @param <T> the type of the component.
     * @param componentClass the class of the component to add.
     */
    public <T extends Component> void addComponent(Class<T> componentClass) {
        try {
            T component = componentClass.getDeclaredConstructor().newInstance();
            component.setGameObject(this); // Link the component to this GameObject.
            components.add(component);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Adds the specified component instance to this GameObject.
     * <p>
     * The component's GameObject reference is set to this GameObject.
     * </p>
     *
     * @param component the component instance to add.
     */
    public void addComponent(Component component) {
        if (component != null) {
            components.add(component);
            component.setGameObject(this); // Link the component to this GameObject.
        }
    }
    
    /**
     * Retrieves the list of components attached to this GameObject.
     *
     * @return a list of components.
     */
    public List<Component> getComponents() {
        return components;
    }
    
    /**
     * Retrieves a component of the specified class from this GameObject.
     * <p>
     * If multiple components of the specified type exist, the first one encountered is returned.
     * </p>
     *
     * @param <T> the type of the component.
     * @param componentClass the class of the component to retrieve.
     * @return the component instance if found, otherwise null.
     */
    public <T extends Component> T getComponent(Class<T> componentClass) {
        for (Component component : components) {
            if (componentClass.isInstance(component)) {
                return componentClass.cast(component); // Safely cast the component.
            }
        }
        return null; // Return null if no matching component is found.
    }
    
    /**
     * Retrieves the unique identifier of this GameObject.
     *
     * @return the GameObject's ID.
     */
    public int getId() {
        return this.id;
    }
    
    /**
     * Returns a string representation of the GameObject.
     * <p>
     * The representation includes the GameObject's name and a list of its component types.
     * </p>
     *
     * @return a string representation of the GameObject.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("\n");
        for (Component component : components) {
            sb.append("    - ").append(component.getClass().getSimpleName()).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * Recursively searches for a GameObject that has a component of the specified class,
     * avoiding cycles by keeping track of visited game objects.
     *
     * @param <T> the type of the component.
     * @param componentClass the class of the component to search for.
     * @return the first GameObject containing the component, or null if not found.
     */
    public static <T extends Component> GameObject getGameObjectWithComponent(Class<T> componentClass) {
        return getGameObjectWithComponent(componentClass, new HashSet<>());
    }
    
    private static <T extends Component> GameObject getGameObjectWithComponent(Class<T> componentClass, Set<GameObject> visited) {
        for (GameObject gameObject : Engine.activeScene.getGameObjects()) {
            GameObject result = getGameObjectWithComponent(gameObject, componentClass, visited);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
    
    private static <T extends Component> GameObject getGameObjectWithComponent(GameObject gameObject, Class<T> componentClass, Set<GameObject> visited) {
        if (visited.contains(gameObject)) {
            return null;
        }
        visited.add(gameObject);
        
        if (gameObject.getComponent(componentClass) != null) {
            return gameObject;
        }
        for (GameObject child : gameObject.children) {
            GameObject result = getGameObjectWithComponent(child, componentClass, visited);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
