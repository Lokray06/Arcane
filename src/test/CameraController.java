package test;

import engine.Component;
import engine.Input;
import engine.Time;
import org.joml.Vector3f;

public class CameraController extends Component
{
    public float moveSpeed = 5;
    public float sensitivity = 0.005f;

    private float rotationX = 0, rotationY = 0;

    @Override
    public void update()
    {
        if(GameStuff.inGame)
        {
            Vector3f forward = gameObject.transform.front();
            forward.y = 0;
            Vector3f right = gameObject.transform.right();

            // Movement controls
            if(Input.getKey("ctrl"))
            {
                moveSpeed = 20;
            }
            else
            {
                moveSpeed =10;
            }
            moveSpeed *= Time.deltaTime;
            
            if(Input.getKey("space"))
            {
                gameObject.transform.position.y += moveSpeed;
            }
            if(Input.getKey("shift"))
            {
                gameObject.transform.position.y -= moveSpeed;
            }
            
            if(Input.getKey("w"))
            {
                gameObject.transform.position.add(forward.mul(moveSpeed));
            }
            if(Input.getKey("s"))
            {
                gameObject.transform.position.add(forward.mul(-moveSpeed));
            }
            if(Input.getKey("a"))
            {
                gameObject.transform.position.add(right.mul(-moveSpeed));
            }
            if(Input.getKey("d"))
            {
                gameObject.transform.position.add(right.mul(moveSpeed));
            }
            
            // Mouse look
            float mouseDeltaX = (float) Input.getMouseDeltaX();
            float mouseDeltaY = (float) Input.getMouseDeltaY();

            rotationX += -mouseDeltaY * sensitivity; // Pitch
            rotationY += -mouseDeltaX * sensitivity; // Yaw

            // Clamp pitch to avoid flipping
            rotationX = Math.max(-89f, Math.min(89f, rotationX));

            // Apply rotation using quaternion-based method
            gameObject.transform.rotation.set(0, rotationY, 0);
            gameObject.transform.rotate(new Vector3f(1, 0, 0), rotationX);
        }
    }
}