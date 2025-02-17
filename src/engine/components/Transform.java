package engine.components;

import engine.Component;
import engine.utils.Logger;
import org.joml.*;

public class Transform extends Component {
    public Vector3f position = new Vector3f(0);
    public Vector3f scale = new Vector3f(1);
    public Vector3f rotation = new Vector3f(0); // Local Euler angles
    
    // Global properties (calculated relative to the root)
    public Vector3f globalPosition = new Vector3f(0);
    public Vector3f globalScale = new Vector3f(1);
    public Vector3f globalRotation = new Vector3f(0); // For display/logging (Euler angles)
    
    // Store the global rotation as a quaternion for correct composition.
    public Quaternionf globalRotationQuat = new Quaternionf();
    
    // Reference to the parent Transform (if applicable)
    private Transform parent;
    
    public Transform(Vector3f position) {
        this.position = new Vector3f(position);
        this.scale = new Vector3f(1);
        this.rotation = new Vector3f(0);
    }
    
    public Transform(Vector3f position, Vector3f scale) {
        this.position = new Vector3f(position);
        this.scale = new Vector3f(scale);
        this.rotation = new Vector3f(0);
    }
    
    public Transform(Vector3f position, Vector3f scale, Vector3f rotation) {
        this.position = new Vector3f(position);
        this.scale = new Vector3f(scale);
        this.rotation = new Vector3f(rotation);
    }
    
    // Set the parent transform. (No immediate update here; the hierarchy will be updated from the root.)
    public void setParent(Transform parent) {
        this.parent = parent;
    }
    
    public void move(Vector3f direction, float distance) {
        Vector3f distanceToMove = new Vector3f(direction).mul(distance);
        position.add(distanceToMove);
    }
    
    /**
     * Updates the global transforms based on the parent's transform.
     * This version assumes the parent's global transform is already up-to-date.
     */
    public void updateGlobalTransforms() {
        // Convert the local Euler rotation to a quaternion.
        Quaternionf localQuat = new Quaternionf().rotateXYZ(rotation.x, rotation.y, rotation.z);
        
        if (parent != null) {
            // Compute global scale: parent's global scale * local scale (component-wise)
            globalScale.set(parent.globalScale).mul(scale);
            
            // Compute global rotation: parent's global rotation (as quaternion) * local rotation
            globalRotationQuat.set(parent.globalRotationQuat).mul(localQuat);
            globalRotation.set(globalRotationQuat.getEulerAnglesXYZ(new Vector3f()));
            
            // Compute global position without scaling the local position:
            Vector3f rotatedPos = new Vector3f();
            parent.globalRotationQuat.transform(new Vector3f(position), rotatedPos);
            globalPosition.set(parent.globalPosition).add(rotatedPos);
        } else {
            // No parent – local transform is global transform.
            globalPosition.set(position);
            globalScale.set(scale);
            globalRotationQuat.set(localQuat);
            globalRotation.set(rotation);
        }
    }
    
    
    /**
     * Rotates the transform around an arbitrary axis.
     * Uses quaternions to avoid gimbal lock.
     * @param axis The axis to rotate around (must be normalized).
     * @param angle The angle in radians.
     */
    public void rotate(Vector3f axis, float angle) {
        // Create a quaternion for the rotation around the given axis.
        Quaternionf rotationQuat = new Quaternionf().rotateAxis(angle, axis);
        // Convert the current local Euler rotation to a quaternion.
        Quaternionf localQuat = new Quaternionf().rotateXYZ(rotation.x, rotation.y, rotation.z);
        // Compose the new rotation.
        localQuat.mul(rotationQuat);
        // Update the local Euler angles from the resulting quaternion.
        Vector3f newEuler = localQuat.getEulerAnglesXYZ(new Vector3f());
        rotation.set(newEuler);
    }
    
    /**
     * Creates a transformation matrix based on global position, rotation, and scale.
     * @return The transformation matrix.
     */
    public Matrix4f getModelMatrix() {
        Matrix4f model = new Matrix4f().identity();
        // Translate, then rotate (using the quaternion), then scale.
        model.translate(globalPosition);
        model.rotate(globalRotationQuat);
        model.scale(globalScale);
        return model;
    }
    
    // Static direction vectors
    public Vector3f right = new Vector3f(1, 0, 0);
    public Vector3f left  = new Vector3f(-1, 0, 0);
    public Vector3f up    = new Vector3f(0, 1, 0);
    public Vector3f down  = new Vector3f(0, -1, 0);
    public Vector3f front = new Vector3f(0, 0, 1);
    public Vector3f back  = new Vector3f(0, 0, -1);
    
    // Methods to get transformed direction vectors using the global rotation.
    public Vector3f up() {
        Vector3f upVec = new Vector3f(0, 1, 0);
        globalRotationQuat.transform(upVec);
        return upVec.normalize();
    }
    
    public Vector3f down() {
        return new Vector3f(up()).negate();
    }
    
    public Vector3f right() {
        Vector3f rightVec = new Vector3f(1, 0, 0);
        globalRotationQuat.transform(rightVec);
        return rightVec.normalize();
    }
    
    public Vector3f left() {
        return new Vector3f(right()).negate();
    }
    
    public Vector3f front() {
        Vector3f forward = new Vector3f(0, 0, -1);
        globalRotationQuat.transform(forward);
        return forward.normalize();
    }
    
    public Vector3f back() {
        return new Vector3f(front()).negate();
    }
    
    @Override
    public String toString() {
        return "Position: " + Logger.toStringVector3(position) + "\n" +
               "Scale:    " + Logger.toStringVector3(scale) + "\n" +
               "Rotation: " + Logger.toStringVector3(rotation) + "\n" +
               "Global Position: " + Logger.toStringVector3(globalPosition) + "\n" +
               "Global Scale:    " + Logger.toStringVector3(globalScale) + "\n" +
               "Global Rotation (Euler): " + Logger.toStringVector3(globalRotation) + "\n";
    }
}
