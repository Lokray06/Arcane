package engine.components;

import engine.utils.Meshes;
import engine.Component;
import engine.CubeMapTexture;
import engine.Mesh;
import engine.utils.ShaderProgram;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP;

/**
 * The {@code Skybox} component represents a skybox in the scene.
 * <p>
 * It contains a cubemap texture and provides a method to render the skybox.
 * The view matrix is modified to remove translation so that the skybox appears infinitely distant.
 * </p>
 */
public class Skybox extends Component {
    /** The cubemap texture used for the skybox. */
    private CubeMapTexture cubeMap;
    /** The ambient color applied to the skybox lighting. */
    private Vector3f ambientColor = new Vector3f(1);
    
    /**
     * Constructs a Skybox component with the specified cubemap texture.
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
        skyboxShader.setUniform("skybox", 0);
        
        // Render a cube that will be drawn as the background.
        Mesh cube = Meshes.createSkybox();
        cube.render();
    }
}
