package engine.utils;

import engine.GameObject;
import engine.components.Transform;
import org.joml.*;

public class TransformManager
{
    public static void updateTransforms(GameObject gameObject) {
        Transform transform = gameObject.transform;
        
        if (gameObject.parent != null) {
            Transform parentTransform = gameObject.parent.transform;
            
            // Compute global position
            transform.globalPosition.set(parentTransform.globalPosition).add(transform.position);
            
            // Compute global rotation
            Quaternionf parentQuat = new Quaternionf().rotateXYZ(parentTransform.globalRotation.x, parentTransform.globalRotation.y, parentTransform.globalRotation.z);
            Quaternionf localQuat = new Quaternionf().rotateXYZ(transform.rotation.x, transform.rotation.y, transform.rotation.z);
            Quaternionf globalQuat = new Quaternionf(parentQuat).mul(localQuat);
            globalQuat.getEulerAnglesXYZ(transform.globalRotation);
            
            // Compute global scale
            transform.globalScale.set(parentTransform.globalScale).mul(transform.scale);
        } else {
            // Root object: global properties equal local properties
            transform.globalPosition.set(transform.position);
            transform.globalRotation.set(transform.rotation);
            transform.globalScale.set(transform.scale);
        }
        
        // Update children
        for (GameObject child : gameObject.children) {
            updateTransforms(child);
        }
    }
    
}