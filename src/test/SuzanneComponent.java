package test;

import engine.Component;
import engine.GameObject;
import engine.Input;
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
            System.out.println(child.transform);
        }
        
        // Handle movement
        if (Input.getKey("left")) {
            gameObject.transform.position.x--;
        }
        if (Input.getKey("right")) {
            gameObject.transform.position.x++;
        }
    }
}
