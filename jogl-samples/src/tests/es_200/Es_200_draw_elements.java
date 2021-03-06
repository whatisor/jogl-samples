/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests.es_200;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import static com.jogamp.opengl.GL2ES2.*;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import glm.glm;
import glm.mat._4.Mat4;
import glm.vec._2.Vec2;
import framework.BufferUtils;
import framework.Profile;
import framework.Semantic;
import framework.Test;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 *
 * @author elect
 */
public class Es_200_draw_elements extends Test {

    public static void main(String[] args) {
        Es_200_draw_elements es_200_draw_elements = new Es_200_draw_elements();
    }

    private final String SHADERS_SOURCE = "flat-color";
    private final String SHADERS_ROOT = "src/data/es_200";

    private final int elementCount = 6;
    private final int elementSize = elementCount * Short.BYTES;
    private final short[] elementData = new short[]{
        0, 1, 2,
        0, 2, 3
    };
    private final int vertexCount = 4;
    private final int positionSize = vertexCount * Vec2.SIZE;
    private final float[] positionData = new float[]{
        -1f, -1f,
        +1f, -1f,
        +1f, +1f,
        -1f, +1f
    };

    private class Buffer {

        private static final int VERTEX = 0;
        private static final int ELEMENT = 1;
        private static final int MAX = 2;
    }

    private IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX);
    private int programName, uniformMvp, uniformDiffuse;

    public Es_200_draw_elements() {
        super("es_200_draw_elements", Profile.ES, 2, 0);
    }

    @Override
    protected boolean begin(GL gl) {

        GL2ES2 gl2es2 = (GL2ES2) gl;

        boolean validated = true;

        System.out.println("Vendor " + gl2es2.glGetString(GL_VENDOR));
        System.out.println("Renderer " + gl2es2.glGetString(GL_RENDERER));
        System.out.println("Version " + gl2es2.glGetString(GL_VERSION));
        System.out.println("Extensions " + gl2es2.glGetString(GL_EXTENSIONS));

        if (validated) {
            validated = initProgram(gl2es2);
        }
        if (validated) {
            validated = initBuffer(gl2es2);
        }

        return validated;
    }

    private boolean initProgram(GL2ES2 gl2es2) {

        boolean validated = true;
        // Create program
        if (validated) {
            ShaderCode vertShader = ShaderCode.create(gl2es2, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null, 
                    SHADERS_SOURCE, "vert", null, true);
            ShaderCode fragShader = ShaderCode.create(gl2es2, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null, 
                    SHADERS_SOURCE, "frag", null, true);

            vertShader.defaultShaderCustomization(gl2es2, true, false);
            fragShader.defaultShaderCustomization(gl2es2, true, false);

            ShaderProgram program = new ShaderProgram();
            program.add(vertShader);
            program.add(fragShader);

            program.init(gl2es2);
            programName = program.program();
            gl2es2.glBindAttribLocation(programName, Semantic.Attr.POSITION, "position");

            program.link(gl2es2, System.out);
        }
        // Get variables locations
        if (validated) {
            uniformMvp = gl2es2.glGetUniformLocation(programName, "mvp");
            uniformDiffuse = gl2es2.glGetUniformLocation(programName, "diffuse");
        }
        // Set some variables 
        if (validated) {
            // Bind the program for use
            gl2es2.glUseProgram(programName);
            // Set uniform value
            gl2es2.glUniform4fv(uniformDiffuse, 1, new float[]{1f, .5f, 0f, 1f}, 0);
            // Unbind the program
            gl2es2.glUseProgram(0);
        }
        return validated & checkError(gl2es2, "initProgram");
    }

    private boolean initBuffer(GL2ES2 gl2es2) {

        gl2es2.glGenBuffers(Buffer.MAX, bufferName);

        FloatBuffer positionBuffer = GLBuffers.newDirectFloatBuffer(positionData);
        ShortBuffer shortBuffer = GLBuffers.newDirectShortBuffer(elementData);

        gl2es2.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        gl2es2.glBufferData(GL_ARRAY_BUFFER, positionSize, positionBuffer, GL_STATIC_DRAW);
        gl2es2.glBindBuffer(GL_ARRAY_BUFFER, 0);

        gl2es2.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        gl2es2.glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementSize, shortBuffer, GL_STATIC_DRAW);
        gl2es2.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        BufferUtils.destroyDirectBuffer(positionBuffer);
        BufferUtils.destroyDirectBuffer(shortBuffer);

        return checkError(gl2es2, "initBuffer");
    }

    @Override
    protected boolean render(GL gl) {

        GL2ES2 gl2es2 = (GL2ES2) gl;

        // Compute the MVP (Model View Projection matrix)
        Mat4 projection = glm.perspective_((float) Math.PI * 0.25f, 4.0f / 3.0f, 0.1f, 100.0f);
        Mat4 model = new Mat4(1.0f);
        Mat4 mvp = projection.mul(viewMat4()).mul(model);

        // Set the display viewport
        gl2es2.glViewport(0, 0, windowSize.x, windowSize.y);

        // Clear color buffer with black
        gl2es2.glClearColor(0f, 0f, 0f, 1f);
        gl2es2.glClearDepthf(1f);
        gl2es2.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Bind program
        gl2es2.glUseProgram(programName);

        // Set the value of MVP uniform.
        gl2es2.glUniformMatrix4fv(uniformMvp, 1, false, mvp.toFa_(), 0);

        gl2es2.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        {
            gl2es2.glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL_FLOAT, false, 0, 0);
        }
        gl2es2.glBindBuffer(GL_ARRAY_BUFFER, 0);
        gl2es2.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));

        gl2es2.glEnableVertexAttribArray(Semantic.Attr.POSITION);
        {
            gl2es2.glDrawElements(GL_TRIANGLES, elementCount, GL_UNSIGNED_SHORT, 0);
        }
        gl2es2.glDisableVertexAttribArray(Semantic.Attr.POSITION);

        // Unbind program
        gl2es2.glUseProgram(0);

        return true;
    }

    @Override
    protected boolean end(GL gl) {
        GL2ES2 gl2es2 = (GL2ES2) gl;

        gl2es2.glDeleteBuffers(Buffer.MAX, bufferName);
        gl2es2.glDeleteProgram(programName);

        BufferUtils.destroyDirectBuffer(bufferName);

        return true;
    }
}
