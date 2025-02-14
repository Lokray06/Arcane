package engine.utils;

import engine.Component;
import engine.GameObject;
import static engine.Engine.updateCount;
import java.util.HashSet;
import java.util.Set;

public class Logger
{
    private static Set<GameObject> loggedGameObjects = new HashSet<>();

    public static void log(String log)
    {
        System.out.println(log);
    }

    public static void logStuff()
    {
        // Header for the log
        System.out.println("------------------------------------------------");

        // Active scene and its contents
        System.out.println(updateCount);
        System.out.println("🏠 " + engine.Engine.activeScene.getName());

        // Call the recursive function to log the hierarchy
        for(GameObject gameObject : engine.Engine.activeScene.getGameObjects())
        {
            loggedGameObjects.clear(); // Clear the set at the start of each log
            logGameObjectHierarchy(gameObject, 2); // Starting from 2 spaces indentation for the first level
        }

        // Separator for the update logs
        System.out.println("     ⌨️");

        // Component update logs
        for(GameObject gameObject : engine.Engine.activeScene.rootGameObject.children)
        {
            for(Component component : gameObject.getComponents())
            {
                System.out.println("        Updating " + component.getClass().getSimpleName() + " of: " + gameObject.getName());
            }
        }
    }

    // New method to log performance statistics.
    public static void logPerformance(int framesRenderedLastSecond, int updateCallsLastSecond, int fixedUpdateCallsLastSecond, long totalFrameCount, float uptime)
    {
        System.out.println("===== Performance Stats =====");
        System.out.println("Uptime: " + uptime);
        System.out.println("Total Frames Rendered: " + totalFrameCount);
        System.out.println("Frames Rendered Last Second: " + framesRenderedLastSecond);
        System.out.println("Update Calls Last Second: " + updateCallsLastSecond);
        System.out.println("Fixed Update Calls Last Second: " + fixedUpdateCallsLastSecond);
        System.out.println("=============================");
    }

    // Helper method to log a GameObject and its children recursively with indentation
    private static void logGameObjectHierarchy(GameObject gameObject, int indentationLevel)
    {
        // If the GameObject has already been logged, return early to prevent duplicates
        if(loggedGameObjects.contains(gameObject))
        {
            return;
        }

        // Mark the GameObject as logged
        loggedGameObjects.add(gameObject);

        // Indentation based on the level of the hierarchy
        String indentation = " ".repeat(indentationLevel);

        // Print the GameObject name
        System.out.println(indentation + "🎭 " + gameObject.getName());

        // Iterate over each component of the GameObject
        for(Component component : gameObject.getComponents())
        {
            System.out.println(indentation + "    📝 " + component.getClass().getSimpleName()); // Component type (name)
        }

        // Recursively print each child GameObject
        for(GameObject child : gameObject.children)
        {
            logGameObjectHierarchy(child, indentationLevel + 4); // Increase indentation for each child
        }
    }

    public static String toStringVector3(org.joml.Vector3f v)
    {
        return "(" + v.x + ", " + v.y + ", " + v.z + ")";
    }
}
