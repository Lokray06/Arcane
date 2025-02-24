package engine;

import org.joml.Vector3i;
import org.joml.Vector4i;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * The {@code Texture} class encapsulates an OpenGL texture.
 * <p>
 * A texture can be loaded from an image file, created from a default value based on a specified type,
 * or created from raw RGBA/RGB values.
 * </p>
 */
public class Texture {
    /**
     * Enum representing the different types of textures.
     */
    public enum Type {
        ALBEDO, NORMAL, ROUGHNESS, METALLIC, AO, HEIGHT
    }
    
    public boolean isLUT = false;
    
    /** The OpenGL texture ID (0 if not yet initialized). */
    private int textureID = 0;
    /** The file path for the texture (if loaded from file). */
    private String path;
    /** The type of the texture (if using a default texture). */
    private Type type;
    /** RGBA values for a 1x1 texture (if generated from raw values). */
    private Vector4i rgba;
    /** Flag indicating whether the texture has been loaded. */
    private boolean loaded = false;
    
    /**
     * Constructs a texture from the specified file path.
     *
     * @param path the file path of the texture.
     */
    public Texture(String path) {
        this.path = path;
    }
    
    public Texture(String path, boolean isLUT) {
        this.path = path;
        this.isLUT = isLUT;
    }
    
    /**
     * Constructs a default texture for the given type.
     *
     * @param type the texture type.
     */
    public Texture(Type type) {
        this.type = type;
    }
    
    /**
     * Constructs a texture from the specified RGBA values.
     *
     * @param rgba the RGBA values.
     */
    public Texture(Vector4i rgba) {
        this.rgba = rgba;
    }
    
    /**
     * Constructs a texture from the specified RGB values.
     *
     * @param rgb the RGB values.
     */
    public Texture(Vector3i rgb) {
        this.rgba = new Vector4i(rgb.x, rgb.y, rgb.z, 255);
    }
    
    /**
     * Creates a 1x1 texture with the specified RGBA color.
     *
     * @param rgba the color as a Vector4i.
     */
    private void createTexture(Vector4i rgba) {
        int r = rgba.x;
        int g = rgba.y;
        int b = rgba.z;
        int a = rgba.w;
        
        this.textureID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.textureID);
        
        // Create a ByteBuffer and fill it with the RGBA values.
        ByteBuffer pixel = BufferUtils.createByteBuffer(4);
        pixel.put((byte) r).put((byte) g).put((byte) b).put((byte) a);
        pixel.flip(); // Prepare buffer for reading.
        
        // Upload the texture data to OpenGL.
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 1, 1, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
        
        // Set texture parameters.
        glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        
        // Unbind the texture.
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        
        this.loaded = true;
    }
    
    /**
     * Helper method to create a texture from RGB values.
     *
     * @param rgb the RGB values.
     */
    public void createTexture(Vector3i rgb) {
        createTexture(new Vector4i(rgb.x, rgb.y, rgb.z, 255));
    }
    
    /**
     * Loads the texture from the specified file path.
     *
     * @param path the file path.
     */
    private void loadTexture(String path) {
        try (var stack = stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            
            STBImage.stbi_set_flip_vertically_on_load(false);
            ByteBuffer image = STBImage.stbi_load(path, width, height, channels, 4);
            
            if (image == null) {
                throw new RuntimeException("Failed to load texture: " + path);
            }
            
            textureID = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
            
            glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            
            if(!isLUT)
            {
                glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
                glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
            }
            else
            {
                glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            }
            
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width.get(), height.get(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, image);
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
            
            STBImage.stbi_image_free(image);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            
            loaded = true;
        }
    }
    
    /**
     * Creates a default texture based on the specified type.
     *
     * @param type the texture type.
     */
    private void createDefaultTexture(Type type) {
        textureID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        ByteBuffer pixel = BufferUtils.createByteBuffer(4);
        
        switch (type) {
            case ALBEDO -> pixel.put((byte) 255).put((byte) 255).put((byte) 255).put((byte) 255);
            case NORMAL -> pixel.put((byte) 128).put((byte) 128).put((byte) 255).put((byte) 255);
            case ROUGHNESS -> pixel.put((byte) 255).put((byte) 255).put((byte) 255).put((byte) 255);
            case METALLIC -> pixel.put((byte) 255).put((byte) 255).put((byte) 255).put((byte) 255);
            case AO -> pixel.put((byte) 255).put((byte) 255).put((byte) 255).put((byte) 255); // full ambient occlusion.
            case HEIGHT -> pixel.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 255); // full ambient occlusion.
        }
        pixel.flip();
        
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 1, 1, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
        glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        
        loaded = true;
    }
    
    /**
     * Ensures the texture is loaded.
     * <p>
     * If not already loaded, the texture is loaded from the file,
     * created as a default texture based on type, or generated from raw RGBA values.
     * </p>
     */
    public void ensureLoaded() {
        if (!loaded) {
            if (path != null) {
                loadTexture(path);
            } else if (type != null) {
                createDefaultTexture(type);
            } else if (rgba != null) {
                createTexture(rgba);
                loaded = true;
            }
        }
    }
    
    /**
     * Binds the texture to the specified texture unit.
     *
     * @param unit the texture unit to bind to.
     */
    public void bind(int unit) {
        ensureLoaded(); // Ensure the texture is loaded before binding.
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
    }
    
    /**
     * Deletes the texture, releasing its OpenGL resources.
     */
    public void delete() {
        if (textureID != 0) {
            GL11.glDeleteTextures(textureID);
            textureID = 0;
            loaded = false;
        }
    }
    
    /**
     * Returns the OpenGL texture ID.
     *
     * @return the texture ID.
     */
    public int getID() {
        ensureLoaded(); // Ensure texture is loaded before retrieving its ID.
        return textureID;
    }
    
    /**
     * Returns a string representation of the texture.
     *
     * @return a string describing the texture.
     */
    @Override
    public String toString() {
        if(path != null)
            return path;
        else if (type != null)
            return "Default " + type + " texture";
        else if(rgba != null)
            return "" + rgba;
        return "Something is wrong with the texture";
    }
}
