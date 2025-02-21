package engine;

import engine.editor.Editor;
import engine.utils.FileUtils;
import engine.utils.Logger;
import engine.utils.TransformManager;
import engine.utils.debug.DebugRenderer;
import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * The Engine class serves as the main entry point for the game engine.
 * It handles initialization of GLFW, OpenGL, input management,
 * the main game loop (updates, fixed updates, and rendering), and cleanup.
 *
 * <p>This class uses a fixed timestep (TPS) for fixed updates and variable timestep for other updates.
 * It also provides performance statistics logging for debugging and profiling.</p>
 */
public class Engine {
    /** Window width in pixels. */
    public static final int WINDOW_WIDTH = 1920;
    /** Window height in pixels. */
    public static final int WINDOW_HEIGHT = 1080;
    /** Flag to indicate when the engine should quit. */
    public static boolean quit = false;
    /** Fixed update rate in Hz (ticks per second). */
    public static final int TPS = 30;
    /** Flag to indicate the first frame of the engine's execution. */
    static boolean firstFrame = true;
    /** Total number of update() calls since start. */
    public static long updateCount = 0;
    /** Total number of frames rendered since start. */
    public static long frameCount = 0;
    /** The currently active scene. */
    public static Scene activeScene;
    /** GLFW window handle. */
    private static long window;
    /** Accumulator for fixed update timing. */
    private static double fixedDelta = 0.0;
    /** Fixed timestep (in seconds) calculated from TPS. */
    private static final double FIXED_DT = 1.0 / TPS;
    
    // Performance Counters
    private static int callsOfUpdateLastSecond = 0;
    private static int callsOfFixedUpdateLastSecond = 0;
    public static int framesRenderedLastSecond = 0;
    private static long lastSecondTime = System.nanoTime();
    public static float uptime = 0;
    
    public static Editor editor = new Editor();
    
    /** Path to the shaders directory. */
    public static String shadersPath = FileUtils.getResPath() + "/shaders/";
    
    // ImGui renderer instances
    private static ImGuiImplGl3 imGuiGl3;
    private static ImGuiImplGlfw imGuiGlfw;
    
    /**
     * Initializes the engine by setting up GLFW, OpenGL, input handling,
     * and then starting the main loop.
     */
    public static void init() throws IllegalAccessException
    {
        // Initialize GLFW and create the window.
        initGLFW();
        
        // Initialize OpenGL bindings.
        GL.createCapabilities();
        
        // ===== ImGui Initialization =====
        ImGui.createContext();
        // Set IO configuration flags BEFORE initializing the platform backend!
        int ioConfigFlags = ImGui.getIO().getConfigFlags();
        ioConfigFlags |= ImGuiConfigFlags.DockingEnable | ImGuiConfigFlags.ViewportsEnable;
        ImGui.getIO().setConfigFlags(ioConfigFlags);
        
        // Initialize the GLFW backend for ImGui (this installs mouse, keyboard, and window callbacks)
        imGuiGlfw = new ImGuiImplGlfw();
        imGuiGlfw.init(window, true);
        
        // Now initialize the OpenGL3 renderer for ImGui.
        imGuiGl3 = new ImGuiImplGl3();
        imGuiGl3.init("#version 460");  // Use the appropriate GLSL version for your setup
        // ===================================
        
        // Enable depth testing for proper 3D rendering.
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        
        // Initialize our modern shader-based renderer.
        Renderer.init();
        DebugRenderer.init();
        
        // Initialize our custom input handling.
        Input.init(window, WINDOW_WIDTH, WINDOW_HEIGHT);
        
        run();
    }
    
