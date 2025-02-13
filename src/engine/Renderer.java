package engine;

import engine.components.Camera;
import engine.components.MeshRenderer;
import engine.components.Transform;
import engine.utils.FileUtils;
import engine.utils.ShaderProgram;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.io.BufferedReader;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

public class Renderer {
    
    // Our shader program used for all rendering
    private static ShaderProgram shaderProgram;
    
    // Uniform names used in the shader
    private static final String MODEL_UNIFORM = "model";
    private static final String VIEW_UNIFORM = "view";
    private static final String PROJECTION_UNIFORM = "projection";
    
    // Call this once during initialization
    public static void init() {
        // Load shader source code from file or use hardcoded strings.
        String vertexSource;
        String fragmentSource;
        
        String vertexSourcePath = Engine.shadersPath.concat("vertex.glsl");
        String fragmentSourcePath = Engine.shadersPath.concat("fragment.glsl");
        
        System.out.println(vertexSourcePath);
        
        vertexSource = FileUtils.loadFileAsString(vertexSourcePath);
        fragmentSource = FileUtils.loadFileAsString(fragmentSourcePath);
        
        // For demonstration, we'll assume the shader source strings are provided.
        shaderProgram = new ShaderProgram(vertexSource, fragmentSource);
    }
    
    public static void render(Scene activeScene) {
        Camera mainCamera = getActiveCamera(activeScene);
        if(mainCamera == null) {
            System.err.println("No active camera to render");
            return;
        }
        
        // Activate our shader program.
        shaderProgram.use();
        
        // Set projection and view matrices from the camera.
        try (MemoryStack stack = MemoryStack.stackPush()) {
            shaderProgram.setUniformMat4(PROJECTION_UNIFORM, getProjectionMatrix(mainCamera));
            shaderProgram.setUniformMat4(VIEW_UNIFORM, mainCamera.viewMatrix);
        }
        
        // Render the meshes
        for(GameObject gameObject : activeScene.getGameObjects()) {
            MeshRenderer meshRenderer = gameObject.getComponent(MeshRenderer.class);
            if(meshRenderer != null && meshRenderer.mesh != null) {
                // Set the model matrix uniform
                Matrix4f modelMatrix = gameObject.transform.getTransformationMatrix();
                shaderProgram.setUniformMat4(MODEL_UNIFORM, modelMatrix);
                // Render the mesh
                meshRenderer.mesh.render();
            }
        }
    }
    
    private static Matrix4f getProjectionMatrix(Camera camera) {
        Matrix4f projectionMatrix = new Matrix4f();
        float aspectRatio = camera.aspectRatio;
        
        if(camera.isOrthographic) {
            float orthoSize = camera.size;
            projectionMatrix.ortho(-orthoSize * aspectRatio, orthoSize * aspectRatio, -orthoSize, orthoSize, camera.near, camera.far);
        } else {
            projectionMatrix.perspective((float)Math.toRadians(camera.fov), aspectRatio, camera.near, camera.far);
        }
        return projectionMatrix;
    }
    
    private static Camera getActiveCamera(Scene activeScene) {
        for(GameObject gameObject : activeScene.getGameObjects()) {
            Camera camera = gameObject.getComponent(Camera.class);
            if(camera != null && camera.isActive) {
                return camera;
            }
        }
        return null;
    }
    
    // Call this during engine shutdown to clean up shader resources.
    public static void cleanup() {
        shaderProgram.cleanup();
    }
}
