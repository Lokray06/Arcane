package engine;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * The {@code CubeMapTexture} class encapsulates a cubemap texture.
 * <p>
 * It supports loading a cubemap texture either from a predefined layout (4x3 grid)
 * or by converting an equirectangular image into a cubemap.
 * </p>
 */
public class CubeMapTexture {
    /** The OpenGL texture ID for the cubemap (0 if not yet initialized). */
    private int textureID = 0;
    /** The path to the texture image file. */
    private final String texturePath;
    /** Flag indicating whether the texture has been loaded. */
    private boolean loaded = false;
    /** Flag indicating if the provided image is an equirectangular image that should be converted to a cubemap. */
    private boolean isCubemap = false;
    
    /**
     * Constructs a {@code CubeMapTexture} assuming the image is already a cubemap (using a 4x3 layout).
     *
     * @param texturePath the path to the cubemap texture image.
     */
    public CubeMapTexture(String texturePath) {
        this(texturePath, false);
    }
    
    /**
     * Constructs a {@code CubeMapTexture}.
     *
     * @param texturePath the path to the texture image.
     * @param isCubemap   {@code true} if the texture should be loaded as an equirectangular image and converted to a cubemap.
     */
    public CubeMapTexture(String texturePath, boolean isCubemap) {
        this.texturePath = texturePath;
        this.isCubemap = isCubemap;
    }
    
    /**
     * Loads the cubemap texture.
     * <p>
     * If {@code isCubemap} is false, it loads the image from a 4x3 layout.
     * Otherwise, it converts an equirectangular image into a cubemap.
     * </p>
     */
    private void loadCubeMap() {
        if (loaded) return; // Prevent reloading
        
        // If the texture is not already a cubemap layout, load as equirectangular and convert.
        if (!isCubemap) {
            loadEquirectangularToCubemap();
            return;
        }
        
        // Otherwise, assume the image contains a 4x3 layout of faces.
        textureID = GL11.glGenTextures();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, textureID);
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            
            ByteBuffer image = STBImage.stbi_load(texturePath, width, height, channels, 4);
            if (image == null) {
                throw new RuntimeException("Failed to load cubemap texture: " + texturePath);
            }
            
            int imgWidth = width.get(0);
            int imgHeight = height.get(0);
            int faceSize = imgWidth / 4; // Assuming the UV layout is a 4x3 grid
            
            // Define face positions in the UV map.
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
        
