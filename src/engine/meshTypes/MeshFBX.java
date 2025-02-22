package engine.meshTypes;

import engine.Mesh;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

/**
 * Represents a 3D mesh, typically loaded from an OBJ file.
 * <p>
 * A Mesh contains vertex data, normals, texture coordinates (UVs), and face definitions.
 * It also manages OpenGL buffers for rendering.
 * </p>
 */
public class MeshFBX extends Mesh
{
    /** Array of vertex positions. */
    public Vector3f[] vertices;
    /** Array of vertex normals. */
    public Vector3f[] normals;
    /** Array of texture coordinates. */
    public Vector2f[] uvs;
    /**
     * 3D array representing faces.
     * <p>
     * Each face is an array of vertices, where each vertex is represented by an array of indices:
     * [vertex index, uv index, normal index].
     * </p>
     */
    public int[][][] faces;
    /** Name of the mesh. */
    public String meshName;

    // OpenGL handles.
    private int vaoId;
    private int vboId;
    private int eboId;
    private boolean initialized = false;

    private float[] interleavedVertexData;
    private int[] indices;

    /**
     * Constructs a Mesh by loading an OBJ file.
     *
     * @param path the path to the OBJ file.
     */
    public MeshFBX(String path) {
        super();
        List<Vector3f> verticesList = new ArrayList<>();
        List<Vector3f> normalsList = new ArrayList<>();
        List<Vector2f> uvsList = new ArrayList<>();
        List<int[][]> facesList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            Pattern pattern = Pattern.compile("^(o|v|vn|vt|f) (.+)$");
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;
                Matcher matcher = pattern.matcher(line);

                if (matcher.find()) {
                    String type = matcher.group(1);
                    String data = matcher.group(2);

                    switch (type) {
                        case "o":
                            this.meshName = data;
                            break;
                        case "v":
                            String[] vParts = data.split("\\s+");
                            verticesList.add(new Vector3f(
                                    Float.parseFloat(vParts[0]),
                                    Float.parseFloat(vParts[1]),
                                    Float.parseFloat(vParts[2])
                            ));
                            break;
                        case "vn":
                            String[] vnParts = data.split("\\s+");
                            normalsList.add(new Vector3f(
                                    Float.parseFloat(vnParts[0]),
                                    Float.parseFloat(vnParts[1]),
                                    Float.parseFloat(vnParts[2])
                            ));
                            break;
                        case "vt":
                            String[] vtParts = data.split("\\s+");
                            uvsList.add(new Vector2f(
                                    Float.parseFloat(vtParts[0]),
                                    Float.parseFloat(vtParts[1])
                            ));
                            break;
                        case "f":
                            String[] faceComponents = data.split("\\s+");
                            int[][] faceIndices = new int[faceComponents.length][3];

                            for (int i = 0; i < faceComponents.length; i++) {
                                String[] indices = faceComponents[i].split("/");
                                faceIndices[i][0] = Integer.parseInt(indices[0]) - 1;
                                faceIndices[i][1] = (indices.length > 1 && !indices[1].isEmpty()) ? Integer.parseInt(indices[1]) - 1 : -1;
                                faceIndices[i][2] = (indices.length > 2 && !indices[2].isEmpty()) ? Integer.parseInt(indices[2]) - 1 : -1;
                            }

                            // If the face has 4 vertices, split it into two triangles.
                            if (faceIndices.length == 4) {
                                facesList.add(new int[][]{faceIndices[0], faceIndices[1], faceIndices[2]});
                                facesList.add(new int[][]{faceIndices[0], faceIndices[2], faceIndices[3]});
                            } else {
                                facesList.add(faceIndices);
                            }
                            break;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load mesh: " + path);
            throw new RuntimeException(e);
        }

        this.vertices = verticesList.toArray(new Vector3f[0]);
        this.normals = normalsList.toArray(new Vector3f[0]);
        this.uvs = uvsList.toArray(new Vector2f[0]);
        this.faces = facesList.toArray(new int[0][][]);
    }

    /**
     * Initializes the mesh by creating and binding OpenGL buffers.
     * <p>
     * This method interleaves vertex data and uploads it to the GPU.
     * </p>
     */
    public void initMesh() {
        if (initialized) return;

        List<Float> vertexDataList = new ArrayList<>();
        List<Integer> indicesList = new ArrayList<>();
        Map<String, Integer> uniqueVertexMap = new HashMap<>();
        int currentIndex = 0;

        for (int[][] face : faces) {
            for (int[] vertexIndices : face) {
                int vIndex = vertexIndices[0];
                int uvIndex = vertexIndices.length > 1 ? vertexIndices[1] : -1;
                int nIndex = vertexIndices.length > 2 ? vertexIndices[2] : -1;

                String key = vIndex + "/" + uvIndex + "/" + nIndex;

                if (!uniqueVertexMap.containsKey(key)) {
                    uniqueVertexMap.put(key, currentIndex);
                    Vector3f pos = vertices[vIndex];
                    vertexDataList.add(pos.x);
                    vertexDataList.add(pos.y);
                    vertexDataList.add(pos.z);

                    if (normals != null && nIndex >= 0 && nIndex < normals.length) {
                        Vector3f norm = normals[nIndex];
                        vertexDataList.add(norm.x);
                        vertexDataList.add(norm.y);
                        vertexDataList.add(norm.z);
                    } else {
                        vertexDataList.add(0.0f);
                        vertexDataList.add(0.0f);
                        vertexDataList.add(0.0f);
                    }

                    if (uvs != null && uvIndex >= 0 && uvIndex < uvs.length) {
                        Vector2f uv = uvs[uvIndex];
                        vertexDataList.add(uv.x);
                        vertexDataList.add(uv.y);
                    } else {
                        vertexDataList.add(0.0f);
                        vertexDataList.add(0.0f);
                    }

                    indicesList.add(currentIndex);
                    currentIndex++;
                } else {
                    indicesList.add(uniqueVertexMap.get(key));
                }
            }
        }

        interleavedVertexData = new float[vertexDataList.size()];
        for (int i = 0; i < vertexDataList.size(); i++) {
            interleavedVertexData[i] = vertexDataList.get(i);
        }
        indices = indicesList.stream().mapToInt(i -> i).toArray();

        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        // Use a direct FloatBuffer for vertex data.
        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(interleavedVertexData.length);
        vertexBuffer.put(interleavedVertexData).flip();
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        int stride = 8 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6 * Float.BYTES);
        glEnableVertexAttribArray(2);

        // Use a direct IntBuffer for indices.
        eboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.length);
        indexBuffer.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

        glBindVertexArray(0);
        initialized = true;
    }

    /**
     * Renders the mesh.
     * <p>
     * If the mesh is not already initialized, it will be initialized first.
     * </p>
     */
    public void render() {
        if (!initialized) initMesh();
        glBindVertexArray(vaoId);
        // Rebind both the VBO and the EBO to ensure all needed state is present on Intel GPUs.
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glDrawElements(GL_TRIANGLES, indices.length, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }



    /**
     * Releases the OpenGL buffers associated with this mesh.
     */
    public void cleanup() {
        if (initialized) {
            glDeleteBuffers(vboId);
            glDeleteBuffers(eboId);
            glDeleteVertexArrays(vaoId);
        }
    }

    /**
     * Returns a string representation of the mesh.
     *
     * @return a string describing the mesh.
     */
    @Override
    public String toString() {
        return "Mesh: " + (meshName != null ? meshName : "Unnamed") +
                "\nVertices: " + vertices.length +
                "\nTriangles: " + faces.length;
    }
}
