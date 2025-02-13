/*package engine;

import engine.components.Camera;
import engine.components.Transform;

import java.util.List;

public class CameraSystem {
    private Camera activeCamera;
    private List<GameObject> gameObjects;  // List of all GameObjects in the scene
    
    public CameraSystem(List<GameObject> gameObjects) {
        this.gameObjects = gameObjects;
        findActiveCamera();
    }
    
    // Set the active camera manually
    public void setActiveCamera(Camera camera) {
        this.activeCamera = camera;
    }
    
    // Automatically find the first camera in the scene and set it as the active camera
    private void findActiveCamera() {
        for (GameObject gameObject : gameObjects) {
            Camera camera = gameObject.getComponent(Camera.class);
            if (camera != null) {
                activeCamera = camera;
                break;
            }
        }
    }
    
    // Update the camera system (update active camera settings)
    public void update() {
        if (activeCamera != null) {
            activeCamera.update();  // Update the camera properties like aspect ratio
            applyCameraProjection();  // Apply projection matrix
        }
    }
    
    // Apply the camera's projection matrix (perspective or orthographic)
    private void applyCameraProjection() {
        if (activeCamera != null) {
            // Set the perspective or orthographic projection matrix here
            if (activeCamera.isOrthographic) {
                activeCamera.setOrthographicProjection();
            } else {
                activeCamera.setPerspectiveProjection();
            }
            
            // Set the view matrix based on camera's transform
            Transform transform = activeCamera.gameObject.transform;
            // Apply the camera's position, rotation, and other transformations for the view matrix
            // Use an appropriate library or custom matrix code to update the view matrix
        }
    }
    
    // Get the active camera
    public Camera getActiveCamera() {
        return activeCamera;
    }
}
*/