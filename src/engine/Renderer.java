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
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.List;

public class Renderer {
    // Main shader program used for scene rendering.
    private static ShaderProgram shaderProgram;
    private static final String MODEL_UNIFORM = "model";
    private static final String VIEW_UNIFORM = "view";
    private static final String PROJECTION_UNIFORM = "projection";
    
    // Shadow shader program (used for depth-only rendering).
    private static ShaderProgram shadowShader;
    private static ShaderProgram skyboxShader;
    
    // Cascaded shadow parameters.
    public static int cascadeCount = 4; // Number of cascades.
    public static int baseShadowMapWidth = 2048;
    public static int baseShadowMapHeight = 2048;
    
    // Arrays for cascaded shadow maps, light-space matrices, and split distances.
    public static ShadowMap[] cascadeShadowMaps;
    private static Matrix4f[] cascadeLightSpaceMatrices;
    private static float[] cascadeSplits;
    
    private static Skybox skybox;
    
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
        
        // --- Create Cascaded Shadow Maps ---
        cascadeShadowMaps = new ShadowMap[cascadeCount];
        for (int i = 0; i < cascadeCount; i++) {
            // Each cascade may have a different resolution if desired.
            int resWidth = baseShadowMapWidth >> i;
            int resHeight = baseShadowMapHeight >> i;
            cascadeShadowMaps[i] = new ShadowMap(resWidth, resHeight);
        }
        
        // Allocate arrays for the matrices and splits.
        cascadeLightSpaceMatrices = new Matrix4f[cascadeCount];
        for (int i = 0; i < cascadeCount; i++) {
            cascadeLightSpaceMatrices[i] = new Matrix4f();
        }
        cascadeSplits = new float[cascadeCount];
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
        
        // --- Compute Cascade Splits (Linear split example) ---
        float camNear = mainCamera.near;
        float camFar = mainCamera.far;
        for (int i = 0; i < cascadeCount; i++) {
            float p = (i + 1) / (float)cascadeCount;
            cascadeSplits[i] = camNear + (camFar - camNear) * p;
        }
        
        // --- Compute the light view matrix ---
        // (Here we position the light far enough along its direction so the scene is in view.)
        Vector3f lightDir = new Vector3f(mainLight.gameObject.transform.front()).negate();
        Vector3f sceneCenter = new Vector3f(0, 0, 0); // Alternatively, compute the center of your scene.
        Vector3f lightPos = new Vector3f(sceneCenter).sub(new Vector3f(lightDir).mul(30f));
        Matrix4f lightView = new Matrix4f().lookAt(lightPos, sceneCenter, new Vector3f(0, 1, 0));
        
        // --- Compute the light-space matrix for each cascade based on the camera frustum slice ---
        for (int cascade = 0; cascade < cascadeCount; cascade++) {
            float splitNear = (cascade == 0) ? camNear : cascadeSplits[cascade - 1];
            float splitFar = cascadeSplits[cascade];
            
            // Get the eight corners of the cascade's frustum slice in world space.
            Vector3f[] frustumCornersWS = calculateFrustumCorners(mainCamera, splitNear, splitFar);
            
            // Transform the frustum corners into light space.
            Vector3f minLS = new Vector3f(Float.POSITIVE_INFINITY);
            Vector3f maxLS = new Vector3f(Float.NEGATIVE_INFINITY);
            for (Vector3f corner : frustumCornersWS) {
                Vector4f cornerLS = new Vector4f(corner, 1.0f);
                lightView.transform(cornerLS);
                // Update bounds.
                minLS.x = Math.min(minLS.x, cornerLS.x);
                minLS.y = Math.min(minLS.y, cornerLS.y);
                minLS.z = Math.min(minLS.z, cornerLS.z);
                
                maxLS.x = Math.max(maxLS.x, cornerLS.x);
                maxLS.y = Math.max(maxLS.y, cornerLS.y);
                maxLS.z = Math.max(maxLS.z, cornerLS.z);
                
            }
            
            // Optional: Extend the Z range to encompass more of the scene (to avoid clipping shadows).
            float zMult = 10.0f;
            if (minLS.z < 0) {
                minLS.z *= zMult;
            } else {
                minLS.z /= zMult;
            }
            if (maxLS.z < 0) {
                maxLS.z /= zMult;
            } else {
                maxLS.z *= zMult;
            }
            
            // Create an orthographic projection that bounds the frustum slice in light space.
            Matrix4f lightProjection = new Matrix4f().ortho(minLS.x, maxLS.x, minLS.y, maxLS.y, -maxLS.z, -minLS.z);
            cascadeLightSpaceMatrices[cascade].set(lightProjection).mul(lightView);
        }
        
