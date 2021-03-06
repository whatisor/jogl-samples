/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests.gl_320.fbo;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL2ES3.*;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import glm.glm;
import glm.mat._4.Mat4;
import glm.vec._2.Vec2;
import glm.vec._2.i.Vec2i;
import glm.vec._3.Vec3;
import framework.BufferUtils;
import framework.Profile;
import framework.Semantic;
import framework.Test;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jgli.Texture2d;

/**
 *
 * @author GBarbieri
 */
public class Gl_320_fbo_multisample_explicit extends Test {

    public static void main(String[] args) {
        Gl_320_fbo_multisample_explicit gl_320_fbo_multisample_explicit = new Gl_320_fbo_multisample_explicit();
    }

    public Gl_320_fbo_multisample_explicit() {
        super("Gl-320-fbo-multisample-explicit", Profile.CORE, 3, 2, new Vec2(Math.PI * 0.2f));
    }

    private final String TEXTURE_DIFFUSE = "kueken7_rgba8_srgb.dds";
    private final String SHADERS_ROOT = "src/data/gl_320/fbo";
    private final String VERT_SHADER_SOURCE = "fbo-multisample-explicit";
    private final String[] FRAG_SHADER_SOURCE = new String[]{
        "fbo-multisample-explicit-texture",
        "fbo-multisample-explicit-box",
        "fbo-multisample-explicit-near"};

    private Vec2i FRAMEBUFFER_SIZE = new Vec2i(160, 120);
    private int vertexCount = 6;
    private int vertexSize = vertexCount * glf.Vertex_v2fv2f.SIZE;
    private float[] vertexData = new float[]{
        -1.0f, -1.0f,/**/ 0.0f, 1.0f,
        +1.0f, -1.0f,/**/ 1.0f, 1.0f,
        +1.0f, +1.0f,/**/ 1.0f, 0.0f,
        +1.0f, +1.0f,/**/ 1.0f, 0.0f,
        -1.0f, +1.0f,/**/ 0.0f, 0.0f,
        -1.0f, -1.0f,/**/ 0.0f, 1.0f
    };

    private class Program {

        public static final int THROUGH = 0;
        public static final int RESOLVE_BOX = 1;
        public static final int RESOLVE_NEAR = 2;
        public static final int MAX = 3;
    }

    private class Texture {

        public static final int COLORBUFFER = 0;
        public static final int MULTISAMPLE_DEPTHBUFFER = 1;
        public static final int MULTISAMPLE_COLORBUFFER = 2;
        public static final int DIFFUSE = 3;
        public static final int MAX = 4;
    }

    private class Shader {

        public static final int VERT = 0;
        public static final int FRAG_TEXTURE = 1;
        public static final int FRAG_BOX = 2;
        public static final int FRAG_NEAR = 3;
        public static final int MAX = 4;
    }

    private IntBuffer vertexArrayName = GLBuffers.newDirectIntBuffer(1), bufferName = GLBuffers.newDirectIntBuffer(1),
            framebufferRenderName = GLBuffers.newDirectIntBuffer(1),
            textureName = GLBuffers.newDirectIntBuffer(Texture.MAX);
    private int[] programName = new int[Program.MAX], uniformMvp = new int[Program.MAX],
            uniformDiffuse = new int[Program.MAX];

    @Override
    protected boolean begin(GL gl) {

        GL3 gl3 = (GL3) gl;
        boolean validated = true;

        if (validated) {
            validated = initProgram(gl3);
        }
        if (validated) {
            validated = initBuffer(gl3);
        }
        if (validated) {
            validated = initVertexArray(gl3);
        }
        if (validated) {
            validated = initTexture(gl3);
        }
        if (validated) {
            validated = initFramebuffer(gl3);
        }

        return validated;
    }

    private boolean initProgram(GL3 gl3) {

        boolean validated = true;

        ShaderCode[] shaderCodes = new ShaderCode[Shader.MAX];

        shaderCodes[Shader.VERT] = ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null, 
                VERT_SHADER_SOURCE, "vert", null, true);

        for (int i = 0; i < Program.MAX; i++) {

            shaderCodes[Shader.FRAG_TEXTURE + i] = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(), 
                    SHADERS_ROOT, null, FRAG_SHADER_SOURCE[i], "frag", null, true);

            ShaderProgram program = new ShaderProgram();
            program.add(shaderCodes[Shader.VERT]);
            program.add(shaderCodes[Shader.FRAG_TEXTURE + i]);

            program.init(gl3);

            programName[i] = program.program();

            gl3.glBindAttribLocation(programName[i], Semantic.Attr.POSITION, "position");
            gl3.glBindAttribLocation(programName[i], Semantic.Attr.TEXCOORD, "texCoord");
            gl3.glBindFragDataLocation(programName[i], Semantic.Frag.COLOR, "color");

