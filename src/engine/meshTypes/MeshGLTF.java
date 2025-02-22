package engine.meshTypes;

import engine.Material;
import engine.Mesh;
import engine.Texture;
import engine.utils.FileUtils;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4i;
import org.lwjgl.BufferUtils;
import org.lwjgl.assimp.*;
import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.PointerBuffer;

public class MeshGLTF extends Mesh {
    
    // Remove the single material field.
    // Instead, we will store a list of submeshes, each with its own material.
    public List<SubMesh> subMeshes = new ArrayList<>();
    
    // OpenGL buffer handles and interleaved data
    public int vaoId;
    private int vboId;
    private int eboId;
    private boolean initialized = false;
    private float[] interleavedVertexData;
    private int[] indices;
    
    // Helper class to store per-submesh data.
    public static class SubMesh {
        public Material material;
        public int indexCount;    // number of indices for this submesh
        public int indexOffset;   // starting index (in the merged index buffer)
        
        public SubMesh(Material material, int indexCount, int indexOffset) {
            this.material = material;
            this.indexCount = indexCount;
            this.indexOffset = indexOffset;
        }
    }
    
    /**
     * Constructs a MeshGLTF by loading the glTF file at the given path.
     * Uses Assimp to import the model and merges all meshes into one,
     * while preserving material differences in submeshes.
     *
     * @param path the path to the .gltf file.
     */
    public MeshGLTF(String path) {
        super();
        // Import the scene with triangulation and UV flipping.
        AIScene scene = aiImportFile(path, aiProcess_Triangulate | aiProcess_FlipUVs);
        if (scene == null || (scene.mFlags() & AI_SCENE_FLAGS_INCOMPLETE) != 0 || scene.mRootNode() == null) {
            throw new RuntimeException("Error loading glTF file: " + aiGetErrorString());
        }
        
        PointerBuffer meshBuffer = scene.mMeshes();
        if (meshBuffer == null || meshBuffer.capacity() == 0) {
            throw new RuntimeException("No meshes found in glTF file.");
        }
        
        // Temporary lists to merge data from all meshes.
        List<Vector3f> verticesList = new ArrayList<>();
        List<Vector3f> normalsList = new ArrayList<>();
        List<Vector2f> uvsList = new ArrayList<>();
        List<Integer> indicesList = new ArrayList<>();
        
        // Pointer to the materials in the scene.
        PointerBuffer aiMaterials = scene.mMaterials();
        
        int vertexOffset = 0;   // tracks total vertices added so far
        int indicesOffset = 0;  // tracks total indices count (for submesh grouping)
        
        // Iterate over each mesh in the file.
        for (int m = 0; m < meshBuffer.capacity(); m++) {
            AIMesh aiMesh = AIMesh.create(meshBuffer.get(m));
            int vertexCount = aiMesh.mNumVertices();
            
            // Load vertex positions.
            AIVector3D.Buffer aiVertices = aiMesh.mVertices();
            for (int i = 0; i < vertexCount; i++) {
                AIVector3D pos = aiVertices.get(i);
                verticesList.add(new Vector3f(pos.x(), pos.y(), pos.z()));
            }
            
            // Load normals.
            AIVector3D.Buffer aiNormals = aiMesh.mNormals();
            for (int i = 0; i < vertexCount; i++) {
                if (aiNormals != null) {
                    AIVector3D norm = aiNormals.get(i);
                    normalsList.add(new Vector3f(norm.x(), norm.y(), norm.z()));
                } else {
                    normalsList.add(new Vector3f(0, 0, 0));
                }
            }
            
            // Load texture coordinates (first UV channel).
            AIVector3D.Buffer aiTexCoords = aiMesh.mTextureCoords(0);
            for (int i = 0; i < vertexCount; i++) {
                if (aiTexCoords != null) {
                    AIVector3D tex = aiTexCoords.get(i);
                    uvsList.add(new Vector2f(tex.x(), tex.y()));
                } else {
                    uvsList.add(new Vector2f(0, 0));
                }
            }
            
            // Process faces (indices). Each face is assumed to be a triangle.
            int faceCount = aiMesh.mNumFaces();
            AIFace.Buffer aiFaces = aiMesh.mFaces();
            int subMeshFaceCount = 0;  // count faces for this mesh
            for (int i = 0; i < faceCount; i++) {
                AIFace face = aiFaces.get(i);
                if (face.mNumIndices() != 3) continue; // should not happen after triangulation.
                IntBuffer buffer = face.mIndices();
                int i0 = buffer.get(0) + vertexOffset;
                int i1 = buffer.get(1) + vertexOffset;
                int i2 = buffer.get(2) + vertexOffset;
                indicesList.add(i0);
                indicesList.add(i1);
                indicesList.add(i2);
                subMeshFaceCount++;
            }
            
            // Load material for this mesh.
            Material material = Material.empty; // default material
            material.name = "Material";
            
            int materialIndex = aiMesh.mMaterialIndex();
            if (aiMaterials != null && aiMaterials.capacity() > materialIndex) {
                AIMaterial aiMaterial = AIMaterial.create(aiMaterials.get(materialIndex));
                // Default textures from Material.empty.
                Texture albedoTex    = Material.empty.albedoMap;
                Texture normalTex    = Material.empty.normalMap;
                Texture metallicTex  = Material.empty.metallicMap;
                Texture roughnessTex = Material.empty.roughnessMap;
                Texture aoTex        = Material.empty.aoMap;
                Texture heightTex    = Material.empty.heightMap;
                
                AIString pathStr = AIString.calloc();
                // Query diffuse texture.
                if (aiGetMaterialTexture(aiMaterial, aiTextureType_DIFFUSE, 0, pathStr,
                                         (int[]) null, (int[]) null, (float[]) null, (int[]) null, (int[]) null, (int[]) null) == 0) {
                    String texPath = pathStr.dataString();
                    albedoTex = new Texture(FileUtils.load(texPath));
                }
                // Query normal map.
                if (aiGetMaterialTexture(aiMaterial, aiTextureType_NORMALS, 0, pathStr,
                                         (int[]) null, (int[]) null, (float[]) null, (int[]) null, (int[]) null, (int[]) null) == 0) {
                    String texPath = pathStr.dataString();
                    normalTex = new Texture(FileUtils.load(texPath));
                }
                // Query metallic map.
                if (aiGetMaterialTexture(aiMaterial, aiTextureType_METALNESS, 0, pathStr,
                                         (int[]) null, (int[]) null, (float[]) null, (int[]) null, (int[]) null, (int[]) null) == 0) {
                    String texPath = pathStr.dataString();
                    metallicTex = new Texture(FileUtils.load(texPath));
                }
                // Query roughness map.
                if (aiGetMaterialTexture(aiMaterial, aiTextureType_DIFFUSE_ROUGHNESS, 0, pathStr,
                                         (int[]) null, (int[]) null, (float[]) null, (int[]) null, (int[]) null, (int[]) null) == 0) {
                    String texPath = pathStr.dataString();
                    roughnessTex = new Texture(FileUtils.load(texPath));
                }
                // Query ambient (occlusion) texture.
                if (aiGetMaterialTexture(aiMaterial, aiTextureType_AMBIENT, 0, pathStr,
                                         (int[]) null, (int[]) null, (float[]) null, (int[]) null, (int[]) null, (int[]) null) == 0) {
                    String texPath = pathStr.dataString();
                    aoTex = new Texture(FileUtils.load(texPath));
                }
                // Query height map texture.
                if (aiGetMaterialTexture(aiMaterial, aiTextureType_HEIGHT, 0, pathStr,
                                         (int[]) null, (int[]) null, (float[]) null, (int[]) null, (int[]) null, (int[]) null) == 0) {
                    String texPath = pathStr.dataString();
                    heightTex = new Texture(FileUtils.load(texPath));
                }
                pathStr.free();
                
                material = new Material(albedoTex, normalTex, metallicTex, roughnessTex, aoTex, heightTex, 0.0f, 0.2f);
                material.normalMapStrength = 2;
            }
            
            // Record this mesh as a submesh.
            int subMeshIndexCount = subMeshFaceCount * 3;
            subMeshes.add(new SubMesh(material, subMeshIndexCount, indicesOffset));
            indicesOffset += subMeshIndexCount;
            vertexOffset += vertexCount;
        }
        
        // Convert lists to arrays.
        this.vertices = verticesList.toArray(new Vector3f[0]);
        this.normals = normalsList.toArray(new Vector3f[0]);
        this.uvs = uvsList.toArray(new Vector2f[0]);
        // Convert List<Integer> to int[].
        this.indices = indicesList.stream().mapToInt(i -> i).toArray();
        
        // Release the imported scene.
        aiReleaseImport(scene);
    }
    
