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

public class Skybox extends Component {
    private CubeMapTexture cubeMap;
    // This ambient color will be used to light the scene.
    private Vector3f ambientColor;
    
    /**
     * Expects an array of 6 image file paths in the order:
     * right, left, top, bottom, front, back.
     */
    public Skybox(CubeMapTexture cubeMap) {
        this.cubeMap = cubeMap;
        ambientColor = new Vector3f(0.2f, 0.2f, 0.2f); // Default ambient light
    }
    
    public CubeMapTexture getCubeMap() {
        return cubeMap;
    }
    
    public Vector3f getAmbientColor() {
        return ambientColor;
    }
    
    public void setAmbientColor(Vector3f ambientColor) {
        this.ambientColor.set(ambientColor);
    }
    
    /**
     * Renders the skybox. The view matrix should have its translation removed.
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