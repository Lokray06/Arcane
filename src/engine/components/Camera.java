package engine.components;

import engine.Component;
import engine.Engine;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera extends Component
{
    public float fov = 70;
    public float size = 5;
    public float aspectRatio = (float) Engine.WINDOW_WIDTH / Engine.WINDOW_HEIGHT;
    public boolean isOrthographic = false;
    public boolean isActive = false;
    public float near = 0.01f;
    public float far = 1000;
    
    public Camera(float fov)
    {
        this.fov = fov;
    }
    
    public Matrix4f viewMatrix;
    
    @Override
    public void start()
    {
        updateViewMatrix();
    }
    
    @Override
    public void update()
    {
        updateViewMatrix();
    }
    
    private void updateViewMatrix()
    {
        // Create a copy of the camera's position so the original isn't modified
        Vector3f target = new Vector3f(gameObject.transform.position)
                .add(gameObject.transform.getForward());
        
        // Update the view matrix using the camera's position, the target, and the up vector
        viewMatrix = new Matrix4f().lookAt(
                gameObject.transform.position,  // Camera position
                target,                         // Target position (camera's position + forward)
                gameObject.transform.getUp()    // Up vector
        );
    }
    
    
}
