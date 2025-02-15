package test;

import engine.Component;
import engine.GameObject;
import engine.Input;
import engine.Time;
import org.joml.Vector3f;

public class SuzanneComponent extends Component
{
    private long lastUpdateTime = System.nanoTime(); // Store the last update time
    
    @Override
    public void update()
    {
        // Rotate the object
        if(gameObject.getName().equals("Sun"))
        {
            //gameObject.transform.rotation.x += 1f * Time.deltaTime;
        }
        else
        {
            gameObject.transform.rotation.y += 1f * Time.deltaTime;
            gameObject.transform.rotation.x += 1f * Time.deltaTime;
        }
        if(!gameObject.children.isEmpty())
        {
            GameObject child = gameObject.children.getFirst();
        }
        
        // Handle movement
        if(Input.getKey("left"))
        {
            transform.move(transform.left, 0.2f);
        }
        if(Input.getKey("right"))
        {
            transform.move(transform.right, 0.2f);
        }
    }
}
