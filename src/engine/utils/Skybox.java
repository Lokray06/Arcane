package engine.utils;

import engine.Component;
import engine.CubeMapTexture;
import engine.Engine;
import engine.Mesh;
import engine.utils.ShaderProgram;
import engine.utils.Meshes;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

// Import OpenGL functions (assuming LWJGL)
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * The {@code Skybox} component represents a skybox in the scene.
 * <p>
 * It contains a cubemap texture, an irradiance map, and a prefiltered environment map.
 * The view matrix is modified to remove translation so that the skybox appears infinitely distant.
 * </p>
 */
public class Skybox extends Component {
    /**
     * The cubemap texture used for the skybox.
     */
    private CubeMapTexture cubeMap;
    
    /**
     * The irradiance map for diffuse IBL.
     * Will be generated lazily when first requested.
     */
    private CubeMapTexture irradianceMap = null;
    
    /**
     * The prefiltered environment map for specular IBL.
     * Will be generated lazily when first requested.
     */
    private CubeMapTexture prefilteredMap = null;
    
    /**
     * The ambient color applied to the skybox lighting.
     */
    private Vector3f ambientColor = new Vector3f(1);
    
    /**
     * Constructs a Skybox component with the specified cubemap texture.
     * <p>
     * Note: The irradiance and prefiltered maps are not generated here,
     * but rather lazily when first accessed.
     * </p>
     *
     * @param cubeMap the cubemap texture.
     */
    public Skybox(CubeMapTexture cubeMap) {
        this.cubeMap = cubeMap;
        ambientColor = new Vector3f(0.2f, 0.2f, 0.2f); // Default ambient light.
    }
    
    /**
     * Returns the cubemap texture.
     *
     * @return the cubemap texture.
     */
    public CubeMapTexture getCubeMap() {
        return cubeMap;
    }
    
    /**
     * Returns the irradiance map.
     * If not yet generated, it is created using the provided cubemap texture.
     *
     * @return the irradiance map.
     */
    public CubeMapTexture getIrradianceMap() {
        if (irradianceMap == null) {
            irradianceMap = generateIrradianceMap(cubeMap);
        }
        return irradianceMap;
    }
    
    /**
     * Returns the prefiltered environment map.
     * If not yet generated, it is created using the provided cubemap texture.
     *
     * @return the prefiltered environment map.
     */
    public CubeMapTexture getPrefilteredMap() {
        if (prefilteredMap == null) {
            prefilteredMap = generatePrefilteredMap(cubeMap);
        }
        return prefilteredMap;
    }
    
    /**
     * Returns the ambient color.
     *
     * @return the ambient color.
     */
    public Vector3f getAmbientColor() {
        return ambientColor;
    }
    
    /**
     * Sets the ambient color.
     *
     * @param ambientColor the new ambient color.
     */
    public void setAmbientColor(Vector3f ambientColor) {
        this.ambientColor.set(ambientColor);
    }
    
    /**
     * Renders the skybox.
     * <p>
     * The view matrix is modified to remove translation before passing to the shader.
     * The cubemap is bound and a cube mesh is rendered to display the skybox.
     * </p>
     *
     * @param skyboxShader the shader program used for the skybox.
     * @param view         the view matrix (with translation removed).
     * @param projection   the projection matrix.
     */
    public void render(ShaderProgram skyboxShader, Matrix4f view, Matrix4f projection) {
        // Remove translation from the view matrix.
        Matrix4f viewNoTranslation = new Matrix4f(view);
        viewNoTranslation.m30(0).m31(0).m32(0);
        
        skyboxShader.use();
        skyboxShader.setUniformMat4("view", viewNoTranslation);
        skyboxShader.setUniformMat4("projection", projection);
        
        // Bind the cubemap to texture unit 0.
        cubeMap.bind(0);
        
        //Debug cubemaps
        //prefilteredMap.bind(0);
        //irradianceMap.bind(0);
        
        skyboxShader.setUniform("skybox", 0);
        
        // Lazily generate and bind the IBL maps.
        CubeMapTexture irrMap = getIrradianceMap();
        if (irrMap != null) {
            irrMap.bind(1);
            skyboxShader.setUniform("irradianceMap", 1);
        }
        CubeMapTexture prefilterMap = getPrefilteredMap();
        if (prefilterMap != null) {
            prefilterMap.bind(2);
            skyboxShader.setUniform("prefilterMap", 2);
        }
        
        // Render a cube that will be drawn as the background.
        Mesh cube = Meshes.createSkybox();
        cube.render();
    }
    
