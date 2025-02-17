package engine.utils;

import engine.GameObject;

/**
 * The {@code TransformManager} class provides a method to recursively update
 * the global transforms of a game object hierarchy.
 */
public class TransformManager {
    /**
     * Recursively updates the global transforms of the given game object and its children.
     *
     * @param gameObject the root game object to update.
     */
    public static void updateTransforms(GameObject gameObject) {
        // Call the update method to ensure global transforms are up-to-date.
        gameObject.transform.updateGlobalTransforms();
        for (GameObject child : gameObject.children) {
            updateTransforms(child);
        }
    }
}
