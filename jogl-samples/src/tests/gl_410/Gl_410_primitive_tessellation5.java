/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests.gl_410;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL2GL3.GL_LINE;
import static com.jogamp.opengl.GL3ES3.*;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import framework.BufferUtils;
import glm.glm;
import glm.mat._4.Mat4;
import framework.Profile;
import framework.Semantic;
import framework.Test;
import glf.Vertex_v2fc4f;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author GBarbieri
 */
public class Gl_410_primitive_tessellation5 extends Test {

    public static void main(String[] args) {
        Gl_410_primitive_tessellation5 gl_410_primitive_tessellation5 = new Gl_410_primitive_tessellation5();
    }

    public Gl_410_primitive_tessellation5() {
        super("gl-410-primitive-tessellation5", Profile.CORE, 4, 1);
    }

    private final String SHADERS_SOURCE = "tess";
    private final String SHADERS_ROOT = "src/data/gl_410";

    private int vertexCount = 4;
    private int vertexSize = vertexCount * Vertex_v2fc4f.SIZE;
    private float[] vertexData = {
        -1.0f, -1.0f,/**/ 1.0f, 0.0f, 0.0f, 1.0f,
        +1.0f, -1.0f,/**/ 1.0f, 1.0f, 0.0f, 1.0f,
        +1.0f, +1.0f,/**/ 0.0f, 1.0f, 0.0f, 1.0f,
        -1.0f, +1.0f,/**/ 0.0f, 0.0f, 1.0f, 1.0f};

    private class Program {

        public static final int VERT = 0;
        public static final int CONT = 1;
        public static final int EVAL = 2;
        public static final int GEOM = 3;
        public static final int FRAG = 4;
        public static final int MAX = 5;
    }

    private IntBuffer pipelineName = GLBuffers.newDirectIntBuffer(1), arrayBufferName = GLBuffers.newDirectIntBuffer(1),
            vertexArrayName = GLBuffers.newDirectIntBuffer(1);
    private int[] programName = new int[Program.MAX];
    private int uniformMvp;

    @Override
    protected boolean begin(GL gl) {

        GL4 gl4 = (GL4) gl;

        boolean validated = true;

        if (validated) {
            validated = initProgram(gl4);
        }
        if (validated) {
            validated = initArrayBuffer(gl4);
        }
        if (validated) {
            validated = initVertexArray(gl4);
        }

        return validated && checkError(gl4, "begin");
    }

    private boolean initProgram(GL4 gl4) {

        boolean validated = true;

        gl4.glGenProgramPipelines(1, pipelineName);
        gl4.glBindProgramPipeline(pipelineName.get(0));
        gl4.glBindProgramPipeline(0);

        // Create program
        if (validated) {

            ShaderProgram[] shaderPrograms = new ShaderProgram[Program.MAX];

            ShaderCode[] shaderCodes = new ShaderCode[]{
                ShaderCode.create(gl4, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null, SHADERS_SOURCE,
                "vert", null, true),
                ShaderCode.create(gl4, GL_TESS_CONTROL_SHADER, this.getClass(), SHADERS_ROOT, null, SHADERS_SOURCE,
                "cont", null, true),
                ShaderCode.create(gl4, GL_TESS_EVALUATION_SHADER, this.getClass(), SHADERS_ROOT, null, SHADERS_SOURCE,
                "eval", null, true),
                ShaderCode.create(gl4, GL_GEOMETRY_SHADER, this.getClass(), SHADERS_ROOT, null, SHADERS_SOURCE,
                "geom", null, true),
                ShaderCode.create(gl4, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null, SHADERS_SOURCE,
                "frag", null, true)};

            for (int i = 0; i < Program.MAX; i++) {

                shaderPrograms[i] = new ShaderProgram();
                shaderPrograms[i].init(gl4);
                shaderPrograms[i].add(shaderCodes[i]);
                programName[i] = shaderPrograms[i].program();
                gl4.glProgramParameteri(programName[i], GL_PROGRAM_SEPARABLE, GL_TRUE);
                shaderPrograms[i].link(gl4, System.out);
            }
        }

        if (validated) {

            gl4.glUseProgramStages(pipelineName.get(0), GL_VERTEX_SHADER_BIT, programName[Program.VERT]);
            gl4.glUseProgramStages(pipelineName.get(0), GL_TESS_CONTROL_SHADER_BIT, programName[Program.CONT]);
            gl4.glUseProgramStages(pipelineName.get(0), GL_TESS_EVALUATION_SHADER_BIT, programName[Program.EVAL]);
            gl4.glUseProgramStages(pipelineName.get(0), GL_GEOMETRY_SHADER_BIT, programName[Program.GEOM]);
            gl4.glUseProgramStages(pipelineName.get(0), GL_FRAGMENT_SHADER_BIT, programName[Program.FRAG]);

        }

        // Get variables locations
        if (validated) {

            uniformMvp = gl4.glGetUniformLocation(programName[Program.VERT], "mvp");
        }

        return validated & checkError(gl4, "initProgram");
    }

