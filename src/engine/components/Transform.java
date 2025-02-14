package engine.components;

import engine.Component;
import engine.Engine;
import engine.utils.Logger;
import org.joml.*;

public class Transform extends Component
{
    public Vector3f position = new Vector3f(0);
    public Vector3f scale = new Vector3f(1);
    public Vector3f rotation = new Vector3f(0); // Stored as Euler angles, but rotations use quaternions
    
    // Global properties (calculated relative to the root)
    public Vector3f globalPosition = new Vector3f(0);
    public Vector3f globalScale = new Vector3f(1);
    public Vector3f globalRotation = new Vector3f(0);
    
    // Reference to the parent Transform (if applicable)
    private Transform parent;
    
    public Transform(Vector3f position)
    {
        this.position = new Vector3f(position);
        this.scale = new Vector3f(1);
        this.rotation = new Vector3f(0);
    }
    
    public Transform(Vector3f position, Vector3f scale)
    {
        this.position = new Vector3f(position);
        this.scale = new Vector3f(scale);
        this.rotation = new Vector3f(0);
    }
    
    public Transform(Vector3f position, Vector3f scale, Vector3f rotation)
    {
        this.position = new Vector3f(position);
        this.scale = new Vector3f(scale);
        this.rotation = new Vector3f(rotation);
    }
    
    // Method to set the parent transform
    public void setParent(Transform parent) {
        this.parent = parent;
    }
    
    /**
     * Updates the global transforms based on the parent's transform.
     * This should be called whenever the local transform changes.
     */
    public void updateGlobalTransforms() {
        if (parent != null) {
            // Global position: parent's global position + local position
            globalPosition.set(parent.globalPosition).add(position);
            
            // Global rotation: parent's global rotation * local rotation (in quaternion form)
            Quaternionf parentRotation = new Quaternionf().rotateXYZ(parent.globalRotation.x, parent.globalRotation.y, parent.globalRotation.z);
            Quaternionf localRotation = new Quaternionf().rotateXYZ(rotation.x, rotation.y, rotation.z);
            Quaternionf globalQuat = parentRotation.mul(localRotation);
            globalRotation.set(globalQuat.getEulerAnglesXYZ(new Vector3f()));
            
            // Global scale: parent's global scale * local scale
            globalScale.set(parent.globalScale).mul(scale);
        } else {
            // If there is no parent, the local transform is the global transform
            globalPosition.set(position);
            globalRotation.set(rotation);
            globalScale.set(scale);
        }
    }
    
    /**
     * Rotates the transform around an arbitrary axis.
     * Uses quaternions to avoid gimbal lock.
     * @param axis The axis to rotate around (must be normalized).
     * @param angle The angle in radians.
     */
    public void rotate(Vector3f axis, float angle)
    {
        // Create a quaternion representing the current rotation
        Quaternionf currentRotation = new Quaternionf()
                .rotateXYZ(rotation.x, rotation.y, rotation.z);
        
        // Create a quaternion for the new rotation around the given axis
        Quaternionf rotationQuat = new Quaternionf().rotateAxis(angle, axis);
        
        // Apply rotation
        currentRotation.mul(rotationQuat);
        
        // Convert the quaternion back to Euler angles for storage
        Vector3f newEuler = currentRotation.getEulerAnglesXYZ(new Vector3f());
        rotation.set(newEuler);
    }
    
    /**
     * Creates a transformation matrix based on position, rotation, and scaling.
     * @return The transformation matrix
     */
    public Matrix4f getTransformationMatrix() {
        Matrix4f translationMatrix = new Matrix4f().translation(position);
        Matrix4f rotationMatrix = new Matrix4f().rotateXYZ(rotation.x, rotation.y, rotation.z);
        Matrix4f scaleMatrix = new Matrix4f().scaling(scale);
        
        // First, scale, then rotate, then translate
        return translationMatrix.mul(rotationMatrix).mul(scaleMatrix);
    }
    
    // Method to retrieve the transform's Up vector
    public Vector3f getUp()
    {
        Matrix3f rotationMatrix = new Matrix3f().rotateXYZ(rotation.x, rotation.y, rotation.z);
        Vector3f up = new Vector3f(0, 1, 0);
        rotationMatrix.transform(up);
        return up.normalize();
    }
    
    // Method to retrieve the transform's Right vector
    public Vector3f getRight()
    {
        Matrix3f rotationMatrix = new Matrix3f().rotateXYZ(rotation.x, rotation.y, rotation.z);
        Vector3f right = new Vector3f(1, 0, 0);
        rotationMatrix.transform(right);
        return right.normalize();
    }
    
    // Method to retrieve the transform's Forward vector
    public Vector3f getForward()
    {
        Matrix3f rotationMatrix = new Matrix3f().rotateXYZ(rotation.x, rotation.y, rotation.z);
        Vector3f forward = new Vector3f(0, 0, -1);
        rotationMatrix.transform(forward);
        return forward.normalize();
    }
    
    @Override
    public String toString()
    {
        return
                "Position: " + Logger.toStringVector3(position) + "\n" +
                "Scale:    " + Logger.toStringVector3(scale) + "\n" +
                "Rotation: " + Logger.toStringVector3(rotation) + "\n" +
                "Global Position: " + Logger.toStringVector3(globalPosition) + "\n" +
                "Global Scale:    " + Logger.toStringVector3(globalScale) + "\n" +
                "Global Rotation: " + Logger.toStringVector3(globalRotation) + "\n";
    }
}
