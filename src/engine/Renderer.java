package engine;

import engine.components.Camera;
import engine.components.LightDirectional;
import engine.components.MeshRenderer;
import engine.components.Skybox;
import engine.utils.FileUtils;
import engine.utils.ShaderProgram;
import engine.utils.ShadowMap;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;

public class Renderer {
    // Main shader program used for scene rendering.
    private static ShaderProgram shaderProgram;
    private static final String MODEL_UNIFORM = "model";
    private static final String VIEW_UNIFORM = "view";
    private static final String PROJECTION_UNIFORM = "projection";
    private static final String ALBEDO_UNIFORM = "uAlbedo";
    
    // Shadow shader program (used for depth-only rendering).
    private static ShaderProgram shadowShader;
    private static ShaderProgram skyboxShader;
    
    // Dynamic cascade parameters.
    public static int cascadeCount = 4; // Number of cascades.
    public static int baseShadowMapWidth = 2048;
    public static int baseShadowMapHeight = 2048;
    
    // Arrays for cascaded shadow maps, light-space matrices, and split distances.
    public static ShadowMap[] cascadeShadowMaps;
    private static Matrix4f[] cascadeLightSpaceMatrices;
    private static float[] cascadeSplits;
    
    private static Skybox skybox;
    
    // Called once during engine initialization.
    public static void init() {
        // --- Load Main Shader ---
        String vertexSource = FileUtils.loadFileAsString(Engine.shadersPath.concat("vertex.glsl"));
        String fragmentSource = FileUtils.loadFileAsString(Engine.shadersPath.concat("fragment.glsl"));
        shaderProgram = new ShaderProgram(vertexSource, fragmentSource);
        
        // --- Load Shadow Shader ---
        String shadowVertexSource = FileUtils.loadFileAsString(Engine.shadersPath.concat("shadowVertex.glsl"));
        String shadowFragmentSource = FileUtils.loadFileAsString(Engine.shadersPath.concat("shadowFragment.glsl"));
        shadowShader = new ShaderProgram(shadowVertexSource, shadowFragmentSource);
        
        // --- Load Skybox Shader ---
        String skyboxVertexSource = FileUtils.loadFileAsString(Engine.shadersPath.concat("skyboxVertex.glsl"));
        String skyboxFragmentSource = FileUtils.loadFileAsString(Engine.shadersPath.concat("skyboxFragment.glsl"));
        skyboxShader = new ShaderProgram(skyboxVertexSource, skyboxFragmentSource);
        
        // --- Create Cascaded Shadow Maps Dynamically ---
        cascadeShadowMaps = new ShadowMap[cascadeCount];
        for (int i = 0; i < cascadeCount; i++) {
            // Each subsequent cascade has half the resolution of the previous.
            int resWidth = baseShadowMapWidth >> i;   // equivalent to baseShadowMapWidth / (2^i)
            int resHeight = baseShadowMapHeight >> i; // equivalent to baseShadowMapHeight / (2^i)
            cascadeShadowMaps[i] = new ShadowMap(resWidth, resHeight);
        }
        
        // Initialize arrays for light-space matrices and cascade splits.
        cascadeLightSpaceMatrices = new Matrix4f[cascadeCount];
        for (int i = 0; i < cascadeCount; i++) {
            cascadeLightSpaceMatrices[i] = new Matrix4f();
        }
        cascadeSplits = new float[cascadeCount];
        
        // --- Create SSR related stuff ---
        
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
        
        // --- Compute Cascade Splits (Linear Split Example) ---
        float camNear = mainCamera.near;
        float camFar = mainCamera.far;
        for (int i = 0; i < cascadeCount; i++) {
            float p = (i + 1) / (float)cascadeCount;
            cascadeSplits[i] = camNear + (camFar - camNear) * p;
        }
        
        // --- Compute a Common Light View Matrix ---
        Vector3f lightDir = new Vector3f(mainLight.gameObject.transform.front()).negate();
        Vector3f sceneCenter = new Vector3f(0, 0, 0);
        Vector3f lightPos = new Vector3f(sceneCenter).sub(new Vector3f(lightDir).mul(30f));
        Matrix4f lightView = new Matrix4f().lookAt(lightPos, sceneCenter, new Vector3f(0, 1, 0));
        
        // --- Compute Light-Space Matrices for Each Cascade ---
        // (For simplicity, we use a fixed orthographic projection for all cascades.
        //  In a full implementation, compute frustum corners per cascade for tighter bounds.)
        float orthoSize = 20f;
        for (int i = 0; i < cascadeCount; i++) {
            Matrix4f lightProjection = new Matrix4f().ortho(-orthoSize, orthoSize, -orthoSize, orthoSize, camNear, camFar);
            cascadeLightSpaceMatrices[i].set(lightProjection).mul(lightView);
        }
        
        // --- Render Cascaded Shadow Maps ---
        renderCascadedShadowPass(activeScene);
        
        // --- Render the Main Scene ---
        renderScene(activeScene, mainCamera);
    }
    
