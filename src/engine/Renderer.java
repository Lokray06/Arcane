package engine;

import engine.components.*;
import engine.utils.FileUtils;
import engine.utils.ShaderProgram;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL43.*;

public class Renderer {
    // Main shader program used for scene rendering.
    private static ShaderProgram shaderProgram;
    private static final String MODEL_UNIFORM = "model";
    private static final String VIEW_UNIFORM = "view";
    private static final String PROJECTION_UNIFORM = "projection";
    
    private static ShaderProgram skyboxShader;
    
    // Cascaded shadow parameters for directional light.
    public static int cascadeCount = 4;
    public static int baseShadowMapWidth = 2048;
    public static int baseShadowMapHeight = 2048;
    
    // --- Shadow Map Variables for directional light ---
    private static int shadowMapFBO;
    private static int shadowMap;
    private static ShaderProgram depthShader;
    
    // --- New: Shader program for point light shadow mapping ---
    private static ShaderProgram pointDepthShader;
    // Resolution for point light shadow maps:
    private static final int pointShadowMapWidth = 1024;
    private static final int pointShadowMapHeight = 1024;
    
    // These maps keep track of (point light → its shadow framebuffer and cube map)
    private static final Map<LightPoint, Integer> pointLightShadowFBO = new HashMap<>();
    private static final Map<LightPoint, Integer> pointLightShadowCube = new HashMap<>();
    
    private static Skybox skybox;
    
