package engine;

import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Mesh {
    public Vector3f[] vertices;
    public Vector3f[] normals;
    public Vector2f[] uvs;
    public int[][][] faces;
    public String meshName;
    
    // OpenGL handles
    private int vaoId;
    private int vboId;
    private int eboId;
    private boolean initialized = false;
    
    private float[] interleavedVertexData;
    private int[] indices;
    
    /**
     * Constructor that loads a mesh from an OBJ file.
     *
     * @param path Path to the OBJ file.
     */
    public Mesh(String path) {
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
     * Constructor that initializes the mesh directly from vertex and face data.
     *
     * @param vertices Array of vertex positions.
     * @param faces    Array of face indices.
     */
    public Mesh(Vector3f[] vertices, int[][][] faces) {
        this.vertices = vertices;
        this.faces = faces;
        this.meshName = "GeneratedMesh";
    }
    
    /**
     * Initializes OpenGL buffers.
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
        
        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, interleavedVertexData, GL_STATIC_DRAW);
        
        int stride = 8 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6 * Float.BYTES);
        glEnableVertexAttribArray(2);
        
        eboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
        
        glBindVertexArray(0);
        initialized = true;
    }
    
    public void render() {
        if (!initialized) initMesh();
        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES, indices.length, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }
    
    public void cleanup() {
        if (initialized) {
            glDeleteBuffers(vboId);
            glDeleteBuffers(eboId);
            glDeleteVertexArrays(vaoId);
        }
    }
    
    @Override
    public String toString() {
        return "Mesh: " + (meshName != null ? meshName : "Unnamed") +
               "\nVertices: " + vertices.length +
               "\nTriangles: " + faces.length;
    }
}
