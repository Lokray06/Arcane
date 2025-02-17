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
        float moveSpeed = (float) (10f * Time.deltaTime);
        float sunSpeed = (float) (0.01f * Time.deltaTime);
        float rotationSpeed = (float) (0.5f * Time.deltaTime);
        
        // Rotate the object
        if(gameObject.getName().equals("Sun"))
        {
            //gameObject.transform.rotation.x += sunSpeed;
        }
        else if (gameObject.getName().equals("box"))
        {
            gameObject.transform.rotation.y += rotationSpeed;
            gameObject.children.getFirst().transform.rotation.x += rotationSpeed * 5;
        }
        
        else if (gameObject.getName().equals("Suzanne2"))
        {
        }
        
        // Handle movement
        if(Input.getKey("left"))
        {
            transform.move(transform.left, moveSpeed);
        }
        if(Input.getKey("right"))
        {
            transform.move(transform.right, moveSpeed);
        }
    }
}
