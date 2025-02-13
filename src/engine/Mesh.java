package engine;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

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
    
    // OpenGL handles for modern rendering
    private int vaoId;
    private int vboId;
    private int eboId;
    private boolean initialized = false;
    
    // These will be built from your OBJ data
    private float[] interleavedVertexData;
    private int[] indices;
    
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
                            this.meshName = data; // Store object name
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
                            
                            // Triangulate if it's a quad
                            if (faceIndices.length == 4) {
                                facesList.add(new int[][] { faceIndices[0], faceIndices[1], faceIndices[2] });
                                facesList.add(new int[][] { faceIndices[0], faceIndices[2], faceIndices[3] });
                            } else {
                                facesList.add(faceIndices);
                            }
                            break;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Could not load file from '" + path + "'.");
        } catch (IOException e) {
            System.err.println("Something went wrong.");
            throw new RuntimeException(e);
        }
        
        this.vertices = verticesList.toArray(new Vector3f[0]);
        this.normals = normalsList.toArray(new Vector3f[0]);
        this.uvs = uvsList.toArray(new Vector2f[0]);
        this.faces = facesList.toArray(new int[0][][]);
        
        // You may choose to initialize the OpenGL buffers here or lazily during render().
        // initMesh();
    }
    
    /**
     * Processes the OBJ face data to build an interleaved vertex array and an index array,
     * then uploads the data to GPU buffers (VAO, VBO, and EBO).
     */
    public void initMesh() {
        if (initialized) return;
        
        // Map to track unique vertex combinations.
        // The key is "vIndex/uvIndex/normalIndex".
        Map<String, Integer> uniqueVertexMap = new HashMap<>();
        List<Float> vertexDataList = new ArrayList<>();
        List<Integer> indicesList = new ArrayList<>();
        int currentIndex = 0;
        
        // For each face (each face is already triangulated)
        for (int[][] face : faces) {
            for (int[] vertexIndices : face) {
                int vIndex = vertexIndices[0];
                int uvIndex = vertexIndices[1];
                int nIndex = vertexIndices[2];
                
                // Create a unique key for this vertex combination
                String key = vIndex + "/" + uvIndex + "/" + nIndex;
                if (!uniqueVertexMap.containsKey(key)) {
                    uniqueVertexMap.put(key, currentIndex);
                    
                    // Position
                    Vector3f pos = vertices[vIndex];
                    vertexDataList.add(pos.x);
                    vertexDataList.add(pos.y);
                    vertexDataList.add(pos.z);
                    
                    // Normal (if available, else default to 0,0,0)
                    if (nIndex >= 0 && nIndex < normals.length) {
                        Vector3f norm = normals[nIndex];
                        vertexDataList.add(norm.x);
                        vertexDataList.add(norm.y);
                        vertexDataList.add(norm.z);
                    } else {
                        vertexDataList.add(0.0f);
                        vertexDataList.add(0.0f);
                        vertexDataList.add(0.0f);
                    }
                    
                    // UV (if available, else default to 0,0)
                    if (uvIndex >= 0 && uvIndex < uvs.length) {
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
                    // Already exists, so just add the existing index.
                    indicesList.add(uniqueVertexMap.get(key));
                }
            }
        }
        
        // Convert lists to arrays.
        interleavedVertexData = new float[vertexDataList.size()];
        for (int i = 0; i < vertexDataList.size(); i++) {
            interleavedVertexData[i] = vertexDataList.get(i);
        }
        
        indices = new int[indicesList.size()];
        for (int i = 0; i < indicesList.size(); i++) {
            indices[i] = indicesList.get(i);
        }
        
        // Create and bind VAO
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);
        
        // Create VBO and upload interleaved vertex data
        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, interleavedVertexData, GL_STATIC_DRAW);
        
        // Set up vertex attribute pointers:
        // Interleaved layout: position (3 floats), normal (3 floats), uv (2 floats) = 8 floats total.
        int stride = 8 * Float.BYTES;
        
        // Position attribute (location 0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        
        // Normal attribute (location 1)
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        
        // UV attribute (location 2)
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6 * Float.BYTES);
        glEnableVertexAttribArray(2);
        
        // Create EBO and upload index data
        eboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
        
        // Unbind VAO (the EBO remains associated with the VAO)
        glBindVertexArray(0);
        
        initialized = true;
    }
    
    /**
     * Renders the mesh using the VAO. Assumes a shader program is active and the proper
     * uniforms (model, view, projection) have been set.
     */
    public void render() {
        if (!initialized) {
            initMesh();
        }
        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES, indices.length, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }
    
    /**
     * Releases the GPU resources.
     */
    public void cleanup() {
        if (initialized) {
            glDeleteBuffers(vboId);
            glDeleteBuffers(eboId);
            glDeleteVertexArrays(vaoId);
        }
    }
    
    @Override
    public String toString() {
        return "Mesh: " + (meshName != null ? meshName : "Unnamed") + "\n" +
               "Vertices: " + vertices.length + "\n" +
               "Triangles: " + faces.length + "\n";
    }
}
