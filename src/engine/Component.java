package engine;

public abstract class Component
{
    public GameObject gameObject;
    
    public void setGameObject(GameObject gameObject)
    {
        this.gameObject = gameObject;
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
