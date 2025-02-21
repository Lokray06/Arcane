package engine;

import engine.utils.KeyMapper;
import imgui.ImGui;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;

import static org.lwjgl.glfw.GLFW.*;

/**
 * The Input class handles user input for keyboard and mouse.
 * It sets up GLFW callbacks and provides methods to query the current input state.
 *
 * In order to avoid conflicts with ImGui’s input handling, the update() method
 * checks if ImGui wants to capture keyboard or mouse input.
 *
 * The callbacks are chained with any previously installed ones (e.g. from ImGuiImplGlfw)
 * so that both systems receive the events.
 */
public class Input {
    
    private static boolean[] keys = new boolean[GLFW_KEY_LAST];
    private static boolean[] keysLast = new boolean[GLFW_KEY_LAST];
    private static boolean[] mouse = new boolean[GLFW_MOUSE_BUTTON_LAST];
    private static boolean[] mouseLast = new boolean[GLFW_MOUSE_BUTTON_LAST];
    private static double mouseX, mouseY;
    private static double lastMouseX, lastMouseY;
    private static double deltaX = 0, deltaY = 0;
    
    private static boolean cursorLocked = false;
    private static long windowHandle;
    private static int windowWidth;
    private static int windowHeight;
    
    public static void init(long windowHandle, int windowWidth, int windowHeight) {
        Input.windowHandle = windowHandle;
        Input.windowWidth = windowWidth;
        Input.windowHeight = windowHeight;
        
        // --- Chain the key callback ---
        GLFWKeyCallback prevKeyCb = glfwSetKeyCallback(windowHandle, null);
        GLFWKeyCallback myKeyCb = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key >= 0 && key < GLFW_KEY_LAST) {
                    keys[key] = action != GLFW_RELEASE;
                }
                if (prevKeyCb != null) {
                    prevKeyCb.invoke(window, key, scancode, action, mods);
                }
            }
        };
        glfwSetKeyCallback(windowHandle, myKeyCb);
        
        // --- Chain the mouse button callback ---
        GLFWMouseButtonCallback prevMouseButtonCb = glfwSetMouseButtonCallback(windowHandle, null);
        GLFWMouseButtonCallback myMouseButtonCb = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                if (button >= 0 && button < GLFW_MOUSE_BUTTON_LAST) {
                    mouse[button] = action != GLFW_RELEASE;
                }
                if (prevMouseButtonCb != null) {
                    prevMouseButtonCb.invoke(window, button, action, mods);
                }
            }
        };
        glfwSetMouseButtonCallback(windowHandle, myMouseButtonCb);
        
        // --- Chain the cursor position callback ---
        GLFWCursorPosCallback prevCursorPosCb = glfwSetCursorPosCallback(windowHandle, null);
        GLFWCursorPosCallback myCursorPosCb = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                mouseX = xpos;
                mouseY = ypos;
                if (prevCursorPosCb != null) {
                    prevCursorPosCb.invoke(window, xpos, ypos);
                }
            }
        };
        glfwSetCursorPosCallback(windowHandle, myCursorPosCb);
    }
    
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
    
    public static boolean getButton(int button) {
        return mouse[button];
    }
    
    public static boolean getButtonDown(int button) {
        return mouse[button] && !mouseLast[button];
    }
    
    public static boolean getButtonUp(int button) {
        return !mouse[button] && mouseLast[button];
    }
    
    public static double getMouseDeltaX() {
        return deltaX;
    }
    public static void setMouseDeltaX(double deltaX) {
        Input.deltaX = deltaX;
    }
    
    public static double getMouseDeltaY() {
        return deltaY;
    }
    public static void setMouseDeltaY(double deltaY) {
        Input.deltaY = deltaY;
    }
    
    public static double getMouseX() {
        return mouseX;
    }
    
    public static double getMouseY() {
        return mouseY;
    }
    
    public static void setCursorLocked(boolean locked) {
        cursorLocked = locked;
        if (locked) {
            glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        } else {
            glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        }
    }
    
    public static boolean isCursorLocked() {
        return cursorLocked;
    }
    
    public static boolean isMouseClickInBounds(int mouseButton) {
        if (mouse[mouseButton]) {
            return mouseX >= 0 && mouseX <= windowWidth && mouseY >= 0 && mouseY <= windowHeight;
        }
        return false;
    }
    
    /**
     * Updates the input state.
     * If ImGui is capturing keyboard or mouse input, the engine input update is skipped
     * to avoid conflicts.
     */
    public static void update() {
        // Update previous states.
        System.arraycopy(keys, 0, keysLast, 0, keys.length);
        System.arraycopy(mouse, 0, mouseLast, 0, mouse.length);
        
        // Check if ImGui is capturing mouse input.
        if (!ImGui.getIO().getWantCaptureMouse()) {
            if (cursorLocked) {
                deltaX = mouseX - windowWidth / 2.0;
                deltaY = mouseY - windowHeight / 2.0;
                glfwSetCursorPos(windowHandle, windowWidth / 2.0, windowHeight / 2.0);
                mouseX = windowWidth / 2.0;
                mouseY = windowHeight / 2.0;
            } else {
                deltaX = mouseX - lastMouseX;
                deltaY = mouseY - lastMouseY;
                lastMouseX = mouseX;
                lastMouseY = mouseY;
            }
        } else {
            // If ImGui is handling the mouse, reset engine delta values.
            deltaX = 0;
            deltaY = 0;
        }
    }
}
