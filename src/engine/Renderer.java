package engine;

import engine.components.Camera;
import engine.components.LightDirectional;
import engine.components.MeshRenderer;
import engine.components.Skybox;
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
    // Main shader program used for scene rendering.
    private static ShaderProgram shaderProgram;
    private static final String MODEL_UNIFORM = "model";
    private static final String VIEW_UNIFORM = "view";
    private static final String PROJECTION_UNIFORM = "projection";
    
    private static ShaderProgram skyboxShader;
    
    // Cascaded shadow parameters.
    public static int cascadeCount = 4; // Number of cascades.
    public static int baseShadowMapWidth = 2048;
    public static int baseShadowMapHeight = 2048;
    

    
    private static Skybox skybox;
    
    public static void init() {
        // --- Load Main Shader ---
        String vertexSource = FileUtils.loadFileAsString(Engine.shadersPath.concat("vertex.glsl"));
        String fragmentSource = FileUtils.loadFileAsString(Engine.shadersPath.concat("fragment.glsl"));
        shaderProgram = new ShaderProgram(vertexSource, fragmentSource);
        
        // --- Load Skybox Shader ---
        String skyboxVertexSource = FileUtils.loadFileAsString(Engine.shadersPath.concat("skyboxVertex.glsl"));
        String skyboxFragmentSource = FileUtils.loadFileAsString(Engine.shadersPath.concat("skyboxFragment.glsl"));
        skyboxShader = new ShaderProgram(skyboxVertexSource, skyboxFragmentSource);
    }
    
    public static void render(Scene activeScene) {
        Camera mainCamera = getActiveCamera(activeScene);
        if (mainCamera == null) {
            System.err.println("No active camera to render");
            return;
        }
        
        LightDirectional mainLight = getMainDirectionalLight(activeScene);
        if (mainLight == null) {
            System.err.println("No directional light available for shadows.");
            return;
        }
        
        // --- Compute the light view matrix ---
        // (Here we position the light far enough along its direction so the scene is in view.)
        Vector3f lightDir = new Vector3f(mainLight.gameObject.transform.front()).negate();
        Vector3f sceneCenter = new Vector3f(0, 0, 0); // Alternatively, compute the center of your scene.
        Vector3f lightPos = new Vector3f(sceneCenter).sub(new Vector3f(lightDir).mul(30f));
        Matrix4f lightView = new Matrix4f().lookAt(lightPos, sceneCenter, new Vector3f(0, 1, 0));
        
        // --- Render the Main Scene ---
        renderScene(activeScene, mainCamera);
    }
    
    // Renders the main scene (binding cascaded shadow maps, lights, and other textures).
    private static void renderScene(Scene activeScene, Camera mainCamera) {
        GL11.glViewport(0, 0, Engine.WINDOW_WIDTH, Engine.WINDOW_HEIGHT);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        
        shaderProgram.use();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            shaderProgram.setUniformMat4(PROJECTION_UNIFORM, getProjectionMatrix(mainCamera));
            shaderProgram.setUniformMat4(VIEW_UNIFORM, mainCamera.viewMatrix);
            shaderProgram.setUniform("viewPos", mainCamera.gameObject.transform.globalPosition);
        }
        
        // Set directional lights.
        List<LightDirectional> directionalLights = new ArrayList<>();
        if (activeScene.rootGameObject != null) {
            collectDirectionalLights(activeScene.rootGameObject, directionalLights);
        }
        shaderProgram.setUniform("numDirectionalLights", directionalLights.size());
        int maxLights = 10;
        for (int i = 0; i < directionalLights.size() && i < maxLights; i++) {
            LightDirectional light = directionalLights.get(i);
            Vector3f direction = new Vector3f(light.gameObject.transform.front()).negate();
            shaderProgram.setUniform("directionalLights[" + i + "].direction", direction);
            shaderProgram.setUniform("directionalLights[" + i + "].color", light.color);
            shaderProgram.setUniform("directionalLights[" + i + "].strength", light.strength);
        }
        
        // Bind and set the skybox if available.
        skybox = GameObject.getGameObjectWithComponent(Skybox.class).getComponent(Skybox.class);
        if (skybox != null && skybox.getCubeMap() != null) {
            skybox.getCubeMap().bind(GL13.GL_TEXTURE5);
            skybox.getCubeMap().bind(5);
            shaderProgram.setUniform("skyboxAmbient.cubemap", 5);
            shaderProgram.setUniform("skyboxAmbient.strength", 1);
        }
        else
        {
            System.err.println("Couldn't load skybox");
        }
        
        // Render all objects.
        if (activeScene.rootGameObject != null) {
            renderRecursive(activeScene.rootGameObject);
        }
    }
    
    // Helper: Recursively render objects for the main pass.
    private static void renderRecursive(GameObject gameObject) {
        Camera mainCamera = getActiveCamera(Engine.activeScene);
        MeshRenderer meshRenderer = gameObject.getComponent(MeshRenderer.class);
        Skybox skybox = gameObject.getComponent(Skybox.class);
        if (skybox != null && skybox.getCubeMap() != null) {
            skybox.render(skyboxShader, mainCamera.viewMatrix, getProjectionMatrix(mainCamera));
        }
        if (meshRenderer != null && meshRenderer.mesh != null) {
            shaderProgram.use();
            Matrix4f modelMatrix = gameObject.transform.getModelMatrix();
            shaderProgram.setUniformMat4(MODEL_UNIFORM, modelMatrix);
            Material material = meshRenderer.material;
            
            // Bind material textures (albedo, normal, metallic, roughness, AO).
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            material.albedoMap.bind(0);
            shaderProgram.setUniform("uAlbedo", 0);
            
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            material.normalMap.bind(1);
            shaderProgram.setUniform("uNormal", 1);
            
            GL13.glActiveTexture(GL13.GL_TEXTURE2);
            material.metallicMap.bind(2);
            shaderProgram.setUniform("uMetallic", 2);
            
            GL13.glActiveTexture(GL13.GL_TEXTURE3);
            material.roughnessMap.bind(3);
            shaderProgram.setUniform("uRoughness", 3);
            
            GL13.glActiveTexture(GL13.GL_TEXTURE4);
            material.aoMap.bind(4);
            shaderProgram.setUniform("uAO", 4);
            
            // Set extra material parameters.
            shaderProgram.setUniform("uNormalMapStrength", material.normalMapStrength);
            shaderProgram.setUniform("uAlbedoColor", material.albedoColor);
            shaderProgram.setUniform("uMetallicScalar", material.metallic);
            shaderProgram.setUniform("uRoughnessScalar", material.roughness);
            
            meshRenderer.mesh.render();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }
        for (GameObject child : gameObject.children) {
            renderRecursive(child);
        }
    }
    
    // Helper: Recursively collect all directional lights.
    private static void collectDirectionalLights(GameObject gameObject, List<LightDirectional> lights) {
        LightDirectional light = gameObject.getComponent(LightDirectional.class);
        if (light != null) {
            lights.add(light);
        }
        for (GameObject child : gameObject.children) {
            collectDirectionalLights(child, lights);
        }
    }
    
    // Computes the projection matrix for the active camera.
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
    
    private static LightDirectional getMainDirectionalLight(Scene activeScene) {
        List<LightDirectional> directionalLights = new ArrayList<>();
        if (activeScene.rootGameObject != null) {
            collectDirectionalLights(activeScene.rootGameObject, directionalLights);
        }
        return !directionalLights.isEmpty() ? directionalLights.get(0) : null;
    }
    
    public static void cleanup() {
        shaderProgram.cleanup();
    }
}
