package engine;

import engine.meshTypes.MeshGLTF;
import engine.meshTypes.MeshOBJ;
import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * Abstract base class for all mesh types.
 * It encapsulates common mesh data and behavior.
 */
public abstract class Mesh {
    protected Vector3f[] vertices;
    protected Vector3f[] normals;
    protected Vector2f[] uvs;
    protected int[][][] faces;
    public String meshName;
    
    /**
     * Protected no-argument constructor for subclasses.
     */
    protected Mesh() { }
    
    /**
     * Static factory method that loads a mesh from a file path.
     * It returns a concrete Mesh instance (MeshGLTF or MeshOBJ) based on the file extension.
     *
     * @param filePath the path to the mesh file.
     * @return a Mesh instance of the appropriate type.
     * @throws IllegalArgumentException if the file extension is not supported.
     */
    public static Mesh load(String filePath) {
        String lowerPath = filePath.toLowerCase();
        if (lowerPath.endsWith(".gltf")) {
            return new MeshGLTF(filePath);
        } else if (lowerPath.endsWith(".obj")) {
            return new MeshOBJ(filePath);
        } else {
            throw new IllegalArgumentException("Unsupported mesh file extension: " + filePath);
        }
    }
    
    /**
     * Initializes the mesh (e.g. by creating OpenGL buffers).
     */
    public abstract void initMesh();
    
    /**
     * Renders the mesh.
     */
    public abstract void render();
    
    /**
     * Cleans up any allocated resources.
     */
    public abstract void cleanup();
    
    @Override
    public String toString() {
        return "Mesh: " + (meshName != null ? meshName : "Unnamed") +
               "\nVertices: " + (vertices != null ? vertices.length : 0) +
               "\nTriangles: " + (faces != null ? faces.length : 0);
    }
}
