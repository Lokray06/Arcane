package engine.utils;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram {
    private final int programId;
    
    public ShaderProgram(String vertexSource, String fragmentSource) {
        int vertexShaderId = compileShader(vertexSource, GL_VERTEX_SHADER);
        int fragmentShaderId = compileShader(fragmentSource, GL_FRAGMENT_SHADER);
        programId = glCreateProgram();
        glAttachShader(programId, vertexShaderId);
        glAttachShader(programId, fragmentShaderId);
        glLinkProgram(programId);
        
        // Check linking status
        int linked = glGetProgrami(programId, GL_LINK_STATUS);
        if (linked == 0) {
            String log = glGetProgramInfoLog(programId);
            throw new RuntimeException("Error linking shader program: " + log);
        }
        
        // Shaders can be detached and deleted after linking.
        glDetachShader(programId, vertexShaderId);
        glDetachShader(programId, fragmentShaderId);
        glDeleteShader(vertexShaderId);
        glDeleteShader(fragmentShaderId);
    }
    
    private int compileShader(String source, int type) {
        int shaderId = glCreateShader(type);
        glShaderSource(shaderId, source);
        glCompileShader(shaderId);
        
        // Check compile status
        int compiled = glGetShaderi(shaderId, GL_COMPILE_STATUS);
        if (compiled == 0) {
            String log = glGetShaderInfoLog(shaderId);
            throw new RuntimeException("Error compiling shader: " + log);
        }
        return shaderId;
    }
    
    public void use() {
        glUseProgram(programId);
    }
    
    public int getUniformLocation(String name) {
        return glGetUniformLocation(programId, name);
    }
    
    public void setUniformMat4(String name, Matrix4f matrix) {
        int location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            glUniformMatrix4fv(location, false, matrix.get(fb));
        }
    }
    
    public void cleanup() {
        glDeleteProgram(programId);
    }
}