    private static void initGLFW() {
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_COMPAT_PROFILE);
        glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);
        
        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "Arcane Engine", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (vidMode.width() - WINDOW_WIDTH) / 2, (vidMode.height() - WINDOW_HEIGHT) / 2);
        glfwMakeContextCurrent(window);
        glfwSwapInterval(0);
        glfwShowWindow(window);
    }
    
    private static void run() throws IllegalAccessException
    {
        double accumulator = 0.0;
        long previousTime = System.nanoTime();
        
        while (!quit && !glfwWindowShouldClose(window)) {
            long currentTime = System.nanoTime();
            double frameTime = (currentTime - previousTime) / 1_000_000_000.0;
            previousTime = currentTime;
            frameTime = Math.min(frameTime, 0.25);
            Time.deltaTime = frameTime;
            
            if (firstFrame) {
                start();
                firstFrame = false;
            }
            
            update();
            // Update custom input (which now chains ImGui’s callbacks)
            Input.update();
            TransformManager.updateTransforms(activeScene.rootGameObject);
            
            accumulator += frameTime;
            if (accumulator >= FIXED_DT) {
                fixedUpdate();
                accumulator -= FIXED_DT;
            }
            
            // ===== Rendering Phase =====
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            
            if (activeScene != null) {
                Renderer.render(activeScene);
            }
            
            // Update display size using the current framebuffer dimensions.
            int[] fbWidth = new int[1];
            int[] fbHeight = new int[1];
            glfwGetFramebufferSize(window, fbWidth, fbHeight);
            ImGui.getIO().setDisplaySize((float) fbWidth[0], (float) fbHeight[0]);
            
            imGuiGlfw.newFrame();
            imGuiGl3.newFrame();
            ImGui.newFrame();
            
            // Render the editor UI.
            editor.render(activeScene);
            
            ImGui.render();
            imGuiGl3.renderDrawData(ImGui.getDrawData());
            
            if ((ImGui.getIO().getConfigFlags() & ImGuiConfigFlags.ViewportsEnable) != 0) {
                long backupCurrentContext = glfwGetCurrentContext();
                ImGui.updatePlatformWindows();
                ImGui.renderPlatformWindowsDefault();
                glfwMakeContextCurrent(backupCurrentContext);
            }
            
            glfwSwapBuffers(window);
            
            framesRenderedLastSecond++;
            frameCount++;
            uptime += (float) Time.deltaTime;
            glfwPollEvents();
            
            if (System.nanoTime() - lastSecondTime >= 1_000_000_000L) {
                Logger.logPerformance(framesRenderedLastSecond, callsOfUpdateLastSecond, callsOfFixedUpdateLastSecond, frameCount, uptime);
                framesRenderedLastSecond = 0;
                callsOfUpdateLastSecond = 0;
                callsOfFixedUpdateLastSecond = 0;
                lastSecondTime = System.nanoTime();
            }
        }
        
        imGuiGlfw.shutdown();
        imGuiGl3.shutdown();
        Renderer.cleanup();
        glfwDestroyWindow(window);
        glfwTerminate();
    }
    
    private static void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        if (activeScene != null) {
            Renderer.render(activeScene);
        }
        glfwSwapBuffers(window);
    }
    
    private static void start() {
        if (activeScene != null) {
            GameObject root = activeScene.rootGameObject;
            if (root != null) {
                startRecursive(root);
            }
        }
    }
    
    private static void update() {
        updateCount++;
        callsOfUpdateLastSecond++;
        if (activeScene != null) {
            GameObject root = activeScene.rootGameObject;
            if (root != null) {
                updateRecursive(root);
            }
        }
    }
    
    private static void fixedUpdate() {
        callsOfFixedUpdateLastSecond++;
        if (activeScene != null) {
            GameObject root = activeScene.rootGameObject;
            if (root != null) {
                fixedUpdateRecursive(root);
            }
        }
    }
    
    private static void startRecursive(GameObject gameObject) {
        for (Component component : gameObject.getComponents()) {
            component.start();
        }
        for (GameObject child : gameObject.children) {
            startRecursive(child);
        }
    }
    
    private static void updateRecursive(GameObject gameObject) {
        for (Component component : gameObject.getComponents()) {
            component.update();
        }
        for (GameObject child : gameObject.children) {
            updateRecursive(child);
        }
    }
    
    private static void fixedUpdateRecursive(GameObject gameObject) {
        for (Component component : gameObject.getComponents()) {
            component.fixedUpdate();
        }
        for (GameObject child : gameObject.children) {
            fixedUpdateRecursive(child);
        }
    }
}
