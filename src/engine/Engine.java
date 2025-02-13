package engine;

import engine.utils.Logger;
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
    
    private static int callsOfUpdateLastSecond = 0;  // Count of update calls in the last second
    public static int framesRenderedLastSecond = 0; // Count of rendered frames in the last second
    private static long lastSecondTime = System.nanoTime(); // To track when the last second passed
    
    public static String shadersPath = "C:\\dev\\Arcane\\src\\res\\shaders\\";
    
    public static void init() {
        // Initialize GLFW and create a window
        initGLFW();
        
        // Initialize OpenGL bindings
        GL.createCapabilities();
        
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        
        // Initialize our modern shader-based renderer
        Renderer.init();
        
        // Start the game loop
        Input.init(window, WINDOW_WIDTH, WINDOW_HEIGHT); // Update Input handling to use GLFW
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
        glfwSwapInterval(1);
        
        // Show the window
        glfwShowWindow(window);
    }
    
    private static void run() {
        long lastTime = System.nanoTime();
        double nsPerTick = 1000000000.0 / TPS; // Nanoseconds per tick (update)
        double delta = 0.0;
        
        while (!quit && !glfwWindowShouldClose(window)) {
            // Update Time and deltaTime
            Time.update();
            
            // Calculate how much time has passed since the last update
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerTick;
            lastTime = now;
            
            if (firstFrame) {
                start();
                firstFrame = false;
            }
            
            // Update the game logic at a fixed time step
            while (delta >= 1.0) {
                Input.update();
                update();
                callsOfUpdateLastSecond++;
                delta -= 1.0;
            }
            
            // Call fixedUpdate if enough time has passed since the last fixed update
            fixedDelta += Time.deltaTime;
            if (fixedDelta >= FIXED_DT) {
                fixedUpdate();
                fixedDelta -= FIXED_DT;
            }
            
            // Render the scene using our modern renderer
            render();
            framesRenderedLastSecond++;
            frameCount++;
            
            // If 1 second has passed, log/reset counters
            if (System.nanoTime() - lastSecondTime >= 1000000000L) {
                Time.sec();
                callsOfUpdateLastSecond = 0;
                framesRenderedLastSecond = 0;
                lastSecondTime = System.nanoTime();
            }
            
            glfwPollEvents();
//            Logger.logStuff();
        }
        
        // Cleanup renderer resources before shutdown
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
        if (activeScene != null) {
            GameObject root = activeScene.rootGameObject;
            if (root != null) {
                updateRecursive(root);
            }
        }
    }
    
    private static void fixedUpdate() {
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
