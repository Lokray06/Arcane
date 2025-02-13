package engine.utils;

import engine.GameObject;
import engine.components.Transform;

public class TransformManager
{
    public static void updateTransforms(GameObject gameObject) {
        // Calculate the global transform for this GameObject
        if (gameObject.parent != null) {
            // Combine parent's transform with this GameObject's local transform
            gameObject.transform = new Transform(
                    gameObject.parent.transform.position.add(gameObject.transform.position),
                    gameObject.parent.transform.rotation.add(gameObject.transform.rotation),
                    gameObject.parent.transform.scale.mul(gameObject.transform.scale)
            );
        }
        
        // Recursively update all children
        for (GameObject child : gameObject.children) {
            updateTransforms(child);
        }
    }
}
