package engine.utils;

import java.util.HashMap;

import static org.lwjgl.glfw.GLFW.*;

/**
 * The {@code KeyMapper} class provides a mapping between common key names and their GLFW key codes.
 * <p>
 * This utility enables using string identifiers (such as "up", "space", or "a") to access the corresponding key codes.
 * </p>
 */
public class KeyMapper {
    /** A mapping from key names to GLFW key codes. */
    private static final HashMap<String, Integer> keyNamesToCodes = new HashMap<>();
    
    static {
        // Initialize the HashMap with key names to GLFW key codes.
        keyNamesToCodes.put("up", GLFW_KEY_UP);
        keyNamesToCodes.put("down", GLFW_KEY_DOWN);
        keyNamesToCodes.put("left", GLFW_KEY_LEFT);
        keyNamesToCodes.put("right", GLFW_KEY_RIGHT);
        keyNamesToCodes.put("space", GLFW_KEY_SPACE);
        keyNamesToCodes.put("enter", GLFW_KEY_ENTER);
        keyNamesToCodes.put("escape", GLFW_KEY_ESCAPE);
        keyNamesToCodes.put("backspace", GLFW_KEY_BACKSPACE);
        keyNamesToCodes.put("tab", GLFW_KEY_TAB);
        keyNamesToCodes.put("caps_lock", GLFW_KEY_CAPS_LOCK);
        keyNamesToCodes.put("shift", GLFW_KEY_LEFT_SHIFT);
        keyNamesToCodes.put("ctrl", GLFW_KEY_LEFT_CONTROL);
        keyNamesToCodes.put("alt", GLFW_KEY_LEFT_ALT);
        keyNamesToCodes.put("pause", GLFW_KEY_PAUSE);
        keyNamesToCodes.put("page_up", GLFW_KEY_PAGE_UP);
        keyNamesToCodes.put("page_down", GLFW_KEY_PAGE_DOWN);
        keyNamesToCodes.put("home", GLFW_KEY_HOME);
        keyNamesToCodes.put("end", GLFW_KEY_END);
        keyNamesToCodes.put("insert", GLFW_KEY_INSERT);
        keyNamesToCodes.put("delete", GLFW_KEY_DELETE);
        keyNamesToCodes.put("f1", GLFW_KEY_F1);
        keyNamesToCodes.put("f2", GLFW_KEY_F2);
        keyNamesToCodes.put("f3", GLFW_KEY_F3);
        keyNamesToCodes.put("f4", GLFW_KEY_F4);
        keyNamesToCodes.put("f5", GLFW_KEY_F5);
        keyNamesToCodes.put("f6", GLFW_KEY_F6);
        keyNamesToCodes.put("f7", GLFW_KEY_F7);
        keyNamesToCodes.put("f8", GLFW_KEY_F8);
        keyNamesToCodes.put("f9", GLFW_KEY_F9);
        keyNamesToCodes.put("f10", GLFW_KEY_F10);
        keyNamesToCodes.put("f11", GLFW_KEY_F11);
        keyNamesToCodes.put("f12", GLFW_KEY_F12);
        keyNamesToCodes.put("num_lock", GLFW_KEY_NUM_LOCK);
        keyNamesToCodes.put("scroll_lock", GLFW_KEY_SCROLL_LOCK);
        keyNamesToCodes.put("left_shift", GLFW_KEY_LEFT_SHIFT);
        keyNamesToCodes.put("right_shift", GLFW_KEY_RIGHT_SHIFT);
        keyNamesToCodes.put("left_control", GLFW_KEY_LEFT_CONTROL);
        keyNamesToCodes.put("right_control", GLFW_KEY_RIGHT_CONTROL);
        keyNamesToCodes.put("left_alt", GLFW_KEY_LEFT_ALT);
        keyNamesToCodes.put("right_alt", GLFW_KEY_RIGHT_ALT);
        keyNamesToCodes.put("left_super", GLFW_KEY_LEFT_SUPER);
        keyNamesToCodes.put("right_super", GLFW_KEY_RIGHT_SUPER);
        keyNamesToCodes.put("menu", GLFW_KEY_MENU);
        keyNamesToCodes.put("num_0", GLFW_KEY_0);
        keyNamesToCodes.put("num_1", GLFW_KEY_1);
        keyNamesToCodes.put("num_2", GLFW_KEY_2);
        keyNamesToCodes.put("num_3", GLFW_KEY_3);
        keyNamesToCodes.put("num_4", GLFW_KEY_4);
        keyNamesToCodes.put("num_5", GLFW_KEY_5);
        keyNamesToCodes.put("num_6", GLFW_KEY_6);
        keyNamesToCodes.put("num_7", GLFW_KEY_7);
        keyNamesToCodes.put("num_8", GLFW_KEY_8);
        keyNamesToCodes.put("num_9", GLFW_KEY_9);
        keyNamesToCodes.put("a", GLFW_KEY_A);
        keyNamesToCodes.put("b", GLFW_KEY_B);
        keyNamesToCodes.put("c", GLFW_KEY_C);
        keyNamesToCodes.put("d", GLFW_KEY_D);
        keyNamesToCodes.put("e", GLFW_KEY_E);
        keyNamesToCodes.put("f", GLFW_KEY_F);
        keyNamesToCodes.put("g", GLFW_KEY_G);
        keyNamesToCodes.put("h", GLFW_KEY_H);
        keyNamesToCodes.put("i", GLFW_KEY_I);
        keyNamesToCodes.put("j", GLFW_KEY_J);
        keyNamesToCodes.put("k", GLFW_KEY_K);
        keyNamesToCodes.put("l", GLFW_KEY_L);
        keyNamesToCodes.put("m", GLFW_KEY_M);
        keyNamesToCodes.put("n", GLFW_KEY_N);
        keyNamesToCodes.put("o", GLFW_KEY_O);
        keyNamesToCodes.put("p", GLFW_KEY_P);
        keyNamesToCodes.put("q", GLFW_KEY_Q);
        keyNamesToCodes.put("r", GLFW_KEY_R);
        keyNamesToCodes.put("s", GLFW_KEY_S);
        keyNamesToCodes.put("t", GLFW_KEY_T);
        keyNamesToCodes.put("u", GLFW_KEY_U);
        keyNamesToCodes.put("v", GLFW_KEY_V);
        keyNamesToCodes.put("w", GLFW_KEY_W);
        keyNamesToCodes.put("x", GLFW_KEY_X);
        keyNamesToCodes.put("y", GLFW_KEY_Y);
        keyNamesToCodes.put("z", GLFW_KEY_Z);
        keyNamesToCodes.put("numpad_0", GLFW_KEY_KP_0);
        keyNamesToCodes.put("numpad_1", GLFW_KEY_KP_1);
        keyNamesToCodes.put("numpad_2", GLFW_KEY_KP_2);
        keyNamesToCodes.put("numpad_3", GLFW_KEY_KP_3);
        keyNamesToCodes.put("numpad_4", GLFW_KEY_KP_4);
        keyNamesToCodes.put("numpad_5", GLFW_KEY_KP_5);
        keyNamesToCodes.put("numpad_6", GLFW_KEY_KP_6);
        keyNamesToCodes.put("numpad_7", GLFW_KEY_KP_7);
        keyNamesToCodes.put("numpad_8", GLFW_KEY_KP_8);
        keyNamesToCodes.put("numpad_9", GLFW_KEY_KP_9);
        keyNamesToCodes.put("numpad_add", GLFW_KEY_KP_ADD);
        keyNamesToCodes.put("numpad_subtract", GLFW_KEY_KP_SUBTRACT);
        keyNamesToCodes.put("numpad_multiply", GLFW_KEY_KP_MULTIPLY);
        keyNamesToCodes.put("numpad_divide", GLFW_KEY_KP_DIVIDE);
        keyNamesToCodes.put("numpad_enter", GLFW_KEY_KP_ENTER);
        keyNamesToCodes.put("numpad_decimal", GLFW_KEY_KP_DECIMAL);
    }
    
    /**
     * Retrieves the GLFW key code corresponding to the given key name.
     *
     * @param keyName the name of the key.
     * @return the key code, or {@code null} if the key name is not mapped.
     */
    public static Integer getKeyCode(String keyName) {
        return keyNamesToCodes.get(keyName);
    }
}
