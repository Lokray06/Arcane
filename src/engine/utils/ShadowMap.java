package engine.utils;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL14;

import java.nio.FloatBuffer;

public class ShadowMap {
    public int depthMapFBO;
    public int depthMap;
    public int width, height;
    
    public ShadowMap(int width, int height) {
        this.width = width;
        this.height = height;
        
        // Generate framebuffer object.
        depthMapFBO = GL30.glGenFramebuffers();
        
        // Create depth texture.
        depthMap = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthMap);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT16,
                          width, height, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (java.nio.ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        
        // Clamp to border to avoid shadow artifacts.
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_BORDER);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_BORDER);
        FloatBuffer borderColor = BufferUtils.createFloatBuffer(4).put(new float[]{1.0f, 1.0f, 1.0f, 1.0f});
        borderColor.flip();
        GL11.glTexParameterfv(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_BORDER_COLOR, borderColor);
        
        // Attach the depth texture as the FBO's depth buffer.
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, depthMapFBO);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
                                    GL11.GL_TEXTURE_2D, depthMap, 0);
        // No color output in the shadow map.
        GL11.glDrawBuffer(GL11.GL_NONE);
        GL11.glReadBuffer(GL11.GL_NONE);
        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("ERROR: Shadow Map FBO is not complete!");
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }
}
