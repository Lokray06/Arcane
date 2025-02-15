package engine.utils.debug;

import engine.Engine;
import engine.utils.FileUtils;
import engine.utils.ShaderProgram;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class DebugRenderer {
    private static ShaderProgram debugShader;
    private static DebugQuad debugQuad;
    
    public static void init() {
        String vertexSrc = FileUtils.loadFileAsString(Engine.shadersPath.concat("debugQuadVertex.glsl"));
        String fragmentSrc = FileUtils.loadFileAsString(Engine.shadersPath.concat("debugQuadFragment.glsl"));
        debugShader = new ShaderProgram(vertexSrc, fragmentSrc);
        debugQuad = new DebugQuad();
    }
    
    public static void render(int textureID) {
        // Disable depth test so the quad is drawn on top.
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        
        debugShader.use();
        // Bind the texture (shadow map) to texture unit 0.
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        debugShader.setUniform("debugTexture", 0);
        
        debugQuad.render();
        debugShader.detach();
        
        // Re-enable depth testing for subsequent rendering.
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }
    
    public static void cleanup() {
        debugShader.cleanup();
    }
}
