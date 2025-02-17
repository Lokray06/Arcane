package engine.components;

import engine.Component;
import engine.utils.Logger;
import org.joml.*;

/**
 * The {@code Transform} class represents the position, rotation, and scale of a game object.
 * <p>
 * It maintains both local and global transforms, allowing for hierarchical transformations.
 * Global transforms are updated based on the parent's transform.
 * </p>
 */
public class Transform extends Component {
    /** Local position of the transform. */
    public Vector3f position = new Vector3f(0);
    /** Local scale of the transform. */
    public Vector3f scale = new Vector3f(1);
    /** Local Euler angles for rotation (in radians). */
    public Vector3f rotation = new Vector3f(0);
    
    // Global properties (calculated relative to the root)
    /** Global position computed from the local position and parent's transform. */
    public Vector3f globalPosition = new Vector3f(0);
    /** Global scale computed from the local scale and parent's scale. */
    public Vector3f globalScale = new Vector3f(1);
    /** Global rotation (Euler angles) computed from local and parent's rotations. */
    public Vector3f globalRotation = new Vector3f(0);
    
    /** Global rotation stored as a quaternion for correct composition. */
    public Quaternionf globalRotationQuat = new Quaternionf();
    
    /** Reference to the parent transform, if any. */
    private Transform parent;
    
    /**
     * Constructs a Transform with the specified position.
     *
     * @param position the initial position.
     */
    public Transform(Vector3f position) {
        this.position = new Vector3f(position);
        this.scale = new Vector3f(1);
        this.rotation = new Vector3f(0);
    }
    
    /**
     * Constructs a Transform with the specified position and scale.
     *
     * @param position the initial position.
     * @param scale    the initial scale.
     */
    public Transform(Vector3f position, Vector3f scale) {
        this.position = new Vector3f(position);
        this.scale = new Vector3f(scale);
        this.rotation = new Vector3f(0);
    }
    
    /**
     * Constructs a Transform with the specified position, scale, and rotation.
     *
     * @param position the initial position.
     * @param scale    the initial scale.
     * @param rotation the initial rotation (Euler angles in radians).
     */
    public Transform(Vector3f position, Vector3f scale, Vector3f rotation) {
        this.position = new Vector3f(position);
        this.scale = new Vector3f(scale);
        this.rotation = new Vector3f(rotation);
    }
    
    /**
     * Sets the parent transform.
     * <p>
     * No immediate update is performed; the hierarchy will be updated from the root.
     * </p>
     *
     * @param parent the parent transform.
     */
    public void setParent(Transform parent) {
        this.parent = parent;
    }
    
    /**
     * Moves the transform in the given direction by the specified distance.
     *
     * @param direction the direction vector.
     * @param distance  the distance to move.
     */
    public void move(Vector3f direction, float distance) {
        Vector3f distanceToMove = new Vector3f(direction).mul(distance);
        position.add(distanceToMove);
    }
    
    /**
     * Updates the global transforms (position, scale, and rotation) based on the parent's transform.
     */
    public void updateGlobalTransforms() {
        // Convert the local Euler rotation to a quaternion.
        Quaternionf localQuat = new Quaternionf().rotateXYZ(rotation.x, rotation.y, rotation.z);
        
        if (parent != null) {
            // Compute global scale: parent's global scale * local scale (component-wise)
            globalScale.set(parent.globalScale).mul(scale);
            
            // Compute global rotation: parent's global rotation (as quaternion) * local rotation.
            globalRotationQuat.set(parent.globalRotationQuat).mul(localQuat);
            globalRotation.set(globalRotationQuat.getEulerAnglesXYZ(new Vector3f()));
            
            // Compute global position without scaling the local position.
            Vector3f rotatedPos = new Vector3f();
            parent.globalRotationQuat.transform(new Vector3f(position), rotatedPos);
            globalPosition.set(parent.globalPosition).add(rotatedPos);
        } else {
            // No parent – local transform is the global transform.
            globalPosition.set(position);
            globalScale.set(scale);
            globalRotationQuat.set(localQuat);
            globalRotation.set(rotation);
        }
    }
    
    /**
     * Rotates the transform around an arbitrary axis using quaternions to avoid gimbal lock.
     *
     * @param axis  the normalized axis to rotate around.
     * @param angle the angle in radians.
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
     * Creates and returns a model matrix based on the global position, rotation, and scale.
     *
     * @return the model transformation matrix.
     */
    public Matrix4f getModelMatrix() {
        Matrix4f model = new Matrix4f().identity();
        // Translate, then rotate (using the quaternion), then scale.
        model.translate(globalPosition);
        model.rotate(globalRotationQuat);
        model.scale(globalScale);
        return model;
    }
    
    // Static direction vectors.
    public Vector3f right = new Vector3f(1, 0, 0);
    public Vector3f left  = new Vector3f(-1, 0, 0);
    public Vector3f up    = new Vector3f(0, 1, 0);
    public Vector3f down  = new Vector3f(0, -1, 0);
    public Vector3f front = new Vector3f(0, 0, 1);
    public Vector3f back  = new Vector3f(0, 0, -1);
    
    /**
     * Returns the transformed up vector based on the global rotation.
     *
     * @return the normalized up vector.
     */
    public Vector3f up() {
        Vector3f upVec = new Vector3f(0, 1, 0);
        globalRotationQuat.transform(upVec);
        return upVec.normalize();
    }
    
    /**
     * Returns the transformed down vector (negated up vector).
     *
     * @return the normalized down vector.
     */
    public Vector3f down() {
        return new Vector3f(up()).negate();
    }
    
    /**
     * Returns the transformed right vector based on the global rotation.
     *
     * @return the normalized right vector.
     */
    public Vector3f right() {
        Vector3f rightVec = new Vector3f(1, 0, 0);
        globalRotationQuat.transform(rightVec);
        return rightVec.normalize();
    }
    
    /**
     * Returns the transformed left vector (negated right vector).
     *
     * @return the normalized left vector.
     */
    public Vector3f left() {
        return new Vector3f(right()).negate();
    }
    
    /**
     * Returns the transformed forward vector based on the global rotation.
     *
     * @return the normalized forward vector.
     */
    public Vector3f front() {
        Vector3f forward = new Vector3f(0, 0, -1);
        globalRotationQuat.transform(forward);
        return forward.normalize();
    }
    
    /**
     * Returns the transformed back vector (negated forward vector).
     *
     * @return the normalized back vector.
     */
    public Vector3f back() {
        return new Vector3f(front()).negate();
    }
    
    /**
     * Returns a string representation of the transform, including local and global properties.
     *
     * @return a formatted string describing the transform.
     */
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
