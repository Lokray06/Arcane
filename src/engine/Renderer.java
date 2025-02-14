package engine;

import engine.components.Camera;
import engine.components.LightDirectional;
import engine.components.MeshRenderer;
import engine.utils.FileUtils;
import engine.utils.ShaderProgram;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.List;

public class Renderer {
    
    // Our shader program used for all rendering
    private static ShaderProgram shaderProgram;
    
    // Uniform names used in the shader
    private static final String MODEL_UNIFORM = "model";
    private static final String VIEW_UNIFORM = "view";
    private static final String PROJECTION_UNIFORM = "projection";
    
    // Material texture uniform (make sure your shader uses these names)
    private static final String ALBEDO_UNIFORM = "uAlbedo";
    
    // Call this once during initialization
    public static void init() {
        String vertexSource;
        String fragmentSource;
        
        String vertexSourcePath = Engine.shadersPath.concat("vertex.glsl");
        String fragmentSourcePath = Engine.shadersPath.concat("fragment.glsl");
        
        vertexSource = FileUtils.loadFileAsString(vertexSourcePath);
        fragmentSource = FileUtils.loadFileAsString(fragmentSourcePath);
        
        shaderProgram = new ShaderProgram(vertexSource, fragmentSource);
    }
    
    public static void render(Scene activeScene) {
        Camera mainCamera = getActiveCamera(activeScene);
        if (mainCamera == null) {
            System.err.println("No active camera to render");
            return;
        }
        
        // Activate our shader program.
        shaderProgram.use();
        
        // Set projection, view matrices and view position from the camera.
        try (MemoryStack stack = MemoryStack.stackPush()) {
            shaderProgram.setUniformMat4(PROJECTION_UNIFORM, getProjectionMatrix(mainCamera));
            shaderProgram.setUniformMat4(VIEW_UNIFORM, mainCamera.viewMatrix);
            // Assume the Camera component is attached to a GameObject with a valid transform
            shaderProgram.setUniform("viewPos", mainCamera.gameObject.transform.globalPosition);
        }
        
        // Collect all directional lights in the scene.
        List<LightDirectional> directionalLights = new ArrayList<>();
        if (activeScene.rootGameObject != null) {
            collectDirectionalLights(activeScene.rootGameObject, directionalLights);
        }
        
        // Pass the number of directional lights to the shader.
        shaderProgram.setUniform("numDirectionalLights", directionalLights.size());
        
        // For each directional light, pass its properties.
        // (Limit to MAX_DIR_LIGHTS if needed; here we assume a shader-defined max of 10.)
        int maxLights = 10;
        for (int i = 0; i < directionalLights.size() && i < maxLights; i++) {
            LightDirectional light = directionalLights.get(i);
            // Compute the light's direction from its transform.
            // Here, we assume the light's direction is the negative of its "front" vector.
            Vector3f direction = new Vector3f(light.gameObject.transform.front()).negate();
            shaderProgram.setUniform("directionalLights[" + i + "].direction", direction);
            shaderProgram.setUniform("directionalLights[" + i + "].color", light.color);
            shaderProgram.setUniform("directionalLights[" + i + "].strength", light.strength);
        }
        
        // Now render all objects recursively.
        if (activeScene.rootGameObject != null) {
            renderRecursive(activeScene.rootGameObject);
        }
    }
    
    /**
     * Recursively renders a GameObject and its children.
     */
    private static void renderRecursive(GameObject gameObject) {
        MeshRenderer meshRenderer = gameObject.getComponent(MeshRenderer.class);
        if (meshRenderer != null && meshRenderer.mesh != null) {
            // Use the global transform to build the model matrix.
            Matrix4f modelMatrix = gameObject.transform.getModelMatrix();
            shaderProgram.setUniformMat4(MODEL_UNIFORM, modelMatrix);
            
            Material material = meshRenderer.material;
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            material.albedo.bind(0);
            shaderProgram.setUniform(ALBEDO_UNIFORM, 0);
            
            // Render the mesh.
            meshRenderer.mesh.render();
            
            // Unbind texture.
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }
        
        // Recursively render all child game objects.
        for (GameObject child : gameObject.children) {
            renderRecursive(child);
        }
    }
    
    /**
     * Recursively collects all LightDirectional components in the scene.
     */
    private static void collectDirectionalLights(GameObject gameObject, List<LightDirectional> lights) {
        LightDirectional light = gameObject.getComponent(LightDirectional.class);
        if (light != null) {
            lights.add(light);
        }
        for (GameObject child : gameObject.children) {
            collectDirectionalLights(child, lights);
        }
    }
    
    private static Matrix4f getProjectionMatrix(Camera camera) {
        Matrix4f projectionMatrix = new Matrix4f();
        float aspectRatio = camera.aspectRatio;
        
        if (camera.isOrthographic) {
            float orthoSize = camera.size;
            projectionMatrix.ortho(-orthoSize * aspectRatio, orthoSize * aspectRatio, -orthoSize, orthoSize, camera.near, camera.far);
        } else {
            projectionMatrix.perspective((float) Math.toRadians(camera.fov), aspectRatio, camera.near, camera.far);
        }
        return projectionMatrix;
    }
    
    private static Camera getActiveCamera(Scene activeScene) {
        for (GameObject gameObject : activeScene.getGameObjects()) {
            Camera camera = gameObject.getComponent(Camera.class);
            if (camera != null && camera.isActive) {
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