        // --- Render Shadow Maps ---
        renderCascadedShadowPass(activeScene);
        
        // --- Render the Main Scene ---
        renderScene(activeScene, mainCamera);
    }
    
    // Renders each cascade by drawing the scene from the light's perspective.
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
        
        // Bind cascaded shadow maps.
        for (int i = 0; i < cascadeCount; i++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE1 + i);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, cascadeShadowMaps[i].depthMap);
            shaderProgram.setUniform("shadowMaps[" + i + "]", 1 + i);
            shaderProgram.setUniformMat4("lightSpaceMatrices[" + i + "]", cascadeLightSpaceMatrices[i]);
            shaderProgram.setUniform("cascadeSplits[" + i + "]", cascadeSplits[i]);
        }
        
        // Bind and set the skybox if available.
        skybox = GameObject.getGameObjectWithComponent(Skybox.class).getComponent(Skybox.class);
        if (skybox != null && skybox.getCubeMap() != null) {
            skybox.getCubeMap().bind(GL13.GL_TEXTURE5);
            shaderProgram.setUniform("skyboxAmbient.cubemap", 5);
            shaderProgram.setUniform("skyboxAmbient.strength", 1);
        }
        
        // Render all objects.
        if (activeScene.rootGameObject != null) {
            renderRecursive(activeScene.rootGameObject);
        }
    }
    
    // Helper: Recursively render objects for the shadow pass.
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
    
    // Computes the eight world-space corners of the camera frustum between near and far distances.
    private static Vector3f[] calculateFrustumCorners(Camera camera, float nearPlane, float farPlane) {
        Vector3f[] corners = new Vector3f[8];
        // First, get the inverse of (projection * view) matrix.
        Matrix4f proj = camera.isOrthographic ?
                new Matrix4f().ortho(-camera.size * camera.aspectRatio, camera.size * camera.aspectRatio, -camera.size, camera.size, camera.near, camera.far)
                : new Matrix4f().perspective((float)Math.toRadians(camera.fov), camera.aspectRatio, camera.near, camera.far);
        Matrix4f inv = new Matrix4f();
        proj.mul(camera.viewMatrix, inv).invert();
        
        // Define clip-space corners.
        // In clip space, x and y go from -1 to 1. z is -1 at the near plane and 1 at the far plane.
        float[] clipZ = new float[]{ -1.0f, 1.0f };
        int index = 0;
        for (int x = -1; x <= 1; x += 2) {
            for (int y = -1; y <= 1; y += 2) {
                for (int z = 0; z < 2; z++) {
                    // Start with clip-space position.
                    Vector4f clipPos = new Vector4f(x, y, clipZ[z], 1.0f);
                    
                    // Adjust the z component to lie within our cascade range.
                    // We remap from [-1,1] to [nearPlane, farPlane].
                    float depth = (z == 0) ? nearPlane : farPlane;
                    // Compute the corresponding normalized device coordinate z.
                    // This requires inverting the projection's depth transform.
                    // For simplicity we assume a linear depth distribution here.
                    clipPos.z = (depth - camera.near) / (camera.far - camera.near) * 2.0f - 1.0f;
                    
                    // Transform to world space.
                    Vector4f worldPos = inv.transform(new Vector4f(clipPos));
                    worldPos.div(worldPos.w);
                    corners[index++] = new Vector3f(worldPos.x, worldPos.y, worldPos.z);
                }
            }
        }
        return corners;
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
        shadowShader.cleanup();
    }
}
