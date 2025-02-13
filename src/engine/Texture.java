package engine;

import engine.utils.FileUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;

public class Texture extends Component
{
    
    public static enum Type
    {
        ALBEDO, NORMAL, ROUGHNESS
    }
    
    private int textureID = 0; // Initialize to prevent errors
    public String path;
    private Type type; // Used when no path is provided
    
    /**
     * Constructs a texture that will be loaded from a file.
     */
    public Texture(String path)
    {
        this.path = path;
    }
    
    /**
     * Constructs a default texture of a given type.
     * No GL calls are made here.
     */
    public Texture(Type type)
    {
        this.type = type;
    }
    
    @Override
    public void start()
    {
        if (path != null)
        {
            loadTexture(path);
        }
        else
        {
            createDefaultTexture(type);
        }
    }
    
    private void loadTexture(String path)
    {
        try (var stack = stackPush())
        {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            
            STBImage.stbi_set_flip_vertically_on_load(true);
            ByteBuffer image = STBImage.stbi_load(path, width, height, channels, 4);
            
            if (image == null)
            {
                throw new RuntimeException("Failed to load texture: " + path);
            }
            
            textureID = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
            
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
            
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width.get(), height.get(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, image);
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
            
            STBImage.stbi_image_free(image);
            
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0); // Unbind texture after loading
        }
    }
    
    private void createDefaultTexture(Type type)
    {
        textureID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        ByteBuffer pixel = BufferUtils.createByteBuffer(4);
        
        switch (type)
        {
            case ALBEDO ->
            {
                // White texture: (255, 255, 255, 255)
                pixel.put((byte) 255).put((byte) 255).put((byte) 255).put((byte) 255);
            }
            case NORMAL ->
            {
                // Default normal: (128, 128, 255, 255) for a flat surface in tangent space.
                pixel.put((byte) 128).put((byte) 128).put((byte) 255).put((byte) 255);
            }
            case ROUGHNESS ->
            {
                // Mid-gray roughness: (128, 128, 128, 255)
                pixel.put((byte) 128).put((byte) 128).put((byte) 128).put((byte) 255);
            }
        }
        pixel.flip(); // Ensure buffer is ready to be used by OpenGL
        
        // Create 1x1 texture with the given pixel
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 1, 1, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0); // Unbind texture
    }
    
    public void bind(int unit)
    {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit); // Activate texture slot
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
    }
    
    public void delete()
    {
        if (textureID != 0) // Avoid deleting an uninitialized texture
        {
            GL11.glDeleteTextures(textureID);
            textureID = 0; // Reset to avoid double deletion
        }
    }
    
    public int getID()
    {
        return textureID;
    }
    
    @Override
    public String toString()
    {
        if(path == null)
            return "Default " + type.name() + " texture";
        else
            return path;
    }
}
