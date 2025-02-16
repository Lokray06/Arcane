package test;

import engine.Component;
import engine.GameObject;
import engine.Input;
import engine.Time;
import org.joml.Vector3f;

public class SuzanneComponent extends Component
{
    @Override
    public void update()
    {
        // Rotate the object
        if(gameObject.getName().equals("Sun"))
        {
            //gameObject.transform.rotation.x += 1f * Time.deltaTime;
        }
        else if (gameObject.getName().equals("box"))
        {
            gameObject.transform.rotation.y += Time.deltaTime;
        }
        
        else if (gameObject.getName().equals("Suzanne2"))
        {
            gameObject.transform.rotation.x += Time.deltaTime;
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