    @Override
    public void initMesh() {
        if (initialized) return;
        
        // Build interleaved vertex data: position (3) + normal (3) + uv (2) = 8 floats per vertex.
        int vertexCount = vertices.length;
        interleavedVertexData = new float[vertexCount * 8];
        for (int i = 0; i < vertexCount; i++) {
            Vector3f pos = vertices[i];
            Vector3f norm = normals[i];
            Vector2f uv = uvs[i];
            interleavedVertexData[i * 8]     = pos.x;
            interleavedVertexData[i * 8 + 1] = pos.y;
            interleavedVertexData[i * 8 + 2] = pos.z;
            interleavedVertexData[i * 8 + 3] = norm.x;
            interleavedVertexData[i * 8 + 4] = norm.y;
            interleavedVertexData[i * 8 + 5] = norm.z;
            interleavedVertexData[i * 8 + 6] = uv.x;
            interleavedVertexData[i * 8 + 7] = uv.y;
        }
        
        // Create and bind OpenGL buffers.
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);
        
        // VBO for vertex data.
        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(interleavedVertexData.length);
        vertexBuffer.put(interleavedVertexData).flip();
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        
        // EBO for indices.
        eboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.length);
        indexBuffer.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
        
        // Set up vertex attribute pointers.
        int stride = 8 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6 * Float.BYTES);
        glEnableVertexAttribArray(2);
        
        glBindVertexArray(0);
        initialized = true;
    }
    
    @Override
    public void render() {
        if (!initialized) initMesh();
        glBindVertexArray(vaoId);
        // Instead of binding material(s) here, simply issue the draw calls.
        // This assumes that the active material has already been bound
        // by the renderRecursive method in your scene.
        if(subMeshes != null && !subMeshes.isEmpty()){
            for (SubMesh subMesh : subMeshes) {
                // Draw the current submesh using its index offset and count.
                glDrawElements(GL_TRIANGLES, subMesh.indexCount, GL_UNSIGNED_INT, subMesh.indexOffset * Integer.BYTES);
            }
        } else {
            // Fallback: if no submeshes exist, draw the entire mesh.
            glDrawElements(GL_TRIANGLES, indices.length, GL_UNSIGNED_INT, 0);
        }
        glBindVertexArray(0);
    }
    
    
    @Override
    public void cleanup() {
        if (initialized) {
            glDeleteBuffers(vboId);
            glDeleteBuffers(eboId);
            glDeleteVertexArrays(vaoId);
        }
    }
}
