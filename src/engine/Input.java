package engine;

import engine.utils.KeyMapper;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;

import static org.lwjgl.glfw.GLFW.*;

public class Input {
    
    private static boolean[] keys = new boolean[GLFW_KEY_LAST];
    private static boolean[] keysLast = new boolean[GLFW_KEY_LAST];
    private static boolean[] mouse = new boolean[GLFW_MOUSE_BUTTON_LAST];
    private static boolean[] mouseLast = new boolean[GLFW_MOUSE_BUTTON_LAST];
    private static double mouseX, mouseY;
    private static double lastMouseX, lastMouseY;
    private static double deltaX = 0, deltaY = 0;
    
    private static boolean cursorLocked = false; // Cursor locked state
    private static long windowHandle; // Store the window handle for resetting the cursor position
    private static int windowWidth, windowHeight; // Store the window size for centering the cursor
    
    public static void init(long windowHandle, int windowWidth, int windowHeight) {
        Input.windowHandle = windowHandle;
        Input.windowWidth = windowWidth;
        Input.windowHeight = windowHeight;
        
        // Set up key callback
        glfwSetKeyCallback(windowHandle, new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key >= 0 && key < GLFW_KEY_LAST) {
                    keys[key] = action != GLFW_RELEASE;
                }
            }
        });
        
        // Set up mouse button callback
        glfwSetMouseButtonCallback(windowHandle, new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                if (button >= 0 && button < GLFW_MOUSE_BUTTON_LAST) {
                    mouse[button] = action != GLFW_RELEASE;
                }
            }
        });
        
        // Set up cursor position callback
        glfwSetCursorPosCallback(windowHandle, new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                mouseX = xpos;
                mouseY = ypos;
            }
        });
    }
    
    // Keyboard methods using key name (String)
    public static boolean getKey(String keyName) {
        Integer keyCode = KeyMapper.getKeyCode(keyName);
        return keyCode != null && keys[keyCode];
    }
    
    public static boolean getKeyDown(String keyName) {
        Integer keyCode = KeyMapper.getKeyCode(keyName);
        return keyCode != null && keys[keyCode] && !keysLast[keyCode];
    }
    
    public static boolean getKeyUp(String keyName) {
        Integer keyCode = KeyMapper.getKeyCode(keyName);
        return keyCode != null && !keys[keyCode] && keysLast[keyCode];
    }
    
    // Mouse methods
    public static boolean getButton(int button) {
        return mouse[button];
    }
    
    public static boolean getButtonDown(int button) {
        return mouse[button] && !mouseLast[button];
    }
    
    public static boolean getButtonUp(int button) {
        return !mouse[button] && mouseLast[button];
    }
    
    // Mouse delta methods
    public static double getMouseDeltaX() {
        return deltaX;
    }
    
    public static double getMouseDeltaY() {
        return deltaY;
    }
    
    // Mouse position getters
    public static double getMouseX() {
        return mouseX;
    }
    
    public static double getMouseY() {
        return mouseY;
    }
    
    // Cursor locking methods
    public static void setCursorLocked(boolean locked) {
        cursorLocked = locked;
        if (locked) {
            glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_HIDDEN); // Hide the cursor
        } else {
            glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_NORMAL); // Show the cursor
        }
    }
    
    public static boolean isCursorLocked() {
        return cursorLocked;
    }
    
    public static boolean isMouseClickInBounds(int mouseButton) {
        // Check if the mouse button is pressed down
        if (mouse[mouseButton]) {
            // Ensure the mouse position is within the screen bounds
            return mouseX >= 0 && mouseX <= windowWidth && mouseY >= 0 && mouseY <= windowHeight;
        }
        return false;
    }
    
    
    
    // Update method to process deltas and handle cursor locking
    public static void update() {
        if (cursorLocked) {
            // Compute deltas relative to the center of the screen
            deltaX = mouseX - windowWidth / 2.0;
            deltaY = mouseY - windowHeight / 2.0;
            
            // Reset the cursor to the center of the screen
            glfwSetCursorPos(windowHandle, windowWidth / 2.0, windowHeight / 2.0);
            mouseX = windowWidth / 2.0;
            mouseY = windowHeight / 2.0;
        } else {
            // Compute deltas normally when cursor is not locked
            deltaX = mouseX - lastMouseX;
            deltaY = mouseY - lastMouseY;
            
            // Update the last mouse position
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }
        
        // Update key and mouse button states
        System.arraycopy(keys, 0, keysLast, 0, keys.length);
        System.arraycopy(mouse, 0, mouseLast, 0, mouse.length);
    }
    
}