    private boolean initVertexArray(GL4 gl4) {

        gl4.glGenVertexArrays(1, vertexArrayName);
        gl4.glBindVertexArray(vertexArrayName.get(0));
        {
            gl4.glBindBuffer(GL_ARRAY_BUFFER, arrayBufferName.get(0));
            gl4.glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL_FLOAT, false, (2 + 4) * Float.BYTES, 0);
            gl4.glVertexAttribPointer(Semantic.Attr.COLOR, 4, GL_FLOAT, false, (2 + 4) * Float.BYTES, 2 * Float.BYTES);
            gl4.glBindBuffer(GL_ARRAY_BUFFER, 0);

            gl4.glEnableVertexAttribArray(Semantic.Attr.POSITION);
            gl4.glEnableVertexAttribArray(Semantic.Attr.COLOR);
        }
        gl4.glBindVertexArray(0);

        return checkError(gl4, "initVertexArray");
    }

    private boolean initArrayBuffer(GL4 gl4) {

        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);

        // Generate a buffer object
        gl4.glGenBuffers(1, arrayBufferName);
        gl4.glBindBuffer(GL_ARRAY_BUFFER, arrayBufferName.get(0));
        gl4.glBufferData(GL_ARRAY_BUFFER, vertexSize, vertexBuffer, GL_STATIC_DRAW);
        gl4.glBindBuffer(GL_ARRAY_BUFFER, 0);

        BufferUtils.destroyDirectBuffer(vertexBuffer);

        return checkError(gl4, "initArrayBuffer");
    }

    @Override
    protected boolean render(GL gl) {

        GL4 gl4 = (GL4) gl;

        gl4.glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

        Mat4 projection = glm.perspective_((float) Math.PI * 0.25f, (float) windowSize.x / windowSize.y, 0.1f, 100.0f);
        Mat4 model = new Mat4(1.0f);
        Mat4 mvp = projection.mul(viewMat4()).mul(model);

        gl4.glProgramUniformMatrix4fv(programName[Program.VERT], uniformMvp, 1, false, mvp.toFa_(), 0);

        gl4.glViewportIndexedfv(0, viewportBuffer.put(0, 0).put(1, 0).put(2, windowSize.x).put(3, windowSize.y));
        gl4.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0).put(1, 0).put(2, 0).put(3, 0));

        gl4.glBindProgramPipeline(pipelineName.get(0));

        gl4.glBindVertexArray(vertexArrayName.get(0));
        gl4.glPatchParameteri(GL_PATCH_VERTICES, vertexCount);
        gl4.glDrawArraysInstancedBaseInstance(GL_PATCHES, 0, vertexCount, 1, 0);

        return true;
    }

    @Override
    protected boolean end(GL gl) {

        GL4 gl4 = (GL4) gl;

        gl4.glDeleteVertexArrays(1, vertexArrayName);
        gl4.glDeleteBuffers(1, arrayBufferName);
        for (int i = 0; i < Program.MAX; ++i) {
            gl4.glDeleteProgram(programName[i]);
        }
        gl4.glDeleteProgramPipelines(1, pipelineName);

        BufferUtils.destroyDirectBuffer(vertexArrayName);
        BufferUtils.destroyDirectBuffer(arrayBufferName);
        BufferUtils.destroyDirectBuffer(pipelineName);

        return checkError(gl4, "end");
    }
}
