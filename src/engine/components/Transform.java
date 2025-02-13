package engine.components;

import engine.Component;
import engine.utils.Logger;
import org.joml.*;

public class Transform extends Component
{
    public Vector3f position = new Vector3f(0);
    public Vector3f scale = new Vector3f(1);
    public Vector3f rotation = new Vector3f(0); // Stored as Euler angles, but rotations use quaternions
    
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
    
    public Vector3f getUp()
    {
        Matrix3f rotationMatrix = new Matrix3f().rotateXYZ(rotation.x, rotation.y, rotation.z);
        Vector3f up = new Vector3f(0, 1, 0);
        rotationMatrix.transform(up);
        return up.normalize();
    }
    
    public Vector3f getRight()
    {
        Matrix3f rotationMatrix = new Matrix3f().rotateXYZ(rotation.x, rotation.y, rotation.z);
        Vector3f right = new Vector3f(1, 0, 0);
        rotationMatrix.transform(right);
        return right.normalize();
    }
    
    public Vector3f getForward()
    {
        Matrix3f rotationMatrix = new Matrix3f().rotateXYZ(rotation.x, rotation.y, rotation.z);
        Vector3f forward = new Vector3f(0, 0, 1);
        rotationMatrix.transform(forward);
        return forward.normalize();
    }
    
    @Override
    public String toString()
    {
        return
                "Position: " + Logger.toStringVector3(position) + "\n" +
                "Scale:    " + Logger.toStringVector3(scale) + "\n" +
                "Rotation: " + Logger.toStringVector3(rotation) + "\n";
    }
}