    /**
     * Initializes the renderer by loading shader programs and setting up
     * the shadow map framebuffer and texture.
     */
    public static void init() {
        enableOpenGLDebugging();
        
        shaderProgram = new ShaderProgram(
                FileUtils.loadFileAsString(Engine.shadersPath.concat("vertex.glsl")),
                FileUtils.loadFileAsString(Engine.shadersPath.concat("fragment.glsl"))
        );

        skyboxShader = new ShaderProgram(
                FileUtils.loadFileAsString(Engine.shadersPath.concat("skyboxVertex.glsl")),
                FileUtils.loadFileAsString(Engine.shadersPath.concat("skyboxFragment.glsl"))
        );

        depthShader = new ShaderProgram(
                FileUtils.loadFileAsString(Engine.shadersPath.concat("depthVertex.glsl")),
                FileUtils.loadFileAsString(Engine.shadersPath.concat("depthFragment.glsl"))
        );
        
        pointDepthShader = new ShaderProgram(
                FileUtils.loadFileAsString(Engine.shadersPath.concat("pointDepthVertex.glsl")),
                FileUtils.loadFileAsString(Engine.shadersPath.concat("pointDepthGeometry.glsl")),
                FileUtils.loadFileAsString(Engine.shadersPath.concat("pointDepthFragment.glsl"))
        );

        String[] shaders = new String[8];
        for(int i = 0; i < 8; i++)
        {

        }
        System.out.println();
        
        shadowMapFBO = GL30.glGenFramebuffers();
        shadowMap = GL11.glGenTextures();
        glBindTexture(GL_TEXTURE_2D, shadowMap);
        GL11.glTexImage2D(GL_TEXTURE_2D, 0, GL11.GL_DEPTH_COMPONENT,
                          baseShadowMapWidth, baseShadowMapHeight, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (ByteBuffer) null);
        GL11.glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_BORDER);
        GL11.glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_BORDER);
        FloatBuffer borderColor = BufferUtils.createFloatBuffer(4).put(new float[]{1f, 1f, 1f, 1f});
        borderColor.flip();
        GL11.glTexParameterfv(GL_TEXTURE_2D, GL11.GL_TEXTURE_BORDER_COLOR, borderColor);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, shadowMapFBO);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, shadowMap, 0);
        GL11.glDrawBuffer(GL11.GL_NONE);
        GL11.glReadBuffer(GL11.GL_NONE);
        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("Directional shadow map framebuffer not complete!");
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }
    
    /**
     * Renders the active scene from the perspective of the active camera.
     */
    public static void render(Scene activeScene) {
        Camera mainCamera = getActiveCamera(activeScene);
        if (mainCamera == null) {
            System.err.println("No active camera to render");
            return;
        }
        
        LightDirectional mainDirectionalLight = getMainDirectionalLight(activeScene);
        boolean hasDirectionalLight = (mainDirectionalLight != null);
        Matrix4f lightSpaceMatrix = new Matrix4f();
        
        // -------- 1. Directional Light Shadow Map Pass --------
        if (hasDirectionalLight) {
            Vector3f lightDir = new Vector3f(mainDirectionalLight.gameObject.transform.front()).negate();
            Vector3f sceneCenter = new Vector3f(0, 0, 0); // Could be computed from scene bounds.
            Vector3f lightPos = new Vector3f(sceneCenter).sub(new Vector3f(lightDir).mul(30f));
            // Directional light (left-handed)
            Matrix4f lightView = new Matrix4f().lookAtLH(lightPos, sceneCenter, new Vector3f(0, 1, 0));
            Matrix4f lightProjection = new Matrix4f().orthoLH(-20, 20, -20, 20, 1, 100);
            
            lightProjection.mul(lightView, lightSpaceMatrix);
            
            // Render shadow map from directional light's view.
            GL11.glViewport(0, 0, baseShadowMapWidth, baseShadowMapHeight);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, shadowMapFBO);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            depthShader.use();
            depthShader.setUniformMat4("lightSpaceMatrix", lightSpaceMatrix);
            renderSceneForShadows(activeScene, depthShader);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        } else {
            System.err.println("No directional light available for shadows. Rendering without directional shadows.");
            lightSpaceMatrix.identity();
        }
        
        // -------- 2. Point Light Shadow Map Pass --------
        // Collect point lights from the scene.
        List<LightPoint> pointLights = new ArrayList<>();
        if (activeScene.rootGameObject != null) {
            collectPointLights(activeScene.rootGameObject, pointLights);
        }
        // For each point light, render its shadow cube map.
        for (LightPoint pointLight : pointLights) {
            // (For simplicity we assume every point light casts shadows.)
            // If this light does not yet have a shadow map allocated, create one:
            if (!pointLightShadowFBO.containsKey(pointLight)) {
                int fbo = GL30.glGenFramebuffers();
                int cubeMap = GL11.glGenTextures();
                glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, cubeMap);
                for (int i = 0; i < 6; i++) {
                    GL11.glTexImage2D(GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL11.GL_DEPTH_COMPONENT,
                                      pointShadowMapWidth, pointShadowMapHeight, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (ByteBuffer) null);
                }
                GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL12.GL_TEXTURE_WRAP_R, GL13.GL_CLAMP_TO_EDGE);
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
                GL33.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, cubeMap, 0);
                GL11.glDrawBuffer(GL11.GL_NONE);
                GL11.glReadBuffer(GL11.GL_NONE);
                if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
                    System.err.println("Point light shadow framebuffer not complete!");
                }
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
                pointLightShadowFBO.put(pointLight, fbo);
                pointLightShadowCube.put(pointLight, cubeMap);
            }
            int fbo = pointLightShadowFBO.get(pointLight);
            int cubeMap = pointLightShadowCube.get(pointLight);
            Vector3f lightPos = new Vector3f(pointLight.gameObject.transform.globalPosition);
            System.out.println("Light Position: " + lightPos);
            float farPlane = 100.0f; // You might let each point light specify its own far plane.
            
            // Example (Java): Build the six view–projection matrices using left-handed lookAtLH.
            Matrix4f shadowProj = new Matrix4f().perspective((float)Math.toRadians(90.0f), 1.0f, 1.0f, farPlane);
            Matrix4f[] shadowTransforms = new Matrix4f[6];
            
            shadowTransforms[0] = new Matrix4f().set(shadowProj)
                    .lookAtLH(lightPos, new Vector3f(lightPos).add( 1,  0,  0), new Vector3f(0,  1,  0)); // +X
            shadowTransforms[1] = new Matrix4f().set(shadowProj)
                    .lookAtLH(lightPos, new Vector3f(lightPos).add(-1,  0,  0), new Vector3f(0,  1,  0)); // -X
            shadowTransforms[2] = new Matrix4f().set(shadowProj)
                    .lookAtLH(lightPos, new Vector3f(lightPos).add( 0,  1,  0), new Vector3f(0,  0,  1)); // +Y
            shadowTransforms[3] = new Matrix4f().set(shadowProj)
                    .lookAtLH(lightPos, new Vector3f(lightPos).add( 0, -1,  0), new Vector3f(0,  0, -1)); // -Y
            shadowTransforms[4] = new Matrix4f().set(shadowProj)
                    .lookAtLH(lightPos, new Vector3f(lightPos).add( 0,  0,  1), new Vector3f(0,  1,  0)); // +Z
            shadowTransforms[5] = new Matrix4f().set(shadowProj)
                    .lookAtLH(lightPos, new Vector3f(lightPos).add( 0,  0, -1), new Vector3f(0,  1,  0)); // -Z
            
            
            
            
            
            // Render the scene to this point light's shadow cube map.
            GL11.glViewport(0, 0, pointShadowMapWidth, pointShadowMapHeight);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            pointDepthShader.use();
            // Set the 6 shadow matrices.
            for (int i = 0; i < 6; i++) {
                pointDepthShader.setUniformMat4("shadowMatrices[" + i + "]", shadowTransforms[i]);
            }
            pointDepthShader.setUniform("lightPos", lightPos);
            pointDepthShader.setUniform("farPlane", farPlane);
            renderSceneForShadows(activeScene, pointDepthShader);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        }
        
        // -------- 3. Main Scene Pass --------
        GL11.glViewport(0, 0, Engine.WINDOW_WIDTH, Engine.WINDOW_HEIGHT);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        
        shaderProgram.use();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            shaderProgram.setUniformMat4(PROJECTION_UNIFORM, getProjectionMatrix(mainCamera));
            shaderProgram.setUniformMat4(VIEW_UNIFORM, mainCamera.viewMatrix);
            shaderProgram.setUniform("viewPos", mainCamera.gameObject.transform.globalPosition);
        }
        shaderProgram.setUniformMat4("lightSpaceMatrix", lightSpaceMatrix);
        if (hasDirectionalLight) {
            bindTexture(6, GL_TEXTURE_2D, shadowMap, "Directional Shadow Map");
            shaderProgram.setUniform("shadowMap", 6);
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
        
        // Pass point light parameters (positions, colors, etc.).
        shaderProgram.setUniform("numPointLights", pointLights.size());
        for (int i = 0; i < pointLights.size() && i < maxLights; i++) {
            LightPoint pLight = pointLights.get(i);
            shaderProgram.setUniform("pointLights[" + i + "].position", pLight.gameObject.transform.globalPosition);
            shaderProgram.setUniform("pointLights[" + i + "].color", pLight.color);
            shaderProgram.setUniform("pointLights[" + i + "].strength", pLight.strength);
            shaderProgram.setUniform("pointLights[" + i + "].constant", pLight.constant);
            shaderProgram.setUniform("pointLights[" + i + "].linear", pLight.linear);
            shaderProgram.setUniform("pointLights[" + i + "].quadratic", pLight.quadratic);
        }
        // Bind each point light's shadow cube map.
        int pointShadowTexUnitStart = 7;
        for (int i = 0; i < pointLights.size() && i < maxLights; i++) {
            LightPoint pLight = pointLights.get(i);
            int cubeMap = pointLightShadowCube.getOrDefault(pLight, 0);
            int unit = pointShadowTexUnitStart + i;
            bindTexture(unit, GL_TEXTURE_CUBE_MAP, cubeMap, "Point Light Shadow Cube Map for Light " + i);
            shaderProgram.setUniform("pointShadowMaps[" + i + "]", unit);
            // Pass the far plane used in the point shadow pass:
            shaderProgram.setUniform("pointShadowFarPlanes[" + i + "]", 100.0f);
        }
        
        // Bind skybox if available.
        GameObject skyboxGO = GameObject.getGameObjectWithComponent(Skybox.class);
        if (skyboxGO != null) {
            skybox = skyboxGO.getComponent(Skybox.class);
        }
        if (skybox != null && skybox.getCubeMap() != null) {
            bindTexture(5, GL_TEXTURE_CUBE_MAP, skybox.getCubeMap().getID(), "Skybox Cube Map");
            shaderProgram.setUniform("skyboxAmbient.cubemap", 5);
            shaderProgram.setUniform("skyboxAmbient.strength", 1);
        } else {
            System.err.println("Couldn't load skybox");
        }
        
        // Render all objects.
        if (activeScene.rootGameObject != null) {
            renderRecursive(activeScene.rootGameObject);
        }
    }
    
    private static void renderSceneForShadows(Scene activeScene, ShaderProgram shader) {
        if (activeScene.rootGameObject != null) {
            renderForShadowsRecursive(activeScene.rootGameObject, shader);
        }
    }
    
    private static void renderForShadowsRecursive(GameObject gameObject, ShaderProgram shader) {
        MeshRenderer meshRenderer = gameObject.getComponent(MeshRenderer.class);
        if (meshRenderer != null && meshRenderer.mesh != null) {
            shader.use();
            Matrix4f modelMatrix = gameObject.transform.getModelMatrix();
            shader.setUniformMat4(MODEL_UNIFORM, modelMatrix);
            meshRenderer.mesh.render();
        }
        for (GameObject child : gameObject.children) {
            renderForShadowsRecursive(child, shader);
        }
    }
    
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
            
            // Bind material textures.
            bindTexture(0, GL_TEXTURE_2D, material.albedoMap.getID(), "Albedo Map");
            shaderProgram.setUniform("uAlbedo", 0);
            
            bindTexture(1, GL_TEXTURE_2D, material.normalMap.getID(), "Normal Map");
            shaderProgram.setUniform("uNormal", 1);
            
            bindTexture(2, GL_TEXTURE_2D, material.metallicMap.getID(), "Metallic Map");
            shaderProgram.setUniform("uMetallic", 2);
            
            bindTexture(3, GL_TEXTURE_2D, material.roughnessMap.getID(), "Roughness Map");
            shaderProgram.setUniform("uRoughness", 3);
            
            bindTexture(4, GL_TEXTURE_2D, material.aoMap.getID(), "AO Map");
            shaderProgram.setUniform("uAO", 4);
            
            shaderProgram.setUniform("uNormalMapStrength", material.normalMapStrength);
            shaderProgram.setUniform("uAlbedoColor", material.albedoColor);
            shaderProgram.setUniform("uMetallicScalar", material.metallic);
            shaderProgram.setUniform("uRoughnessScalar", material.roughness);
            
            meshRenderer.mesh.render();
            bindTexture(0, GL_TEXTURE_2D, 0, "Unbind Texture");
        }
        for (GameObject child : gameObject.children) {
            renderRecursive(child);
        }
        IntBuffer mapped = BufferUtils.createIntBuffer(1);
        glGetBufferParameteriv(GL_ELEMENT_ARRAY_BUFFER, GL_BUFFER_MAPPED, mapped);
        System.out.println("Mapped: " + mapped.get(0));
    }
    
    private static void collectDirectionalLights(GameObject gameObject, List<LightDirectional> lights) {
        LightDirectional light = gameObject.getComponent(LightDirectional.class);
        if (light != null) {
            lights.add(light);
        }
        for (GameObject child : gameObject.children) {
            collectDirectionalLights(child, lights);
        }
    }
    
    private static void collectPointLights(GameObject gameObject, List<LightPoint> lights) {
        LightPoint light = gameObject.getComponent(LightPoint.class);
        if (light != null) {
            lights.add(light);
        }
        for (GameObject child : gameObject.children) {
            collectPointLights(child, lights);
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
    
    public static void cleanup() {
        shaderProgram.cleanup();
        depthShader.cleanup();
        pointDepthShader.cleanup();
    }
    
    public static void debugTextureBinding(String type, int textureID) {
        System.out.println("[DEBUG] Binding Texture: Type = " + type + ", ID = " + textureID);
    }
    
    private static void enableOpenGLDebugging() {
        glEnable(GL_DEBUG_OUTPUT);
        glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);
        glDebugMessageCallback((source, type, id, severity, length, message, userParam) -> {
            String msg = GLDebugMessageCallback.getMessage(length, message);
            System.err.println("[OpenGL Debug] " + msg);
        }, 0);
        System.out.println("[INFO] OpenGL debugging enabled.");
    }
    
    public static void bindTexture(int textureUnit, int textureType, int textureID, String textureName) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(textureType, textureID);
        //System.out.println("[DEBUG] Bound " + textureName + " (ID: " + textureID + ") to Texture Unit " + textureUnit);
    }
}
