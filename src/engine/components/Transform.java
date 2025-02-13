package engine.components;

import engine.Component;
import engine.utils.Logger;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public class Transform extends Component
{
    public Vector3f position = new Vector3f(0);
    public Vector3f scale = new Vector3f(1);
    public Vector3f rotation = new Vector3f(0);
    
    public Transform(Vector3f position)
    {
        this.position = new Vector3f(position);
        this.scale = new Vector3f(1);
        this.rotation = new Vector3f(0, 0, 0);
    }
    
    public Transform(Vector3f position, Vector3f scale)
    {
        this.position = new Vector3f(position);
        this.scale = new Vector3f(scale);
        this.rotation = new Vector3f(0, 0, 0);
    }
    
    public Transform(Vector3f position, Vector3f scale, Vector3f rotation)
    {
        this.position = new Vector3f(position);
        this.scale = new Vector3f(scale);
        this.rotation = new Vector3f(rotation);
    }
    
    /**
     * Creates a transformation matrix based on position, rotation, and scaling.
     *
     * @return The transformation matrix
     */
    public Matrix4f getTransformationMatrix() {
        Matrix4f translationMatrix = new Matrix4f().translation(position);
        Matrix4f rotationMatrix = new Matrix4f().rotateX(rotation.x).rotateY(rotation.y).rotateZ(rotation.z);
        Matrix4f scaleMatrix = new Matrix4f().scaling(scale);
        
        // First, scale, then rotate, then translate
        return translationMatrix.mul(rotationMatrix).mul(scaleMatrix);
    }
    
    public Vector3f getUp()
    {
        // Create a rotation matrix from Euler angles
        Matrix3f rotationMatrix = new Matrix3f().rotateX(rotation.x).rotateY(rotation.y).rotateZ(rotation.z);
        
        // Apply the rotation to the up vector
        Vector3f up = new Vector3f(0, 1, 0);
        rotationMatrix.transform(up);
        
        return up.normalize();
    }
    
    public Vector3f getForward()
    {
        // Create a rotation matrix from Euler angles
        Matrix3f rotationMatrix = new Matrix3f().rotateX(rotation.x).rotateY(rotation.y).rotateZ(rotation.z);
        
        // Apply the rotation to the up vector
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
