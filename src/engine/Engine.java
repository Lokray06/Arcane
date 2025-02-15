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

public class Engine {
    public static final int WINDOW_WIDTH = 1920;
    public static final int WINDOW_HEIGHT = 1080;
    public static boolean quit = false;
    public static final int TPS = 30; // Fixed update rate in Hz
    static boolean firstFrame = true;
    public static long updateCount = 0;
    public static long frameCount = 0;
    public static Scene activeScene;
    private static long window; // GLFW window handle
    private static double fixedDelta = 0.0; // Accumulator for fixed updates
    private static final double FIXED_DT = 1.0 / TPS; // Fixed timestep

    // Performance Counters
    private static int callsOfUpdateLastSecond = 0;       // Count of update() calls in the last second
    private static int callsOfFixedUpdateLastSecond = 0;    // Count of fixedUpdate() calls in the last second
    public static int framesRenderedLastSecond = 0;         // Count of rendered frames in the last second
    private static long lastSecondTime = System.nanoTime();  // Time marker to check one-second intervals
    public static float uptime = 0;

    public static String shadersPath = FileUtils.getResPath() + "/shaders/";

    public static void init() {
        // Initialize GLFW and create a window
        initGLFW();

        // Initialize OpenGL bindings
        GL.createCapabilities();

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL); // Allow depth values equal to the far plane
        
        
        // Initialize our modern shader-based renderer
        Renderer.init();
        DebugRenderer.init();

        // Initialize input handling using GLFW
        Input.init(window, WINDOW_WIDTH, WINDOW_HEIGHT);
        run();
    }

    private static void initGLFW() {
        // Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // Window will stay hidden until shown
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);  // Window resizable

        // Create the window
        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "Arcane Engine", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Center the window
        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (vidMode.width() - WINDOW_WIDTH) / 2, (vidMode.height() - WINDOW_HEIGHT) / 2);

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);

        // Enable v-sync
        glfwSwapInterval(0);

        // Show the window
        glfwShowWindow(window);
    }

    private static void run() {
        double accumulator = 0.0;
        long previousTime = System.nanoTime();

        while (!quit && !glfwWindowShouldClose(window)) {
            // Calculate elapsed time in seconds
            long currentTime = System.nanoTime();
            double frameTime = (currentTime - previousTime) / 1_000_000_000.0;
            previousTime = currentTime;

            // Optional: Cap frameTime to avoid spiral-of-death in case of a hiccup
            frameTime = Math.min(frameTime, 0.25);

            // Set the global delta time (for use in Update, etc.)
            Time.deltaTime = frameTime;

            // Call start() on the first frame if needed
            if (firstFrame) {
                start();
                firstFrame = false;
            }

            // Process input and update game logic (variable timestep update)
            Input.update();
            update();
            TransformManager.updateTransforms(activeScene.rootGameObject);

            // Accumulate time for fixed updates
            accumulator += frameTime;
            if (accumulator >= FIXED_DT) {
                fixedUpdate();
                accumulator -= FIXED_DT;
            }

            // Render the scene
            render();

            // Increment frame counters after rendering
            framesRenderedLastSecond++;
            frameCount++;
            uptime += (float) Time.deltaTime;

            // Process window events
            glfwPollEvents();

            // Log performance stats every second
            if (System.nanoTime() - lastSecondTime >= 1_000_000_000L) {
                //Logger.logPerformance(framesRenderedLastSecond, callsOfUpdateLastSecond, callsOfFixedUpdateLastSecond, frameCount, uptime);
                // Reset performance counters for the next second
                framesRenderedLastSecond = 0;
                callsOfUpdateLastSecond = 0;
                callsOfFixedUpdateLastSecond = 0;
                lastSecondTime = System.nanoTime();
            }
        }

        // Cleanup
        Renderer.cleanup();
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private static void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        if (activeScene != null) {
            Renderer.render(activeScene);
        }
        //DebugRenderer.render(shadowMap.depthMap);

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
        //Logger.logStuff();
        callsOfUpdateLastSecond++; // Increment update counter for performance stats
        if (activeScene != null) {
            GameObject root = activeScene.rootGameObject;
            if (root != null) {
                updateRecursive(root);
            }
        }
    }

    private static void fixedUpdate() {
        callsOfFixedUpdateLastSecond++; // Increment fixed update counter for performance stats
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