        // Set texture parameters.
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL12.GL_TEXTURE_WRAP_R, GL12.GL_CLAMP_TO_EDGE);
        
        GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, 0);
        loaded = true;
    }
    
    /**
     * Extracts a single face from the overall cubemap image.
     *
     * @param image    the full image buffer.
     * @param imgWidth the width of the full image.
     * @param imgHeight the height of the full image.
     * @param xOffset  the x offset for the face.
     * @param yOffset  the y offset for the face.
     * @param faceSize the size of the face.
     * @return a {@code ByteBuffer} containing the face data.
     */
    private ByteBuffer extractFace(ByteBuffer image, int imgWidth, int imgHeight, int xOffset, int yOffset, int faceSize) {
        int bytesPerPixel = 4; // RGBA
        ByteBuffer faceData = BufferUtils.createByteBuffer(faceSize * faceSize * bytesPerPixel);
        
        for (int y = 0; y < faceSize; y++) {
            int srcOffset = ((yOffset + y) * imgWidth + xOffset) * bytesPerPixel;
            int destOffset = y * faceSize * bytesPerPixel;
            for (int x = 0; x < faceSize * bytesPerPixel; x++) {
                faceData.put(destOffset + x, image.get(srcOffset + x));
            }
        }
        return faceData;
    }
    
    /**
     * Loads an equirectangular image and converts it into a cubemap texture.
     * <p>
     * This method creates a framebuffer, renders the equirectangular texture onto each cubemap face,
     * and generates mipmaps for the resulting cubemap.
     * </p>
     */
    private void loadEquirectangularToCubemap() {
        // 1. Load the equirectangular texture from file.
        STBImage.stbi_set_flip_vertically_on_load(true);
        int eqTexture;
        int eqWidth, eqHeight;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            
            ByteBuffer data = STBImage.stbi_load(texturePath, width, height, channels, 4);
            if (data == null) {
                throw new RuntimeException("Failed to load equirectangular texture: " + texturePath);
            }
            eqWidth = width.get(0);
            eqHeight = height.get(0);
            
            eqTexture = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, eqTexture);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, eqWidth, eqHeight, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, data);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            STBImage.stbi_image_free(data);
        }
        
        // 2. Create an empty cubemap texture.
        int cubeSize = 512; // Adjust resolution as needed.
        textureID = GL11.glGenTextures();
        GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, textureID);
        for (int i = 0; i < 6; i++) {
            GL11.glTexImage2D(
                    GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i,
                    0,
                    GL11.GL_RGBA,
                    cubeSize,
                    cubeSize,
                    0,
                    GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE,
                    (ByteBuffer) null
            );
        }
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL12.GL_TEXTURE_WRAP_R, GL12.GL_CLAMP_TO_EDGE);
        
        // 3. Set up framebuffer and renderbuffer to render to each cubemap face.
        int captureFBO = GL30.glGenFramebuffers();
        int captureRBO = GL30.glGenRenderbuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, captureFBO);
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, captureRBO);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH_COMPONENT24, cubeSize, cubeSize);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, captureRBO);
        
        // 4. Create the shader used for the conversion.
        String vertShaderSource = "#version 330 core\n" +
                                  "layout (location = 0) in vec3 aPos;\n" +
                                  "out vec3 localPos;\n" +
                                  "uniform mat4 projection;\n" +
                                  "uniform mat4 view;\n" +
                                  "void main()\n" +
                                  "{\n" +
                                  "    localPos = aPos;\n" +
                                  "    gl_Position = projection * view * vec4(aPos, 1.0);\n" +
                                  "}\n";
        String fragShaderSource = "#version 330 core\n" +
                                  "out vec4 FragColor;\n" +
                                  "in vec3 localPos;\n" +
                                  "uniform sampler2D equirectangularMap;\n" +
                                  "const vec2 invAtan = vec2(0.1591, 0.3183);\n" +
                                  "vec2 sampleSphericalMap(vec3 v)\n" +
                                  "{\n" +
                                  "    vec2 uv = vec2(atan(v.z, v.x), asin(v.y));\n" +
                                  "    uv *= invAtan;\n" +
                                  "    uv += 0.5;\n" +
                                  "    return uv;\n" +
                                  "}\n" +
                                  "void main()\n" +
                                  "{   \n" +
                                  "    vec2 uv = sampleSphericalMap(normalize(localPos));\n" +
                                  "    vec3 color = texture(equirectangularMap, uv).rgb;\n" +
                                  "    FragColor = vec4(color, 1.0);\n" +
                                  "}\n";
        
        engine.utils.ShaderProgram convertShader = new engine.utils.ShaderProgram(vertShaderSource, fragShaderSource);
        convertShader.use();
        
        // 5. Set up the projection and view matrices for capturing data onto the cubemap.
        Matrix4f captureProjection = new Matrix4f().perspective((float) Math.toRadians(90.0f), 1.0f, 0.1f, 10.0f);
        convertShader.setUniformMat4("projection", captureProjection);
        
        Matrix4f[] captureViews = new Matrix4f[6];
        captureViews[0] = new Matrix4f().lookAt(new Vector3f(0, 0, 0), new Vector3f(1,  0,  0), new Vector3f(0, -1,  0));
        captureViews[1] = new Matrix4f().lookAt(new Vector3f(0, 0, 0), new Vector3f(-1, 0,  0), new Vector3f(0, -1,  0));
        captureViews[2] = new Matrix4f().lookAt(new Vector3f(0, 0, 0), new Vector3f(0,  1,  0), new Vector3f(0,  0,  1));
        captureViews[3] = new Matrix4f().lookAt(new Vector3f(0, 0, 0), new Vector3f(0, -1,  0), new Vector3f(0,  0, -1));
        captureViews[4] = new Matrix4f().lookAt(new Vector3f(0, 0, 0), new Vector3f(0,  0,  1), new Vector3f(0, -1,  0));
        captureViews[5] = new Matrix4f().lookAt(new Vector3f(0, 0, 0), new Vector3f(0,  0, -1), new Vector3f(0, -1,  0));
        
        // Bind the equirectangular texture to texture unit 0.
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, eqTexture);
        convertShader.setUniform("equirectangularMap", 0);
        
        // 6. Create a cube VAO and VBO for rendering.
        int cubeVAO = GL30.glGenVertexArrays();
        int cubeVBO = GL15.glGenBuffers();
        float[] cubeVertices = {
                // positions for a 36-vertex cube.
                -1.0f,  1.0f, -1.0f,
                -1.0f, -1.0f, -1.0f,
                1.0f, -1.0f, -1.0f,
                1.0f, -1.0f, -1.0f,
                1.0f,  1.0f, -1.0f,
                -1.0f,  1.0f, -1.0f,
                
                -1.0f, -1.0f,  1.0f,
                -1.0f, -1.0f, -1.0f,
                -1.0f,  1.0f, -1.0f,
                -1.0f,  1.0f, -1.0f,
                -1.0f,  1.0f,  1.0f,
                -1.0f, -1.0f,  1.0f,
                
                1.0f, -1.0f, -1.0f,
                1.0f, -1.0f,  1.0f,
                1.0f,  1.0f,  1.0f,
                1.0f,  1.0f,  1.0f,
                1.0f,  1.0f, -1.0f,
                1.0f, -1.0f, -1.0f,
                
                -1.0f, -1.0f,  1.0f,
                -1.0f,  1.0f,  1.0f,
                1.0f,  1.0f,  1.0f,
                1.0f,  1.0f,  1.0f,
                1.0f, -1.0f,  1.0f,
                -1.0f, -1.0f,  1.0f,
                
                -1.0f,  1.0f, -1.0f,
                1.0f,  1.0f, -1.0f,
                1.0f,  1.0f,  1.0f,
                1.0f,  1.0f,  1.0f,
                -1.0f,  1.0f,  1.0f,
                -1.0f,  1.0f, -1.0f,
                
                -1.0f, -1.0f, -1.0f,
                -1.0f, -1.0f,  1.0f,
                1.0f, -1.0f, -1.0f,
                1.0f, -1.0f, -1.0f,
                -1.0f, -1.0f,  1.0f,
                1.0f, -1.0f,  1.0f
        };
        GL30.glBindVertexArray(cubeVAO);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, cubeVBO);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, cubeVertices, GL15.GL_STATIC_DRAW);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        
        // 7. Render to each cubemap face.
        GL11.glViewport(0, 0, cubeSize, cubeSize);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, captureFBO);
        for (int i = 0; i < 6; i++) {
            convertShader.setUniformMat4("view", captureViews[i]);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                                        GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, textureID, 0);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GL30.glBindVertexArray(cubeVAO);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 36);
            GL30.glBindVertexArray(0);
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        
        // Generate mipmaps for the cubemap texture.
        GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, textureID);
        GL30.glGenerateMipmap(GL13.GL_TEXTURE_CUBE_MAP);
        GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, 0);
        
        // Cleanup: detach shader, delete FBO/RBO, cube VAO/VBO, and the temporary equirectangular texture.
        convertShader.detach();
        GL30.glDeleteFramebuffers(captureFBO);
        GL30.glDeleteRenderbuffers(captureRBO);
        GL30.glDeleteVertexArrays(cubeVAO);
        GL15.glDeleteBuffers(cubeVBO);
        GL11.glDeleteTextures(eqTexture);
        
        loaded = true;
    }
    
    /**
     * Ensures the cubemap texture is loaded.
     */
    private void ensureLoaded() {
        if (!loaded) {
            loadCubeMap();
        }
    }
    
    /**
     * Binds the cubemap texture to the specified texture unit.
     *
     * @param unit the texture unit to bind to.
     */
    public void bind(int unit) {
        ensureLoaded();
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, textureID);
    }
    
    /**
     * Returns the cubemap texture ID.
     *
     * @return the texture ID.
     */
    public int getID() {
        ensureLoaded();
        return textureID;
    }
    
    /**
     * Deletes the cubemap texture and releases its OpenGL resources.
     */
    public void delete() {
        if (textureID != 0) {
            GL11.glDeleteTextures(textureID);
            textureID = 0;
            loaded = false;
        }
    }
}
