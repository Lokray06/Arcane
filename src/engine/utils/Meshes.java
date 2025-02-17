package engine.utils;

import engine.Mesh;
import org.joml.Vector3f;

/**
 * The {@code Meshes} class provides utility methods to create common meshes.
 * <p>
 * For example, it includes a method to create a skybox mesh.
 * </p>
 */
public class Meshes {
    /**
     * Creates a cube mesh to be used as a skybox.
     *
     * @return the skybox {@link Mesh} with initialized vertex and face data.
     */
    public static Mesh createSkybox() {
        // Cube vertices for skybox.
        Vector3f[] vertices = new Vector3f[]{
                new Vector3f(-1.0f,  1.0f, -1.0f),
                new Vector3f(-1.0f, -1.0f, -1.0f),
                new Vector3f( 1.0f, -1.0f, -1.0f),
                new Vector3f( 1.0f,  1.0f, -1.0f),
                new Vector3f(-1.0f,  1.0f,  1.0f),
                new Vector3f(-1.0f, -1.0f,  1.0f),
                new Vector3f( 1.0f, -1.0f,  1.0f),
                new Vector3f( 1.0f,  1.0f,  1.0f),
        };
        
        // Cube indices (12 triangles).
        int[][][] faces = new int[][][]{
                {{0, 0, 0}, {1, 0, 0}, {2, 0, 0}}, {{2, 0, 0}, {3, 0, 0}, {0, 0, 0}}, // Front
                {{4, 0, 0}, {5, 0, 0}, {6, 0, 0}}, {{6, 0, 0}, {7, 0, 0}, {4, 0, 0}}, // Back
                {{0, 0, 0}, {4, 0, 0}, {7, 0, 0}}, {{7, 0, 0}, {3, 0, 0}, {0, 0, 0}}, // Top
                {{1, 0, 0}, {5, 0, 0}, {6, 0, 0}}, {{6, 0, 0}, {2, 0, 0}, {1, 0, 0}}, // Bottom
                {{0, 0, 0}, {1, 0, 0}, {5, 0, 0}}, {{5, 0, 0}, {4, 0, 0}, {0, 0, 0}}, // Left
                {{3, 0, 0}, {2, 0, 0}, {6, 0, 0}}, {{6, 0, 0}, {7, 0, 0}, {3, 0, 0}}, // Right
        };
        
        // Create and return the Mesh.
        Mesh skyboxMesh = new Mesh(vertices, faces);
        skyboxMesh.initMesh();
        return skyboxMesh;
    }
}
