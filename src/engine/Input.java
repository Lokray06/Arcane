package engine;

import engine.utils.KeyMapper;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;

import static org.lwjgl.glfw.GLFW.*;

/**
 * The {@code Input} class handles user input for keyboard and mouse.
 * <p>
 * It sets up GLFW callbacks for key presses, mouse button clicks, and mouse movement,
 * and provides methods to query current and previous input states as well as mouse deltas.
 * </p>
 */
public class Input {
    
    /** Array representing the current state of keyboard keys. */
    private static boolean[] keys = new boolean[GLFW_KEY_LAST];
    /** Array representing the previous state of keyboard keys. */
    private static boolean[] keysLast = new boolean[GLFW_KEY_LAST];
    /** Array representing the current state of mouse buttons. */
    private static boolean[] mouse = new boolean[GLFW_MOUSE_BUTTON_LAST];
    /** Array representing the previous state of mouse buttons. */
    private static boolean[] mouseLast = new boolean[GLFW_MOUSE_BUTTON_LAST];
    /** Current mouse X-coordinate. */
    private static double mouseX, mouseY;
    /** Previous mouse X-coordinate. */
    private static double lastMouseX, lastMouseY;
    /** Change in mouse X-coordinate since last update. */
    private static double deltaX = 0, deltaY = 0;
    
    /** Flag indicating whether the cursor is locked (hidden and centered). */
    private static boolean cursorLocked = false;
    /** The GLFW window handle, used for resetting the cursor position. */
    private static long windowHandle;
    /** The window width, used for centering the cursor. */
    private static int windowWidth;
    /** The window height, used for centering the cursor. */
    private static int windowHeight;
    
