package engine;

import engine.components.Transform;

public abstract class Component
{
    public GameObject gameObject;
    public Transform transform;
    
    public void setGameObject(GameObject gameObject)
    {
        this.gameObject = gameObject;
        this.transform = gameObject.transform;
    }
    
    public void start()
    {
        // Called when the component is added to a GameObject
    }
    
    public void fixedUpdate()
    {
        //Called every fixed amount of time
    }
    
    public void update()
    {
        // Called every frame
    }
}