    /**
     * Generates an irradiance cubemap from the provided environment cubemap.
     * This method sets up a framebuffer and renders the environment using an irradiance convolution shader.
     *
     * @param environmentMap the original environment cubemap.
     * @return the generated irradiance cubemap.
     */
    public static CubeMapTexture generateIrradianceMap(CubeMapTexture environmentMap) {
        // Define resolution for irradiance map.
        int resolution = 32;
        // Determine the texture format based on the source.
        boolean isHDR = environmentMap.isHDR();
        int internalFormat = isHDR ? GL30.GL_RGBA16F : GL11.GL_RGBA;
        int format = GL11.GL_RGBA;
        int type = isHDR ? GL11.GL_FLOAT : GL11.GL_UNSIGNED_BYTE;
        
        // Create a new cubemap texture with the same structure as the environment map.
        CubeMapTexture irradianceCube = new CubeMapTexture(resolution, resolution, internalFormat, format, type);
        
        // Create and bind framebuffer and renderbuffer.
        int captureFBO = glGenFramebuffers();
        int captureRBO = glGenRenderbuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, captureFBO);
        glViewport(0, 0, resolution, resolution);
        glBindRenderbuffer(GL_RENDERBUFFER, captureRBO);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, resolution, resolution);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, captureRBO);
        
        // Load the irradiance convolution shader.
        ShaderProgram irradianceShader = new ShaderProgram(
                FileUtils.loadFileAsString(Engine.shadersPath.concat("irradiance.vert")),
                FileUtils.loadFileAsString(Engine.shadersPath.concat("irradiance.frag"))
        );
        irradianceShader.use();
        irradianceShader.setUniform("environmentMap", 0);
        
        // Set up capture projection and views.
        Matrix4f captureProjection = new Matrix4f().perspective((float) Math.toRadians(90.0f), 1.0f, 0.1f, 10.0f);
        Matrix4f[] captureViews = new Matrix4f[]{
                new Matrix4f().lookAt(new Vector3f(0, 0, 0), new Vector3f( 1,  0,  0), new Vector3f(0, -1,  0)),
                new Matrix4f().lookAt(new Vector3f(0, 0, 0), new Vector3f(-1,  0,  0), new Vector3f(0, -1,  0)),
                new Matrix4f().lookAt(new Vector3f(0, 0, 0), new Vector3f( 0,  1,  0), new Vector3f(0,  0,  1)),
                new Matrix4f().lookAt(new Vector3f(0, 0, 0), new Vector3f( 0, -1,  0), new Vector3f(0,  0, -1)),
                new Matrix4f().lookAt(new Vector3f(0, 0, 0), new Vector3f( 0,  0,  1), new Vector3f(0, -1,  0)),
                new Matrix4f().lookAt(new Vector3f(0, 0, 0), new Vector3f( 0,  0, -1), new Vector3f(0, -1,  0))
        };
        
        // Bind the source environment cubemap.
        environmentMap.bind(0);
        
        // Render to each face.
        for (int i = 0; i < 6; ++i) {
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                                   GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, irradianceCube.getId(), 0);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            
            irradianceShader.setUniformMat4("view", captureViews[i]);
            irradianceShader.setUniformMat4("projection", captureProjection);
            
            Mesh cube = Meshes.createSkybox();
            cube.render();
        }
        
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        return irradianceCube;
    }
    
    
    /**
     * Generates a prefiltered (specular) cubemap from the provided environment cubemap.
     * This method renders the environment at multiple mip levels using a prefilter shader.
     *
     * @param environmentMap the original environment cubemap.
     * @return the generated prefiltered cubemap.
     */
    public static CubeMapTexture generatePrefilteredMap(CubeMapTexture environmentMap) {
        // Define base resolution for the prefiltered map.
        int baseResolution = 128;
        boolean isHDR = environmentMap.isHDR();
        int internalFormat = isHDR ? GL30.GL_RGBA16F : GL11.GL_RGBA;
        int format = GL11.GL_RGBA;
        int type = isHDR ? GL11.GL_FLOAT : GL11.GL_UNSIGNED_BYTE;
        
        // Create the cubemap texture with mipmaps enabled.
        CubeMapTexture prefilteredCube = new CubeMapTexture(baseResolution, baseResolution, internalFormat, format, type);
        prefilteredCube.enableMipmaps();
        
        int captureFBO = glGenFramebuffers();
        int captureRBO = glGenRenderbuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, captureFBO);
        
        // Load the prefilter shader.
        ShaderProgram prefilterShader = new ShaderProgram(
                FileUtils.loadFileAsString(Engine.shadersPath.concat("prefilter.vert")),
                FileUtils.loadFileAsString(Engine.shadersPath.concat("prefilter.frag"))
        );
        prefilterShader.use();
        prefilterShader.setUniform("environmentMap", 0);
        
        Matrix4f captureProjection = new Matrix4f().perspective((float) Math.toRadians(90.0f), 1.0f, 0.1f, 10.0f);
        Matrix4f[] captureViews = new Matrix4f[]{
                new Matrix4f().lookAt(new Vector3f(0, 0, 0), new Vector3f( 1,  0,  0), new Vector3f(0, -1,  0)),
                new Matrix4f().lookAt(new Vector3f(0, 0, 0), new Vector3f(-1,  0,  0), new Vector3f(0, -1,  0)),
                new Matrix4f().lookAt(new Vector3f(0, 0, 0), new Vector3f( 0,  1,  0), new Vector3f(0,  0,  1)),
                new Matrix4f().lookAt(new Vector3f(0, 0, 0), new Vector3f( 0, -1,  0), new Vector3f(0,  0, -1)),
                new Matrix4f().lookAt(new Vector3f(0, 0, 0), new Vector3f( 0,  0,  1), new Vector3f(0, -1,  0)),
                new Matrix4f().lookAt(new Vector3f(0, 0, 0), new Vector3f( 0,  0, -1), new Vector3f(0, -1,  0))
        };
        
        // Bind the source environment cubemap.
        environmentMap.bind(0);
        
        int maxMipLevels = 5;
        for (int mip = 0; mip < maxMipLevels; ++mip) {
            int mipWidth = baseResolution >> mip;
            int mipHeight = baseResolution >> mip;
            
            glBindRenderbuffer(GL_RENDERBUFFER, captureRBO);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, mipWidth, mipHeight);
            glViewport(0, 0, mipWidth, mipHeight);
            
            float roughness = (float) mip / (maxMipLevels - 1);
            prefilterShader.setUniform("roughness", roughness);
            
            for (int i = 0; i < 6; ++i) {
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                                       GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, prefilteredCube.getId(), mip);
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                
                prefilterShader.setUniformMat4("view", captureViews[i]);
                prefilterShader.setUniformMat4("projection", captureProjection);
                
                Mesh cube = Meshes.createSkybox();
                cube.render();
            }
        }
        
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        return prefilteredCube;
    }
    
}
