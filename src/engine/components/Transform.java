package engine.components;

import engine.Component;
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
        updateGlobalTransforms();
    }
    
    public void move(Vector3f direction, float distance)
    {
        Vector3f distanceToMove = new Vector3f(direction.x * distance, direction.y * distance, direction.z * distance);
        position.add(distanceToMove);
    }
    
    /**
     * Updates the global transforms based on the parent's transform.
     * This should be called whenever the local transform changes.
     */
    public void updateGlobalTransforms() {
        if (parent != null) {
            // First, scale the local position by the parent's global scale.
            // This applies a component-wise multiplication.
            Vector3f scaledLocalPosition = new Vector3f(position).mul(parent.globalScale);
            
            // Next, rotate the scaled local position by the parent's global rotation.
            Quaternionf parentQuat = new Quaternionf().rotateXYZ(
                    parent.globalRotation.x,
                    parent.globalRotation.y,
                    parent.globalRotation.z
            );
            Vector3f rotatedPosition = parentQuat.transform(scaledLocalPosition, new Vector3f());
            
            // Now, the global position is the parent's global position plus the rotated (and scaled) local offset.
            globalPosition.set(parent.globalPosition).add(rotatedPosition);
            
            // Combine rotations: global rotation = parent's rotation * local rotation.
            Quaternionf localQuat = new Quaternionf().rotateXYZ(rotation.x, rotation.y, rotation.z);
            Quaternionf globalQuat = parentQuat.mul(localQuat, new Quaternionf());
            globalRotation.set(globalQuat.getEulerAnglesXYZ(new Vector3f()));
            
            // The global scale is computed component-wise.
            globalScale.set(parent.globalScale).mul(scale);
        } else {
            // No parent means the local transform is the global transform.
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
    public Matrix4f getModelMatrix() {
        Matrix4f translationMatrix = new Matrix4f().translation(globalPosition);
        Matrix4f rotationMatrix = new Matrix4f().rotateXYZ(globalRotation.x, globalRotation.y, globalRotation.z);
        Matrix4f scaleMatrix = new Matrix4f().scaling(globalScale);
        
        // First, scale, then rotate, then translate
        return translationMatrix.mul(rotationMatrix).mul(scaleMatrix);
    }
    
    //Static directions
    public Vector3f right = new Vector3f(1,0,0);
    public Vector3f left =  new Vector3f(-1,0,0);
    public Vector3f up =    new Vector3f(0,1,0);
    public Vector3f down =  new Vector3f(0,-1,0);
    public Vector3f front = new Vector3f(0,0,1);
    public Vector3f back =  new Vector3f(0,0,-1);
    
    // Method to retrieve the transform's Up vector
    public Vector3f up()
    {
        Matrix3f rotationMatrix = new Matrix3f().rotateXYZ(rotation.x, rotation.y, rotation.z);
        Vector3f up = new Vector3f(0, 1, 0);
        rotationMatrix.transform(up);
        return up.normalize();
    }
    // Method to retrieve the transform's Up vector
    public Vector3f down()
    {
        return up().mul(-1);
    }
    
    // Method to retrieve the transform's Right vector
    public Vector3f right()
    {
        Matrix3f rotationMatrix = new Matrix3f().rotateXYZ(rotation.x, rotation.y, rotation.z);
        Vector3f right = new Vector3f(1, 0, 0);
        rotationMatrix.transform(right);
        return right.normalize();
    }
    // Method to retrieve the transform's Up vector
    public Vector3f left()
    {
        return right().mul(-1);
    }
    
    // Method to retrieve the transform's Forward vector
    public Vector3f front()
    {
        Matrix3f rotationMatrix = new Matrix3f().rotateXYZ(rotation.x, rotation.y, rotation.z);
        Vector3f forward = new Vector3f(0, 0, -1);
        rotationMatrix.transform(forward);
        return forward.normalize();
    }
    
    // Method to retrieve the transform's Up vector
    public Vector3f back()
    {
        return right().mul(-1);
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
