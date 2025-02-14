package engine.utils;

import engine.GameObject;

public class TransformManager
{
    public static void updateTransforms(GameObject gameObject) {
        // Call the update method to ensure global transforms are up-to-date
        gameObject.transform.updateGlobalTransforms();
        
        // Update children
        for (GameObject child : gameObject.children) {
            updateTransforms(child);
        }
    }
}
