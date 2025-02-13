package engine;

import engine.components.Camera;
import engine.components.MeshRenderer;
import engine.utils.FileUtils;
import engine.utils.ShaderProgram;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.system.MemoryStack;

public class Renderer {
    
    // Our shader program used for all rendering
    private static ShaderProgram shaderProgram;
    
    // Uniform names used in the shader
    private static final String MODEL_UNIFORM = "model";
    private static final String VIEW_UNIFORM = "view";
    private static final String PROJECTION_UNIFORM = "projection";
    
    // Material texture uniforms (make sure your shader uses these names)
    private static final String ALBEDO_UNIFORM = "uAlbedo";
    private static final String NORMAL_UNIFORM = "uNormal";
    private static final String ROUGHNESS_UNIFORM = "uRoughness";
    
    // Call this once during initialization
    public static void init() {
        String vertexSource;
        String fragmentSource;
        
        String vertexSourcePath = Engine.shadersPath.concat("vertex.glsl");
        String fragmentSourcePath = Engine.shadersPath.concat("fragment.glsl");
        
        System.out.println(vertexSourcePath);
        
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
        
        // Set projection and view matrices from the camera.
        try (MemoryStack stack = MemoryStack.stackPush()) {
            shaderProgram.setUniformMat4(PROJECTION_UNIFORM, getProjectionMatrix(mainCamera));
            shaderProgram.setUniformMat4(VIEW_UNIFORM, mainCamera.viewMatrix);
        }
        
        // Render the meshes
        for (GameObject gameObject : activeScene.getGameObjects()) {
            MeshRenderer meshRenderer = gameObject.getComponent(MeshRenderer.class);
            if (meshRenderer != null && meshRenderer.mesh != null) {
                // Set the model matrix
                Matrix4f modelMatrix = gameObject.transform.getTransformationMatrix();
                shaderProgram.setUniformMat4(MODEL_UNIFORM, modelMatrix);
                
                Material material = meshRenderer.material;
                System.out.println(gameObject + "" + material);
                if (material.albedo != null) {
                    GL13.glActiveTexture(GL13.GL_TEXTURE0);
                    material.albedo.bind(0);
                    shaderProgram.setUniform(ALBEDO_UNIFORM, 0);
                }
                
                // Render the mesh
                meshRenderer.mesh.render();
                
                // Unbind the texture after rendering
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            }
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
