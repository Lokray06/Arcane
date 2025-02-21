package engine.components;

import engine.Component;
import engine.Engine;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The {@code Camera} component represents a camera in the scene.
 * <p>
 * It stores parameters such as field-of-view, near/far clipping planes, and whether it is orthographic.
 * The camera maintains a view matrix that is updated every frame based on its position and orientation.
 * </p>
 */
public class Camera extends Component {
    
    /** Field of view (in degrees) for a perspective camera. */
    public float fov = 70;
    /** Size of the view for an orthographic camera. */
    public float size = 5;
    /** Aspect ratio of the viewport. */
    public float aspectRatio = (float) Engine.WINDOW_WIDTH / Engine.WINDOW_HEIGHT;
    /** Flag indicating whether the camera is orthographic. */
    public boolean isOrthographic = false;
    /** Flag indicating whether this camera is active. */
    public boolean isActive = false;
    /** Near clipping plane distance. */
    public float near = 0.01f;
    /** Far clipping plane distance. */
    public float far = 10000;
    
    /**
     * Constructs a new Camera with the specified field of view.
     *
     * @param fov the field of view in degrees.
     */
    public Camera(float fov) {
        this.fov = fov;
    }
    
    /** The view matrix computed from the camera's transform. */
    public Matrix4f viewMatrix;
    
    /**
     * Called when the component is first started.
     * <p>
     * Initializes the view matrix.
     * </p>
     */
    @Override
    public void start() {
        updateViewMatrix();
    }
    
    /**
     * Called every frame to update the camera.
     * <p>
     * Recalculates the view matrix based on the current transform.
     * </p>
     */
    @Override
    public void update() {
        updateViewMatrix();
    }
    
    /**
     * Updates the view matrix based on the camera's current position and orientation.
     * <p>
     * The view matrix is created using the camera's position, a target position computed as the
     * current position plus the forward (front) direction, and the up vector.
     * </p>
     */
    private void updateViewMatrix() {
        // Create a copy of the camera's position and add the forward vector to compute the target.
        Vector3f target = new Vector3f(gameObject.transform.position)
                .add(gameObject.transform.front());
        
        // Update the view matrix using the camera's position, target, and up vector.
        viewMatrix = new Matrix4f().lookAt(
                gameObject.transform.position,  // Camera position.
                target,                         // Target position.
                gameObject.transform.up()       // Up vector.
        );
    }
}
