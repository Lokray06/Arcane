package engine.utils;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32.*;

public class ShaderProgram {
    /** The OpenGL program ID. */
    public final int programId;

    /**
     * Creates a new shader program from the provided vertex and fragment shader source code.
     *
     * @param vertexSource   the source code for the vertex shader.
     * @param fragmentSource the source code for the fragment shader.
     */
    public ShaderProgram(String vertexSource, String fragmentSource) {
        int vertexShaderId = compileShader(vertexSource, GL_VERTEX_SHADER);
        int fragmentShaderId = compileShader(fragmentSource, GL_FRAGMENT_SHADER);
        programId = glCreateProgram();
        if (programId == 0) {
            throw new RuntimeException("Could not create Shader Program");
        }
        glAttachShader(programId, vertexShaderId);
        glAttachShader(programId, fragmentShaderId);
        glLinkProgram(programId);

        // Check linking status.
        int linked = glGetProgrami(programId, GL_LINK_STATUS);
        String programLog = glGetProgramInfoLog(programId);
        if (!programLog.isEmpty()) {
            System.out.println("[ShaderProgram] Program link log:\n" + programLog);
        }
        if (linked == 0) {
            throw new RuntimeException("Error linking shader program: " + programLog);
        }

        // Shaders can be detached and deleted after linking.
        glDetachShader(programId, vertexShaderId);
        glDetachShader(programId, fragmentShaderId);
        glDeleteShader(vertexShaderId);
        glDeleteShader(fragmentShaderId);
    }

    /**
     * Creates a new shader program from the provided vertex, geometry, and fragment shader source code.
     *
     * @param vertexSource   the source code for the vertex shader.
     * @param geometrySource the source code for the geometry shader.
     * @param fragmentSource the source code for the fragment shader.
     */
    public ShaderProgram(String vertexSource, String geometrySource, String fragmentSource) {
        int vertexShaderId = compileShader(vertexSource, GL_VERTEX_SHADER);
        int geometryShaderId = compileShader(geometrySource, GL_GEOMETRY_SHADER);
        int fragmentShaderId = compileShader(fragmentSource, GL_FRAGMENT_SHADER);
        programId = glCreateProgram();
        if (programId == 0) {
            throw new RuntimeException("Could not create Shader Program");
        }
        glAttachShader(programId, vertexShaderId);
        glAttachShader(programId, geometryShaderId);
        glAttachShader(programId, fragmentShaderId);
        glLinkProgram(programId);

        // Check linking status.
        int linked = glGetProgrami(programId, GL_LINK_STATUS);
        String programLog = glGetProgramInfoLog(programId);
        if (!programLog.isEmpty()) {
            System.out.println("[ShaderProgram] Program link log:\n" + programLog);
        }
        if (linked == 0) {
            throw new RuntimeException("Error linking shader program: " + programLog);
        }

        // Shaders can be detached and deleted after linking.
        glDetachShader(programId, vertexShaderId);
        glDetachShader(programId, geometryShaderId);
        glDetachShader(programId, fragmentShaderId);
        glDeleteShader(vertexShaderId);
        glDeleteShader(geometryShaderId);
        glDeleteShader(fragmentShaderId);
    }

    /**
     * Compiles a shader of the specified type from source code.
     *
     * @param source the shader source code.
     * @param type   the type of shader (e.g. {@code GL_VERTEX_SHADER}, {@code GL_GEOMETRY_SHADER} or {@code GL_FRAGMENT_SHADER}).
     * @return the shader ID.
     */
    private int compileShader(String source, int type) {
        int shaderId = glCreateShader(type);
        if (shaderId == 0) {
            throw new RuntimeException("Error creating shader. Type: " + type);
        }
        glShaderSource(shaderId, source);
        glCompileShader(shaderId);

        // Check compile status.
        int compiled = glGetShaderi(shaderId, GL_COMPILE_STATUS);
        String shaderLog = glGetShaderInfoLog(shaderId);
        String shaderType = (type == GL_VERTEX_SHADER) ? "VERTEX" :
                (type == GL_GEOMETRY_SHADER) ? "GEOMETRY" : "FRAGMENT";
        if (!shaderLog.isEmpty()) {
            System.out.println("[ShaderProgram] " + shaderType + " shader compile log:\n" + shaderLog);
        }
        if (compiled == 0) {
            throw new RuntimeException("Error compiling shader (" + shaderType + "): " + shaderLog);
        }
        return shaderId;
    }

    /**
     * Sets this shader program as the active program.
     */
    public void use() {
        glUseProgram(programId);
    }

    /**
     * Detaches the current shader program.
     */
    public void detach() {
        glUseProgram(0);
    }

    /**
     * Retrieves the location of a uniform variable in the shader program.
     *
     * @param name the name of the uniform variable.
     * @return the uniform location.
     */
    public int getUniformLocation(String name) {
        int location = glGetUniformLocation(programId, name);
        if (location == -1) {
            //System.out.println("[ShaderProgram] Warning: Uniform '" + name + "' not found.");
        }
        return location;
    }

    /**
     * Sets an integer uniform value.
     *
     * @param name  the name of the uniform variable.
     * @param value the integer value.
     */
    public void setUniform(String name, int value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            glUniform1i(location, value);
        }
    }

    /**
     * Sets a float uniform value.
     *
     * @param name  the name of the uniform variable.
     * @param value the float value.
     */
    public void setUniform(String name, float value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            glUniform1f(location, value);
        }
    }

    /**
     * Sets a Vector3f uniform value.
     *
     * @param name  the name of the uniform variable.
     * @param value the {@link Vector3f} value.
     */
    public void setUniform(String name, Vector3f value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            glUniform3f(location, value.x, value.y, value.z);
        }
    }

    /**
     * Sets a Matrix4f uniform value.
     *
     * @param name   the name of the uniform variable.
     * @param matrix the {@link Matrix4f} value.
     */
    public void setUniformMat4(String name, Matrix4f matrix) {
        int location = getUniformLocation(name);
        if (location != -1) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer fb = stack.mallocFloat(16);
                glUniformMatrix4fv(location, false, matrix.get(fb));
            }
        }
    }

    /**
     * Sets a Vector2f uniform value.
     *
     * @param name  the name of the uniform variable.
     * @param value the {@link Vector2f} value.
     */
    public void setUniform(String name, Vector2f value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            glUniform2f(location, value.x, value.y);
        }
    }

    /**
     * Deletes the shader program and releases its OpenGL resources.
     */
    public void cleanup() {
        glDeleteProgram(programId);
    }
}
