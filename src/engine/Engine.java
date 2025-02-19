package engine;

import engine.utils.FileUtils;
import engine.utils.Logger;
import engine.utils.TransformManager;
import engine.utils.debug.DebugRenderer;
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
    /** Count of update() calls in the last second. */
    private static int callsOfUpdateLastSecond = 0;
    /** Count of fixedUpdate() calls in the last second. */
    private static int callsOfFixedUpdateLastSecond = 0;
    /** Count of rendered frames in the last second. */
    public static int framesRenderedLastSecond = 0;
    /** Time marker (in nanoseconds) for one-second intervals. */
    private static long lastSecondTime = System.nanoTime();
    /** Engine uptime in seconds. */
    public static float uptime = 0;
    
    /** Path to the shaders directory. */
    public static String shadersPath = FileUtils.getResPath() + "/shaders/";
    
    /**
     * Initializes the engine by setting up GLFW, OpenGL, input handling,
     * and then starting the main loop.
     */
    public static void init() {
        // Initialize GLFW and create the window.
        initGLFW();
        
        // Initialize OpenGL bindings.
        GL.createCapabilities();
        
        // Enable depth testing for proper 3D rendering.
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL); // Allow depth values equal to the far plane
        
        // Initialize our modern shader-based renderer.
        Renderer.init();
        DebugRenderer.init();
        
        // Initialize input handling using GLFW.
        Input.init(window, WINDOW_WIDTH, WINDOW_HEIGHT);
        run();
    }
    
    /**
     * Initializes GLFW, creates and configures the window, and sets up the OpenGL context.
     *
     * @throws IllegalStateException if GLFW fails to initialize.
     * @throws RuntimeException if the GLFW window cannot be created.
     */
    private static void initGLFW() {
        // Initialize GLFW.
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW window hints.
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // Window will stay hidden until shown.
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);  // Allow window to be resized.
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_COMPAT_PROFILE); // Force core profile
        glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);


        // Create the GLFW window.
        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "Arcane Engine", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Center the window on the primary monitor.
        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (vidMode.width() - WINDOW_WIDTH) / 2, (vidMode.height() - WINDOW_HEIGHT) / 2);

        // Set the current context to the created window.
        glfwMakeContextCurrent(window);

        // Disable v-sync (swap interval 0).
        glfwSwapInterval(0);

        // Make the window visible.
        glfwShowWindow(window);
    }
    
    /**
     * The main game loop.
     * <p>
     * This loop processes input, updates game logic (both variable and fixed timestep),
     * renders the scene, polls window events, and logs performance statistics.
     * </p>
     */
    private static void run() {
        double accumulator = 0.0;
        long previousTime = System.nanoTime();
        
        while (!quit && !glfwWindowShouldClose(window)) {
            // Calculate elapsed time in seconds.
            long currentTime = System.nanoTime();
            double frameTime = (currentTime - previousTime) / 1_000_000_000.0;
            previousTime = currentTime;
            
            // Optional: Cap frameTime to avoid spiral-of-death in case of a hiccup.
            frameTime = Math.min(frameTime, 0.25);
            
            // Set the global delta time (used in updates).
            Time.deltaTime = frameTime;
            
            // Call start() on the first frame.
            if (firstFrame) {
                start();
                firstFrame = false;
            }
            
            // Process input and update game logic (variable timestep update).
            Input.update();
            update();
            TransformManager.updateTransforms(activeScene.rootGameObject);
            
            // Accumulate time for fixed updates.
            accumulator += frameTime;
            if (accumulator >= FIXED_DT) {
                fixedUpdate();
                accumulator -= FIXED_DT;
            }
            
            // Render the scene.
            render();
            
            // Increment frame counters after rendering.
            framesRenderedLastSecond++;
            frameCount++;
            uptime += (float) Time.deltaTime;
            
            // Process window events.
            glfwPollEvents();
            
            // Log performance statistics every second.
            if (System.nanoTime() - lastSecondTime >= 1_000_000_000L) {
                Logger.logPerformance(framesRenderedLastSecond, callsOfUpdateLastSecond, callsOfFixedUpdateLastSecond, frameCount, uptime);
                // Reset performance counters for the next second.
                framesRenderedLastSecond = 0;
                callsOfUpdateLastSecond = 0;
                callsOfFixedUpdateLastSecond = 0;
                lastSecondTime = System.nanoTime();
            }
        }
        
        // Cleanup resources.
        Renderer.cleanup();
        glfwDestroyWindow(window);
        glfwTerminate();
    }
    
    /**
     * Renders the active scene.
     * <p>
     * Clears the color and depth buffers, renders the scene using the Renderer,
     * and swaps the GLFW window buffers.
     * </p>
     */
    private static void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        if (activeScene != null) {
            Renderer.render(activeScene);
        }
        // Uncomment the following line to enable debug rendering if needed.
        // DebugRenderer.render(shadowMap.depthMap);
        
        glfwSwapBuffers(window);
    }
    
    /**
     * Calls the start() method on all components in the active scene recursively.
     */
    private static void start() {
        if (activeScene != null) {
            GameObject root = activeScene.rootGameObject;
            if (root != null) {
                startRecursive(root);
            }
        }
    }
    
    /**
     * Performs a variable timestep update on the active scene.
     * <p>
     * Increments the update counter and recursively calls update() on all components.
     * </p>
     */
    private static void update() {
        updateCount++;
        //Logger.logStuff(); // Uncomment for debugging.
        callsOfUpdateLastSecond++; // Increment update counter for performance stats.
        if (activeScene != null) {
            GameObject root = activeScene.rootGameObject;
            if (root != null) {
                updateRecursive(root);
            }
        }
    }
    
    /**
     * Performs a fixed timestep update on the active scene.
     * <p>
     * Recursively calls fixedUpdate() on all components and increments the fixed update counter.
     * </p>
     */
    private static void fixedUpdate() {
        callsOfFixedUpdateLastSecond++; // Increment fixed update counter for performance stats.
        if (activeScene != null) {
            GameObject root = activeScene.rootGameObject;
            if (root != null) {
                fixedUpdateRecursive(root);
            }
        }
    }
    
    /**
     * Recursively calls the start() method on all components of the given GameObject and its children.
     *
     * @param gameObject the root GameObject to start.
     */
    private static void startRecursive(GameObject gameObject) {
        for (Component component : gameObject.getComponents()) {
            component.start();
        }
        for (GameObject child : gameObject.children) {
            startRecursive(child);
        }
    }
    
    /**
     * Recursively calls the update() method on all components of the given GameObject and its children.
     *
     * @param gameObject the root GameObject to update.
     */
    private static void updateRecursive(GameObject gameObject) {
        for (Component component : gameObject.getComponents()) {
            component.update();
        }
        for (GameObject child : gameObject.children) {
            updateRecursive(child);
        }
    }
    
    /**
     * Recursively calls the fixedUpdate() method on all components of the given GameObject and its children.
     *
     * @param gameObject the root GameObject for fixed updates.
     */
    private static void fixedUpdateRecursive(GameObject gameObject) {
        for (Component component : gameObject.getComponents()) {
            component.fixedUpdate();
        }
        for (GameObject child : gameObject.children) {
            fixedUpdateRecursive(child);
        }
    }
}
