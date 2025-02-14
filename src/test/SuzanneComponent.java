package test;

import engine.Component;
import engine.GameObject;
import engine.Input;
import engine.Time;
import org.joml.Vector3f;

public class SuzanneComponent extends Component {
    private long lastUpdateTime = System.nanoTime(); // Store the last update time
    
    @Override
    public void fixedUpdate() {
        // Get the current time
        long currentTime = System.nanoTime();
        
        // Calculate time difference in milliseconds
        float deltaTime = (currentTime - lastUpdateTime) / 1_000_000f;
        
        // Update the last update time
        lastUpdateTime = currentTime;
        
        // Print the time passed since last update
        //System.out.println("Delta time: " + deltaTime + " ms");
        
        // Rotate the object
        gameObject.transform.rotation.y += 0.02f;
        if(!gameObject.children.isEmpty())
        {
            GameObject child = gameObject.children.getFirst();
        }
        
        // Handle movement
        if (Input.getKey("left")) {
            transform.move(transform.left, 0.2f);
        }
        if (Input.getKey("right")) {
            transform.move(transform.right, 0.2f);
        }
    }
}
