package engine.utils;

import engine.Component;
import engine.GameObject;

import java.util.HashSet;
import java.util.Set;

import static engine.Engine.activeScene;
import static engine.Engine.updateCount;

/**
 * The {@code Logger} class provides utility methods for logging various engine
 * details including performance statistics and game object hierarchies.
 */
public class Logger {
    /** A set to keep track of logged game objects to prevent duplicate logging. */
    private static Set<GameObject> loggedGameObjects = new HashSet<>();
    
    /**
     * Logs a simple message to standard output.
     *
     * @param log the message to log.
     */
    public static void log(String log) {
        System.out.println(log);
    }
    
    /**
     * Logs detailed information about the active scene including game object hierarchy,
     * component updates, and performance statistics.
     */
    public static void logStuff() {
        // Header for the log.
        System.out.println("------------------------------------------------");
        
        // Active scene and its contents.
        System.out.println(updateCount);
        System.out.println("🏠 " + activeScene.getName());
        
        // Clear the set once at the start of logging.
        loggedGameObjects.clear();
        
        // Log the entire hierarchy starting from the root game object.
        logGameObjectHierarchy(activeScene.getRootGameObject(), 0);
        
        // Separator for the update logs.
        System.out.println("     ⌨️");
        
        // Component update logs for each direct child of the root.
        for (GameObject gameObject : activeScene.rootGameObject.children) {
            for (Component component : gameObject.getComponents()) {
                System.out.println("        Updating " + component.getClass().getSimpleName() + " of: " + gameObject.getName());
            }
        }
    }
    
    /**
     * Logs performance statistics such as uptime, frame counts, and update counts.
     *
     * @param framesRenderedLastSecond the number of frames rendered in the last second.
     * @param updateCallsLastSecond      the number of update calls in the last second.
     * @param fixedUpdateCallsLastSecond the number of fixed update calls in the last second.
     * @param totalFrameCount            the total number of frames rendered.
     * @param uptime                     the total uptime in seconds.
     */
    public static void logPerformance(int framesRenderedLastSecond, int updateCallsLastSecond, int fixedUpdateCallsLastSecond, long totalFrameCount, float uptime) {
        System.out.println("===== Performance Stats =====");
        System.out.println("Uptime: " + uptime);
        System.out.println("Total Frames Rendered: " + totalFrameCount);
        System.out.println("Frames Rendered Last Second: " + framesRenderedLastSecond);
        System.out.println("Update Calls Last Second: " + updateCallsLastSecond);
        System.out.println("Fixed Update Calls Last Second: " + fixedUpdateCallsLastSecond);
        System.out.println("=============================");
    }
    
    /**
     * Recursively logs the game object hierarchy with indentation.
     *
     * @param gameObject       the current game object.
     * @param indentationLevel the level of indentation (number of spaces).
     */
    private static void logGameObjectHierarchy(GameObject gameObject, int indentationLevel) {
        // If the GameObject has already been logged, return early to prevent duplicates.
        if (loggedGameObjects.contains(gameObject)) {
            return;
        }
        
        // Mark the GameObject as logged.
        loggedGameObjects.add(gameObject);
        
        // Indentation based on the level of the hierarchy.
        String indentation = " ".repeat(indentationLevel);
        
        // Print the GameObject name.
        System.out.println(indentation + "🎭 " + gameObject.getName());
        
        // Iterate over each component of the GameObject.
        for (Component component : gameObject.getComponents()) {
            System.out.println(indentation + "    📝 " + component.getClass().getSimpleName());
        }
        
        // Recursively print each child GameObject.
        for (GameObject child : gameObject.children) {
            logGameObjectHierarchy(child, indentationLevel + 4);
        }
    }
    
    /**
     * Converts a {@link org.joml.Vector3f} to a string representation.
     *
     * @param v the vector.
     * @return the string representation in the format "(x, y, z)".
     */
    public static String toStringVector3(org.joml.Vector3f v) {
        return "(" + v.x + ", " + v.y + ", " + v.z + ")";
    }
}
