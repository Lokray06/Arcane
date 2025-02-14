package engine;

import org.joml.Vector3d;
import org.joml.Vector3i;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;

public class Texture {

    public enum Type {
        ALBEDO, NORMAL, ROUGHNESS
    }

    private int textureID = 0; // 0 means not yet initialized
    private String path;
    private Type type;
    private Vector3i rgb;
    private boolean loaded = false; // Flag to track if the texture has been loaded

    public Texture(String path) {
        this.path = path;
    }

    public Texture(Type type) {
        this.type = type;
    }

    public Texture(Vector3i rgb)
    {
        this.rgb = rgb;
    }

    private void createTexture(Vector3i rgb)
    {
        int r = rgb.x;
        int g = rgb.y;
        int b = rgb.z;
        int a = 255; // Full opacity

        this.textureID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.textureID);

        // Create a ByteBuffer and fill it with the RGBA values
        ByteBuffer pixel = BufferUtils.createByteBuffer(4);
        pixel.put((byte) r).put((byte) g).put((byte) b).put((byte) a);
        pixel.flip(); // Make sure the buffer is ready to be read

        // Upload the texture data to OpenGL
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 1, 1, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);

        // Set the texture parameters
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        // Unbind the texture
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        this.loaded = true;
    }


    private void loadTexture(String path) {
        System.out.println("Loading texture: " + path);
        try (var stack = stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            STBImage.stbi_set_flip_vertically_on_load(true);
            ByteBuffer image = STBImage.stbi_load(path, width, height, channels, 4);

            if (image == null) {
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
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

            loaded = true;
        }
    }

    private void createDefaultTexture(Type type) {
        System.out.println("Creating default texture: " + type);
        textureID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        ByteBuffer pixel = BufferUtils.createByteBuffer(4);

        switch (type) {
            case ALBEDO -> pixel.put((byte) 255).put((byte) 255).put((byte) 255).put((byte) 255);
            case NORMAL -> pixel.put((byte) 128).put((byte) 128).put((byte) 255).put((byte) 255);
            case ROUGHNESS -> pixel.put((byte) 128).put((byte) 128).put((byte) 128).put((byte) 255);
        }
        pixel.flip();

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 1, 1, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        loaded = true;
    }

    public void ensureLoaded() {
        if (!loaded) {
            if (path != null) {
                loadTexture(path);
            } else if (type != null) {
                createDefaultTexture(type);
            } else if (rgb != null) {
                createTexture(rgb); // This line might not be executing properly
                loaded = true;
            }
        }
    }


    public void bind(int unit)
    {
        ensureLoaded(); // Ensure the texture is loaded before binding
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
    }

    public void delete() {
        if (textureID != 0) {
            GL11.glDeleteTextures(textureID);
            textureID = 0;
            loaded = false;
        }
    }

    public int getID() {
        ensureLoaded(); // Ensure texture is loaded before retrieving ID
        return textureID;
    }

    @Override
    public String toString() {
        if(path != null)
            return path;
        else if (type != null)
            return "Default " + type + " texture";
        else if(rgb != null)
            return ""+rgb;
        return "Something is wrong with the texture";
    }
}