    /**
     * Initializes input handling by setting up GLFW callbacks for keyboard, mouse buttons, and cursor position.
     *
     * @param windowHandle the GLFW window handle.
     * @param windowWidth  the width of the window.
     * @param windowHeight the height of the window.
     */
    public static void init(long windowHandle, int windowWidth, int windowHeight) {
        Input.windowHandle = windowHandle;
        Input.windowWidth = windowWidth;
        Input.windowHeight = windowHeight;
        
        // Set up key callback.
        glfwSetKeyCallback(windowHandle, new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key >= 0 && key < GLFW_KEY_LAST) {
                    keys[key] = action != GLFW_RELEASE;
                }
            }
        });
        
        // Set up mouse button callback.
        glfwSetMouseButtonCallback(windowHandle, new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                if (button >= 0 && button < GLFW_MOUSE_BUTTON_LAST) {
                    mouse[button] = action != GLFW_RELEASE;
                }
            }
        });
        
        // Set up cursor position callback.
        glfwSetCursorPosCallback(windowHandle, new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                mouseX = xpos;
                mouseY = ypos;
            }
        });
    }
    
    // --- Keyboard Methods ---
    
    /**
     * Checks if the key corresponding to the given key name is currently pressed.
     *
     * @param keyName the name of the key.
     * @return {@code true} if the key is pressed; {@code false} otherwise.
     */
    public static boolean getKey(String keyName) {
        Integer keyCode = KeyMapper.getKeyCode(keyName);
        return keyCode != null && keys[keyCode];
    }
    
    /**
     * Checks if the key corresponding to the given key name was pressed during this frame.
     *
     * @param keyName the name of the key.
     * @return {@code true} if the key was pressed this frame; {@code false} otherwise.
     */
    public static boolean getKeyDown(String keyName) {
        Integer keyCode = KeyMapper.getKeyCode(keyName);
        return keyCode != null && keys[keyCode] && !keysLast[keyCode];
    }
    
    /**
     * Checks if the key corresponding to the given key name was released during this frame.
     *
     * @param keyName the name of the key.
     * @return {@code true} if the key was released this frame; {@code false} otherwise.
     */
    public static boolean getKeyUp(String keyName) {
        Integer keyCode = KeyMapper.getKeyCode(keyName);
        return keyCode != null && !keys[keyCode] && keysLast[keyCode];
    }
    
    // --- Mouse Methods ---
    
    /**
     * Checks if the specified mouse button is currently pressed.
     *
     * @param button the mouse button index.
     * @return {@code true} if the button is pressed; {@code false} otherwise.
     */
    public static boolean getButton(int button) {
        return mouse[button];
    }
    
    /**
     * Checks if the specified mouse button was pressed during this frame.
     *
     * @param button the mouse button index.
     * @return {@code true} if the button was pressed this frame; {@code false} otherwise.
     */
    public static boolean getButtonDown(int button) {
        return mouse[button] && !mouseLast[button];
    }
    
    /**
     * Checks if the specified mouse button was released during this frame.
     *
     * @param button the mouse button index.
     * @return {@code true} if the button was released this frame; {@code false} otherwise.
     */
    public static boolean getButtonUp(int button) {
        return !mouse[button] && mouseLast[button];
    }
    
    /**
     * Returns the horizontal mouse movement (delta X) since the last update.
     *
     * @return the mouse delta X.
     */
    public static double getMouseDeltaX() {
        return deltaX;
    }
    
    /**
     * Returns the vertical mouse movement (delta Y) since the last update.
     *
     * @return the mouse delta Y.
     */
    public static double getMouseDeltaY() {
        return deltaY;
    }
    
    /**
     * Returns the current mouse X-coordinate.
     *
     * @return the mouse X-coordinate.
     */
    public static double getMouseX() {
        return mouseX;
    }
    
    /**
     * Returns the current mouse Y-coordinate.
     *
     * @return the mouse Y-coordinate.
     */
    public static double getMouseY() {
        return mouseY;
    }
    
    // --- Cursor Locking Methods ---
    
    /**
     * Sets whether the cursor should be locked (hidden and centered).
     *
     * @param locked {@code true} to lock the cursor; {@code false} to release it.
     */
    public static void setCursorLocked(boolean locked) {
        cursorLocked = locked;
        if (locked) {
            glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_DISABLED); // Lock & hide the cursor.
        } else {
            glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_NORMAL); // Show the cursor.
        }
    }
    
    /**
     * Checks whether the cursor is currently locked.
     *
     * @return {@code true} if the cursor is locked; {@code false} otherwise.
     */
    public static boolean isCursorLocked() {
        return cursorLocked;
    }
    
    /**
     * Checks if the specified mouse button is pressed and if the mouse is within the window bounds.
     *
     * @param mouseButton the mouse button index.
     * @return {@code true} if the button is pressed and the mouse is within bounds; {@code false} otherwise.
     */
    public static boolean isMouseClickInBounds(int mouseButton) {
        // Check if the mouse button is pressed.
        if (mouse[mouseButton]) {
            // Ensure the mouse position is within the screen bounds.
            return mouseX >= 0 && mouseX <= windowWidth && mouseY >= 0 && mouseY <= windowHeight;
        }
        return false;
    }
    
    /**
     * Updates the input state by computing mouse movement deltas and storing the previous key and mouse states.
     * <p>
     * When the cursor is locked, the mouse position is reset to the center of the window after calculating the delta.
     * </p>
     */
    public static void update() {
        if (cursorLocked) {
            // Compute deltas relative to the center of the screen.
            deltaX = mouseX - windowWidth / 2.0;
            deltaY = mouseY - windowHeight / 2.0;
            
            // Reset the cursor to the center of the screen.
            glfwSetCursorPos(windowHandle, windowWidth / 2.0, windowHeight / 2.0);
            mouseX = windowWidth / 2.0;
            mouseY = windowHeight / 2.0;
        } else {
            // Compute deltas normally when the cursor is not locked.
            deltaX = mouseX - lastMouseX;
            deltaY = mouseY - lastMouseY;
            
            // Update the last mouse position.
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }
        
        // Update key and mouse button states.
        System.arraycopy(keys, 0, keysLast, 0, keys.length);
        System.arraycopy(mouse, 0, mouseLast, 0, mouse.length);
    }
}
