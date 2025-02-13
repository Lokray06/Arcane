package engine;

import engine.components.Transform;
import engine.utils.GameObjectManager;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class GameObject
{
    private final int id;
    private String name;
    public Transform transform; // Transform relative to Scene origin (0, 0, 0)
    
    private List<Component> components;
    public List<GameObject> children; // List of child GameObjects
    public GameObject parent;
    
    // Constructor with only a name
    public GameObject(String name)
    {
        this.id = GameObjectManager.lastId;
        this.name = name;
        this.components = new ArrayList<>();
        this.children = new ArrayList<>();
        this.transform = new Transform(new Vector3f(0), new Vector3f(1), new Vector3f(0));
        this.parent = null;
        GameObjectManager.putGameObject(this);
    }
    
    // Constructor with name and transform
    public GameObject(String name, Transform transform)
    {
        this.id = GameObjectManager.lastId;
        this.name = name;
        this.components = new ArrayList<>();
        this.children = new ArrayList<>();
        this.transform = transform;
        GameObjectManager.putGameObject(this);
    }
    
    // Getter for name
    public String getName()
    {
        return name;
    }
    
    // Add a child to this GameObject
    public void addChild(GameObject child)
    {
        if(child != null)
        {
            child.parent = this; // Set this as the parent of the child
            children.add(child);
        }
    }
    
    // Remove a child
    public void removeChild(GameObject child)
    {
        if(children.remove(child))
        {
            child.parent = null; // Clear the parent of the removed child
        }
    }
    
    // Recursively print hierarchy for debugging
    public void printHierarchy(String prefix)
    {
        System.out.println(prefix + name);
        for(GameObject child : children)
        {
            child.printHierarchy(prefix + "    "); // Indent for child objects
        }
    }
    
    public <T extends Component> void addComponent(Class<T> componentClass)
    {
        try
        {
            T component = componentClass.getDeclaredConstructor().newInstance();
            component.setGameObject(this); // Link the component to the GameObject BEFORE using it
            components.add(component);
        } catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    
    
    public void addComponent(Component component)
    {
        if(component != null)
        {
            components.add(component);
            component.setGameObject(this); // Link the component to the GameObject
        }
    }
    
    public List<Component> getComponents()
    {
        return components;
    }
    
    public <T extends Component> T getComponent(Class<T> componentClass)
    {
        for(Component component : components)
        {
            if(componentClass.isInstance(component))
            {
                return componentClass.cast(component); // Safely cast the component
            }
        }
        return null; // Return null if no matching component is found
    }
    
    public int getId()
    {
        return this.id;
    }
    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("\n");
        for(Component component : components)
        {
            sb.append("    - ").append(component.getClass().getSimpleName()).append("\n");
        }
        return sb.toString();
    }
}