            program.link(gl3, System.out);
        }

        if (validated) {

            for (int i = 0; i < Program.MAX; i++) {
                uniformMvp[i] = gl3.glGetUniformLocation(programName[i], "mvp");
                uniformDiffuse[i] = gl3.glGetUniformLocation(programName[i], "diffuse");
            }
        }
        return validated & checkError(gl3, "initProgram");
    }

    private boolean initBuffer(GL3 gl3) {

        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);

        gl3.glGenBuffers(1, bufferName);
        gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(0));
        gl3.glBufferData(GL_ARRAY_BUFFER, vertexSize, vertexBuffer, GL_STATIC_DRAW);
        gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

        BufferUtils.destroyDirectBuffer(vertexBuffer);

        return checkError(gl3, "initBuffer");
    }

    private boolean initTexture(GL3 gl3) {

        try {
            jgli.Texture2d texture = new Texture2d(jgli.Load.load(TEXTURE_ROOT + "/" + TEXTURE_DIFFUSE));

            gl3.glGenTextures(Texture.MAX, textureName);

            gl3.glActiveTexture(GL_TEXTURE0);
            gl3.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.DIFFUSE));
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            gl3.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

            jgli.Gl.Format format = jgli.Gl.translate(texture.format());
            for (int level = 0; level < texture.levels(); level++) {
                gl3.glTexImage2D(GL_TEXTURE_2D, level,
                        format.internal.value,
                        texture.dimensions(level)[0], texture.dimensions(level)[1],
                        0,
                        format.external.value, format.type.value,
                        texture.data(level));
            }

            gl3.glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, textureName.get(Texture.MULTISAMPLE_COLORBUFFER));
            gl3.glTexImage2DMultisample(GL_TEXTURE_2D_MULTISAMPLE, 4, GL_RGBA8, FRAMEBUFFER_SIZE.x, FRAMEBUFFER_SIZE.y,
                    true);
            gl3.glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, textureName.get(Texture.MULTISAMPLE_DEPTHBUFFER));
            gl3.glTexImage2DMultisample(GL_TEXTURE_2D_MULTISAMPLE, 4, GL_DEPTH_COMPONENT24, FRAMEBUFFER_SIZE.x,
                    FRAMEBUFFER_SIZE.y, true);
            gl3.glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, 0);

            gl3.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.COLORBUFFER));
            gl3.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, FRAMEBUFFER_SIZE.x, FRAMEBUFFER_SIZE.y, 0, GL_RGBA,
                    GL_UNSIGNED_BYTE, null);
            gl3.glBindTexture(GL_TEXTURE_2D, 0);

        } catch (IOException ex) {
            Logger.getLogger(Gl_320_fbo_multisample_explicit.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

    private boolean initFramebuffer(GL3 gl3) {

        gl3.glGenFramebuffers(1, framebufferRenderName);
        gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferRenderName.get(0));
        gl3.glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, textureName.get(Texture.MULTISAMPLE_COLORBUFFER),
                0);
        gl3.glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, textureName.get(Texture.MULTISAMPLE_DEPTHBUFFER),
                0);

        if (!isFramebufferComplete(gl3, framebufferRenderName.get(0))) {
            return false;
        }
        gl3.glBindFramebuffer(GL_FRAMEBUFFER, 0);

        return true;
    }

    private boolean initVertexArray(GL3 gl3) {

        gl3.glGenVertexArrays(1, vertexArrayName);
        gl3.glBindVertexArray(vertexArrayName.get(0));
        {
            gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(0));
            gl3.glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL_FLOAT, false, glf.Vertex_v2fv2f.SIZE, 0);
            gl3.glVertexAttribPointer(Semantic.Attr.TEXCOORD, 2, GL_FLOAT, false, glf.Vertex_v2fv2f.SIZE, Vec2.SIZE);
            gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

            gl3.glEnableVertexAttribArray(Semantic.Attr.POSITION);
            gl3.glEnableVertexAttribArray(Semantic.Attr.TEXCOORD);
        }
        gl3.glBindVertexArray(0);

        return true;
    }

    @Override
    protected boolean render(GL gl) {

        GL3 gl3 = (GL3) gl;

        // Clear the framebuffer
        gl3.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        gl3.glClearBufferfv(GL_COLOR, 0, new float[]{0.0f, 0.5f, 1.0f, 1.0f}, 0);

        // Pass 1
        // Render the scene in a multisampled framebuffer
        gl3.glEnable(GL_MULTISAMPLE);
        renderFBO(gl3, framebufferRenderName.get(0));
        gl3.glDisable(GL_MULTISAMPLE);

        // Pass 2
        // Resolved and render the colorbuffer from the multisampled framebuffer
        resolveMultisampling(gl3);

        return true;
    }

    private void renderFBO(GL3 gl3, int framebuffer) {

        Mat4 perspective = glm.perspective_((float) Math.PI * 0.25f, (float) FRAMEBUFFER_SIZE.x / FRAMEBUFFER_SIZE.y,
                0.1f, 100.0f);
        Mat4 model = new Mat4(1.0f).scale(new Vec3(1.0f));
        Mat4 mvp = perspective.mul(viewMat4()).mul(model);

        gl3.glEnable(GL_DEPTH_TEST);

        gl3.glUseProgram(programName[Program.THROUGH]);
        gl3.glUniform1i(uniformDiffuse[Program.THROUGH], 0);
        gl3.glUniformMatrix4fv(uniformMvp[Program.THROUGH], 1, false, mvp.toFa_(), 0);

        gl3.glViewport(0, 0, FRAMEBUFFER_SIZE.x, FRAMEBUFFER_SIZE.y);

        gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        float[] depth = new float[]{1.0f};
        gl3.glClearBufferfv(GL_DEPTH, 0, depth, 0);
        gl3.glClearBufferfv(GL_COLOR, 0, new float[]{1.0f, 0.5f, 0.0f, 1.0f}, 0);

        gl3.glActiveTexture(GL_TEXTURE0);
        gl3.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.DIFFUSE));
        gl3.glBindVertexArray(vertexArrayName.get(0));

        gl3.glDrawArraysInstanced(GL_TRIANGLES, 0, vertexCount, 5);

        gl3.glDisable(GL_DEPTH_TEST);
    }

    private void resolveMultisampling(GL3 gl3) {

        Mat4 perspective = glm.ortho_(-1.0f, 1.0f, -1.0f, 1.0f, 0.0f, 100.0f);
        Mat4 viewFlip = new Mat4(1.0f).scale(new Vec3(1.0f, -1.0f, 1.0f));
        Mat4 view = viewFlip.translate(new Vec3(0.0f, 0.0f, -cameraDistance() * 1.0f));
        Mat4 model = new Mat4(1.0f);
        Mat4 mvp = perspective.mul(view).mul(model);

        gl3.glViewport(0, 0, windowSize.x, windowSize.y);
        gl3.glBindFramebuffer(GL_FRAMEBUFFER, 0);

        gl3.glActiveTexture(GL_TEXTURE0);
        gl3.glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, textureName.get(Texture.MULTISAMPLE_COLORBUFFER));

        gl3.glBindVertexArray(vertexArrayName.get(0));

        gl3.glEnable(GL_SCISSOR_TEST);

        // Box
        {
            gl3.glScissor(1, 1, windowSize.x / 2 - 2, windowSize.y - 2);

            gl3.glUseProgram(programName[Program.RESOLVE_BOX]);
            gl3.glUniform1i(uniformDiffuse[Program.RESOLVE_BOX], 0);
            gl3.glUniformMatrix4fv(uniformMvp[Program.RESOLVE_BOX], 1, false, mvp.toFa_(), 0);

            gl3.glDrawArraysInstanced(GL_TRIANGLES, 0, vertexCount, 5);
        }

        // Near
        {
            gl3.glScissor(windowSize.x / 2 + 1, 1, windowSize.x / 2 - 2, windowSize.y - 2);

            gl3.glUseProgram(programName[Program.RESOLVE_NEAR]);
            gl3.glUniform1i(uniformDiffuse[Program.RESOLVE_NEAR], 0);
            gl3.glUniformMatrix4fv(uniformMvp[Program.RESOLVE_NEAR], 1, false, mvp.toFa_(), 0);

            gl3.glDrawArraysInstanced(GL_TRIANGLES, 0, vertexCount, 5);
        }

        gl3.glDisable(GL_SCISSOR_TEST);
    }

    @Override
    protected boolean end(GL gl) {

        GL3 gl3 = (GL3) gl;

        for (int i = 0; i < Program.MAX; ++i) {
            gl3.glDeleteProgram(programName[i]);
        }

        gl3.glDeleteBuffers(1, bufferName);
        gl3.glDeleteTextures(Texture.MAX, textureName);
        gl3.glDeleteFramebuffers(1, framebufferRenderName);
        gl3.glDeleteVertexArrays(1, vertexArrayName);

        BufferUtils.destroyDirectBuffer(bufferName);
        BufferUtils.destroyDirectBuffer(textureName);
        BufferUtils.destroyDirectBuffer(framebufferRenderName);
        BufferUtils.destroyDirectBuffer(vertexArrayName);

        return checkError(gl3, "end");
    }
}
