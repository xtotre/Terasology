/*
 * Copyright 2011 Benjamin Glatzel <benjamin.glatzel@me.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.logic.manager;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.terasology.utilities.MathHelper;

import java.nio.FloatBuffer;
import java.util.HashMap;

import static org.lwjgl.opengl.GL11.*;

/**
 * TODO
 *
 * @author Benjamin Glatzel <benjamin.glatzel@me.com>
 */
public class PostProcessingRenderer {

    public static final boolean EFFECTS_ENABLED = (Boolean) ConfigurationManager.getInstance().getConfig().get("Graphics.enablePostProcessingEffects");
    public static final float MAX_EXPOSURE = 5.0f;

    private static PostProcessingRenderer _instance = null;
    private float _exposure;
    private int _displayListQuad = -1;

    public class FBO {
        public int _fboId = 0;
        public int _textureId = 0;
        public int _depthTextureId = 0;
        public int _depthRboId = 0;

        public int _width = 0;
        public int _height = 0;

        public void bind() {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, _fboId);
        }

        public void unbind() {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        }

        public void bindDepthTexture() {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, _depthTextureId);
        }

        public void bindTexture() {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, _textureId);
        }

        public void unbindTexture() {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }
    }

    private HashMap<String, FBO> _FBOs = new HashMap<String, FBO>();

    /**
     * Returns (and creates – if necessary) the static instance
     * of this helper class.
     *
     * @return The instance
     */
    public static PostProcessingRenderer getInstance() {
        if (_instance == null) {
            _instance = new PostProcessingRenderer();
        }

        return _instance;
    }

    public PostProcessingRenderer() {
        createOrUpdateFullscreenFbos();

        if (EFFECTS_ENABLED) {
            createFBO("sceneHighPass", 1024, 1024, true, false);
            createFBO("sceneBloom0", 1024, 1024, true, false);
            createFBO("sceneBloom1", 1024, 1024, true, false);

            createFBO("sceneBlur0", 1024, 1024, true, false);
            createFBO("sceneBlur1", 1024, 1024, true, false);

            createFBO("scene64", 64, 64, true, false);
            createFBO("scene32", 32, 32, true, false);
            createFBO("scene16", 16, 16, true, false);
            createFBO("scene8", 8, 8, true, false);
            createFBO("scene4", 4, 4, true, false);
            createFBO("scene2", 2, 2, true, false);
            createFBO("scene1", 1, 1, true, false);
        }
    }

    public void deleteFBO(String title) {
        if (_FBOs.containsKey(title)) {
            FBO fbo = _FBOs.get(title);

            GL30.glDeleteFramebuffers(fbo._fboId);
            GL30.glDeleteRenderbuffers(fbo._depthRboId);
            GL11.glDeleteTextures(fbo._depthTextureId);
            GL11.glDeleteTextures(fbo._textureId);
        }
    }

    public FBO createFBO(String title, int width, int height, boolean hdr, boolean depth) {
        // Make sure to delete the existing FBO before creating a new one
        deleteFBO(title);

        // Create a new FBO object
        FBO fbo = new FBO();
        fbo._width = width;
        fbo._height = height;

        // Create the color target texture
        fbo._textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbo._textureId);

        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        if (hdr)
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, width, height, 0, GL11.GL_RGBA, GL30.GL_HALF_FLOAT, (java.nio.ByteBuffer) null);
        else
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);

        if (depth) {
            // Generate the depth texture
            fbo._depthTextureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbo._depthTextureId);

            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT24, width, height, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (java.nio.ByteBuffer) null);

            // Create depth render buffer object
            fbo._depthRboId = GL30.glGenRenderbuffers();
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, fbo._depthRboId);
            GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL14.GL_DEPTH_COMPONENT24, width, height);
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        // Create the FBO
        fbo._fboId = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo._fboId);

        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, fbo._textureId, 0);

        if (depth) {
            // Generate the depth render buffer and depth map texture
            GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, fbo._depthRboId);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, fbo._depthTextureId, 0);
        }

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

        _FBOs.put(title, fbo);
        return fbo;
    }

    public FBO getFBO(String title) {
        return _FBOs.get(title);
    }

    private void updateExposure() {
        FloatBuffer pixels = BufferUtils.createFloatBuffer(4);
        FBO scene = PostProcessingRenderer.getInstance().getFBO("scene1");

        scene.bindTexture();
        glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_FLOAT, pixels);
        scene.unbindTexture();

        float lum = 0.2126f * pixels.get(0) + 0.7152f * pixels.get(1) + 0.0722f * pixels.get(2);

        if (lum > 0.0f) // No division by zero
            _exposure = (float) MathHelper.lerp(_exposure, 0.5f / lum, 0.01);

        if (_exposure > MAX_EXPOSURE)
            _exposure = MAX_EXPOSURE;
    }

    /**
     * Renders the final scene to a quad and displays it. The FBO gets automatically rescaled if the size
     * of the viewport changes.
     */
    public void renderScene() {
        if (EFFECTS_ENABLED) {
            generateDownsampledScene();
            updateExposure();
            generateHighPass();

            for (int i = 0; i < 2; i++) {
                generateBloom(i);
                generateBlur(i);
            }

            ShaderManager.getInstance().enableShader("post");
            PostProcessingRenderer.FBO scene = PostProcessingRenderer.getInstance().getFBO("scene");

            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            PostProcessingRenderer.getInstance().getFBO("sceneBloom1").bindTexture();
            GL13.glActiveTexture(GL13.GL_TEXTURE2);
            scene.bindDepthTexture();
            GL13.glActiveTexture(GL13.GL_TEXTURE3);
            PostProcessingRenderer.getInstance().getFBO("sceneBlur1").bindTexture();
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            PostProcessingRenderer.getInstance().getFBO("scene").bindTexture();

            int texScene = GL20.glGetUniformLocation(ShaderManager.getInstance().getShader("post"), "texScene");
            GL20.glUniform1i(texScene, 0);
            int texBloom = GL20.glGetUniformLocation(ShaderManager.getInstance().getShader("post"), "texBloom");
            GL20.glUniform1i(texBloom, 1);
            int texDepth = GL20.glGetUniformLocation(ShaderManager.getInstance().getShader("post"), "texDepth");
            GL20.glUniform1i(texDepth, 2);
            int texBlur = GL20.glGetUniformLocation(ShaderManager.getInstance().getShader("post"), "texBlur");
            GL20.glUniform1i(texBlur, 3);

            int expos = GL20.glGetUniformLocation(ShaderManager.getInstance().getShader("post"), "exposure");
            GL20.glUniform1f(expos, _exposure);

            renderFullQuad();

            scene.unbindTexture();
            ShaderManager.getInstance().enableShader(null);
        } else {
            PostProcessingRenderer.FBO scene = PostProcessingRenderer.getInstance().getFBO("scene");

            scene.bindTexture();
            glEnable(GL11.GL_TEXTURE_2D);

            renderFullQuad();

            glDisable(GL11.GL_TEXTURE_2D);
            scene.unbindTexture();
        }

        createOrUpdateFullscreenFbos();
    }

    /**
     * Initially creates the scene FBO and updates it according to the size of the viewport.
     */
    private void createOrUpdateFullscreenFbos() {
        if (!_FBOs.containsKey("scene")) {
            createFBO("scene", Display.getWidth(), Display.getHeight(), true, true);

        } else {
            FBO scene = getFBO("scene");

            if (scene._width != Display.getWidth() || scene._height != Display.getHeight()) {
                createFBO("scene", Display.getWidth(), Display.getHeight(), true, true);
            }
        }
    }

    private void generateHighPass() {
        ShaderManager.getInstance().enableShader("highp");

        PostProcessingRenderer.getInstance().getFBO("sceneHighPass").bind();
        glViewport(0, 0, 1024, 1024);

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        PostProcessingRenderer.getInstance().getFBO("scene").bindTexture();

        renderFullQuad();

        PostProcessingRenderer.getInstance().getFBO("sceneHighPass").unbind();

        ShaderManager.getInstance().enableShader(null);
        glViewport(0, 0, Display.getWidth(), Display.getHeight());
    }

    private void generateBlur(int id) {
        ShaderManager.getInstance().enableShader("blur");

        int radius = GL20.glGetUniformLocation(ShaderManager.getInstance().getShader("blur"), "radius");
        GL20.glUniform1f(radius, 2.0f);

        PostProcessingRenderer.getInstance().getFBO("sceneBlur" + id).bind();
        glViewport(0, 0, 1024, 1024);

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        if (id == 0)
            PostProcessingRenderer.getInstance().getFBO("scene").bindTexture();
        else
            PostProcessingRenderer.getInstance().getFBO("sceneBlur" + (id - 1)).bindTexture();

        renderFullQuad();

        PostProcessingRenderer.getInstance().getFBO("sceneBlur" + id).unbind();

        ShaderManager.getInstance().enableShader(null);
        glViewport(0, 0, Display.getWidth(), Display.getHeight());
    }

    private void generateBloom(int id) {
        ShaderManager.getInstance().enableShader("blur");

        int radius = GL20.glGetUniformLocation(ShaderManager.getInstance().getShader("blur"), "radius");
        GL20.glUniform1f(radius, 16.0f);

        PostProcessingRenderer.getInstance().getFBO("sceneBloom" + id).bind();
        glViewport(0, 0, 1024, 1024);

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        if (id == 0)
            PostProcessingRenderer.getInstance().getFBO("sceneHighPass").bindTexture();
        else
            PostProcessingRenderer.getInstance().getFBO("sceneBloom" + (id - 1)).bindTexture();

        renderFullQuad();

        PostProcessingRenderer.getInstance().getFBO("sceneBloom" + id).unbind();

        ShaderManager.getInstance().enableShader(null);
        glViewport(0, 0, Display.getWidth(), Display.getHeight());
    }

    private void generateDownsampledScene() {
        ShaderManager.getInstance().enableShader("down");

        for (int i = 6; i >= 0; i--) {
            int sizePrev = (int) Math.pow(2, i + 1);
            int size = (int) Math.pow(2, i);

            int textureSize = GL20.glGetUniformLocation(ShaderManager.getInstance().getShader("down"), "size");
            GL20.glUniform1f(textureSize, size);

            PostProcessingRenderer.getInstance().getFBO("scene" + size).bind();
            glViewport(0, 0, size, size);

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            if (i == 6)
                PostProcessingRenderer.getInstance().getFBO("scene").bindTexture();
            else
                PostProcessingRenderer.getInstance().getFBO("scene" + sizePrev).bindTexture();

            renderFullQuad();

            PostProcessingRenderer.getInstance().getFBO("scene" + size).unbind();

        }

        ShaderManager.getInstance().enableShader(null);
        glViewport(0, 0, Display.getWidth(), Display.getHeight());
    }

    private void renderFullQuad() {
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();

        renderQuad();

        glPopMatrix();

        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
    }

    private void renderQuad() {
        if (_displayListQuad == -1) {
            _displayListQuad = glGenLists(1);

            glNewList(_displayListQuad, GL11.GL_COMPILE);

            glBegin(GL_QUADS);
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

            glTexCoord2d(0.0, 0.0);
            glVertex3i(-1, -1, -1);

            glTexCoord2d(1.0, 0.0);
            glVertex3i(1, -1, -1);

            glTexCoord2d(1.0, 1.0);
            glVertex3i(1, 1, -1);

            glTexCoord2d(0.0, 1.0);
            glVertex3i(-1, 1, -1);

            glEnd();

            glEndList();
        }

        glCallList(_displayListQuad);
    }

    public float getExposure() {
        return _exposure;
    }
}
