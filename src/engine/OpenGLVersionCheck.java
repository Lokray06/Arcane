package engine;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class OpenGLVersionCheck {
    public static void main(String[] args) {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW");
        }
        
        long window = GLFW.glfwCreateWindow(800, 600, "OpenGL Version Check", 0, 0);
        if (window == 0) {
            throw new IllegalStateException("Failed to create GLFW window");
        }
        
        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();
        
        String version = GL11.glGetString(GL11.GL_VERSION);
        String renderer = GL11.glGetString(GL11.GL_RENDERER);
        String vendor = GL11.glGetString(GL11.GL_VENDOR);
        String glslVersion = GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION); // FIXED
        
        System.out.println("OpenGL Version: " + version);
        System.out.println("Renderer: " + renderer);
        System.out.println("Vendor: " + vendor);
        System.out.println("GLSL Version: " + glslVersion);
        
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }
}