    // Renders depth from the light's perspective into each cascade's shadow map.
    private static void renderCascadedShadowPass(Scene activeScene) {
        for (int i = 0; i < cascadeCount; i++) {
            GL11.glViewport(0, 0, cascadeShadowMaps[i].width, cascadeShadowMaps[i].height);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, cascadeShadowMaps[i].depthMapFBO);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            
            shadowShader.use();
            shadowShader.setUniformMat4("lightSpaceMatrix", cascadeLightSpaceMatrices[i]);
            
            if (activeScene.rootGameObject != null) {
                renderRecursiveShadow(activeScene.rootGameObject, shadowShader);
            }
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        }
    }
    
    // Renders the main scene using the main shader, binding all cascaded shadow maps.
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
        
        // Bind each cascade shadow map to texture units starting from 1.
        for (int i = 0; i < cascadeCount; i++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE1 + i);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, cascadeShadowMaps[i].depthMap);
            shaderProgram.setUniform("shadowMaps[" + i + "]", 1 + i);
            shaderProgram.setUniformMat4("lightSpaceMatrices[" + i + "]", cascadeLightSpaceMatrices[i]);
            shaderProgram.setUniform("cascadeSplits[" + i + "]", cascadeSplits[i]);
        }
        skybox = GameObject.getGameObjectWithComponent(Skybox.class).getComponent(Skybox.class);
        if (skybox != null && skybox.getCubeMap() != null) {
            // Pass the skybox texture to the shader
            skybox.getCubeMap().bind(GL_TEXTURE0);
            shaderProgram.setUniform("skyboxAmbient.cubemap", 0);
            shaderProgram.setUniform("skyboxAmbient.strength", 0.5f); // Adjust this value as needed
        }
        
        if (activeScene.rootGameObject != null) {
            renderRecursive(activeScene.rootGameObject);
        }
    }
    
    // Recursively renders GameObjects for the shadow pass.
    private static void renderRecursiveShadow(GameObject gameObject, ShaderProgram shader) {
        MeshRenderer meshRenderer = gameObject.getComponent(MeshRenderer.class);
        if (meshRenderer != null && meshRenderer.mesh != null) {
            Matrix4f modelMatrix = gameObject.transform.getModelMatrix();
            shader.setUniformMat4("model", modelMatrix);
            meshRenderer.mesh.render();
        }
        for (GameObject child : gameObject.children) {
            renderRecursiveShadow(child, shader);
        }
    }
    
    // Recursively renders GameObjects for the main pass.
    private static void renderRecursive(GameObject gameObject) {
        Camera mainCamera = getActiveCamera(Engine.activeScene);
        MeshRenderer meshRenderer = gameObject.getComponent(MeshRenderer.class);
        Skybox skybox = gameObject.getComponent(Skybox.class);
        if(skybox != null && skybox.getCubeMap() != null)
        {
            skybox.render(skyboxShader, mainCamera.viewMatrix, getProjectionMatrix(mainCamera));
        }
        if (meshRenderer != null && meshRenderer.mesh != null) {
            shaderProgram.use();
            Matrix4f modelMatrix = gameObject.transform.getModelMatrix();
            shaderProgram.setUniformMat4(MODEL_UNIFORM, modelMatrix);
            Material material = meshRenderer.material;
            
            // --- Bind all components of the material ---
            
            // Bind Albedo (texture unit 0)
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            material.albedoMap.bind(0);
            shaderProgram.setUniform("uAlbedo", 0);
            
            // Bind Normal map (texture unit 1)
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            material.normalMap.bind(1);
            shaderProgram.setUniform("uNormal", 1);
            
            // Bind Roughness map (texture unit 2)
            GL13.glActiveTexture(GL13.GL_TEXTURE2);
            material.roughnessMap.bind(2);
            shaderProgram.setUniform("uRoughness", 2);
            
            // Set material scalar uniforms:
            shaderProgram.setUniform("uMetallic", material.metallic);
            shaderProgram.setUniform("uSpecular", material.roughness);
            
            //Send to render
            meshRenderer.mesh.render();
            //Unbind the texture
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }
        for (GameObject child : gameObject.children) {
            renderRecursive(child);
        }
    }
    
    // Recursively collects all directional lights in the scene.
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
    
    private static LightDirectional getMainDirectionalLight(Scene activeScene) {
        List<LightDirectional> directionalLights = new ArrayList<>();
        if (activeScene.rootGameObject != null) {
            collectDirectionalLights(activeScene.rootGameObject, directionalLights);
        }
        return !directionalLights.isEmpty() ? directionalLights.get(0) : null;
    }
    
    // Called during engine shutdown to clean up shader resources.
    public static void cleanup() {
        shaderProgram.cleanup();
        shadowShader.cleanup();
    }
}