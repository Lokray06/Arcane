package engine;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class CubeMapTexture {
    private int textureID = 0; // 0 means not yet initialized
    private final String texturePath;
    private boolean loaded = false; // Indicates if the texture has been loaded
    
    public CubeMapTexture(String texturePath) {
        this.texturePath = texturePath;
    }
    
    private void loadCubeMap() {
        if (loaded) return; // Prevent reloading
        
        textureID = GL11.glGenTextures();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, textureID);
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            
            // Load the full texture
            ByteBuffer image = STBImage.stbi_load(texturePath, width, height, channels, 4);
            if (image == null) {
                throw new RuntimeException("Failed to load cubemap texture: " + texturePath);
            }
            
            int imgWidth = width.get(0);
            int imgHeight = height.get(0);
            int faceSize = imgWidth / 4; // Assuming the UV layout is a 4x3 grid
            
            // Define face positions in the UV map
            int[][] faceCoords = {
                    {2, 1}, // Right
                    {0, 1}, // Left
                    {1, 0}, // Top
                    {1, 2}, // Bottom
                    {1, 1}, // Front
                    {3, 1}  // Back
            };
            
            for (int i = 0; i < 6; i++) {
                int xOffset = faceCoords[i][0] * faceSize;
                int yOffset = faceCoords[i][1] * faceSize;
                
                ByteBuffer faceData = extractFace(image, imgWidth, imgHeight, xOffset, yOffset, faceSize);
                
                GL11.glTexImage2D(
                        GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i,
                        0,
                        GL11.GL_RGBA,
                        faceSize,
                        faceSize,
                        0,
                        GL11.GL_RGBA,
                        GL11.GL_UNSIGNED_BYTE,
                        faceData
                );
            }
            
            STBImage.stbi_image_free(image);
        }
        
        // Set texture parameters
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL12.GL_TEXTURE_WRAP_R, GL12.GL_CLAMP_TO_EDGE);
        
        GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, 0);
        loaded = true;
    }
    
    private ByteBuffer extractFace(ByteBuffer image, int imgWidth, int imgHeight, int xOffset, int yOffset, int faceSize) {
        int bytesPerPixel = 4; // RGBA
        ByteBuffer faceData = ByteBuffer.allocateDirect(faceSize * faceSize * bytesPerPixel);
        
        for (int y = 0; y < faceSize; y++) {
            int srcOffset = ((yOffset + y) * imgWidth + xOffset) * bytesPerPixel;
            int destOffset = y * faceSize * bytesPerPixel;
            for (int x = 0; x < faceSize * bytesPerPixel; x++) {
                faceData.put(destOffset + x, image.get(srcOffset + x));
            }
        }
        return faceData;
    }
    
    private void ensureLoaded() {
        if (!loaded) {
            loadCubeMap();
        }
    }
    
    public void bind(int unit) {
        ensureLoaded();
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, textureID);
    }
    
    public int getID() {
        ensureLoaded();
        return textureID;
    }
    
    public void delete() {
        if (textureID != 0) {
            GL11.glDeleteTextures(textureID);
            textureID = 0;
            loaded = false;
        }
    }
}
