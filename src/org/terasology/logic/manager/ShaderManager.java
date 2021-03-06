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
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.newdawn.slick.util.ResourceLoader;
import org.terasology.game.Terasology;
import org.terasology.model.blocks.Block;
import org.terasology.rendering.shader.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.logging.Level;

/**
 * Provides support for loading and applying shaders.
 *
 * @author Benjamin Glatzel <benjamin.glatzel@me.com>
 */
public class ShaderManager {

    private final HashMap<String, Integer> _shaderPrograms = new HashMap<String, Integer>(32);
    private final HashMap<String, Integer> _fragmentShader = new HashMap<String, Integer>(32);
    private final HashMap<String, Integer> _vertexShader = new HashMap<String, Integer>(32);
    private final HashMap<String, ShaderParameters> _shaderParameters = new HashMap<String, ShaderParameters>(32);

    private static ShaderManager _instance = null;

    private String _preProcessorPreamble = "#version 120 \n float TEXTURE_OFFSET = " + Block.TEXTURE_OFFSET + "; \n";
    private String _includedFunctionsVertex = "", _includedFunctionsFragment = "";

    /**
     * Returns (and creates – if necessary) the static instance
     * of this helper class.
     *
     * @return The instance
     */
    public static ShaderManager getInstance() {
        if (_instance == null) {
            _instance = new ShaderManager();
        }

        return _instance;
    }

    private ShaderManager() {
        Terasology.getInstance().getLogger().log(Level.INFO, "Loading Terasology shader manager...");
        Terasology.getInstance().getLogger().log(Level.INFO, "GL_VERSION: {0}", GL11.glGetString(GL11.GL_VERSION));
        Terasology.getInstance().getLogger().log(Level.INFO, "SHADING_LANGUAGE VERSION: {0}", GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION));
        Terasology.getInstance().getLogger().log(Level.INFO, "EXTENSIONS: {0}", GL11.glGetString(GL11.GL_EXTENSIONS));

        initShaders();
    }

    private void initShaders() {
        _preProcessorPreamble += ((Boolean) ConfigurationManager.getInstance().getConfig().get("Graphics.animatedWaterAndGrass")) ? "#define ANIMATED_WATER_AND_GRASS \n" : "";
        _preProcessorPreamble += "#define GAMMA " + ConfigurationManager.getInstance().getConfig().get("Graphics.gamma").toString() + "\n";

        _includedFunctionsFragment += readShader("globalFunctionsFragIncl.glsl");
        _includedFunctionsVertex += readShader("globalFunctionsVertIncl.glsl");

        createShader("highp_frag.glsl", "highp", GL20.GL_FRAGMENT_SHADER);
        createShader("highp_vert.glsl", "highp", GL20.GL_VERTEX_SHADER);

        createShader("blur_frag.glsl", "blur", GL20.GL_FRAGMENT_SHADER);
        createShader("blur_vert.glsl", "blur", GL20.GL_VERTEX_SHADER);

        createShader("down_vert.glsl", "down", GL20.GL_VERTEX_SHADER);
        createShader("down_frag.glsl", "down", GL20.GL_FRAGMENT_SHADER);

        createShader("post_vert.glsl", "post", GL20.GL_VERTEX_SHADER);
        createShader("post_frag.glsl", "post", GL20.GL_FRAGMENT_SHADER);
        createShaderParameter("post", new ShaderParametersPostProcessing());

        createShader("sky_vert.glsl", "sky", GL20.GL_VERTEX_SHADER);
        createShader("sky_frag.glsl", "sky", GL20.GL_FRAGMENT_SHADER);

        createShader("chunk_vert.glsl", "chunk", GL20.GL_VERTEX_SHADER);
        createShader("chunk_frag.glsl", "chunk", GL20.GL_FRAGMENT_SHADER);
        createShaderParameter("chunk", new ShaderParametersChunk());

        createShader("particle_vert.glsl", "particle", GL20.GL_VERTEX_SHADER);
        createShader("particle_frag.glsl", "particle", GL20.GL_FRAGMENT_SHADER);
        createShaderParameter("particle", new ShaderParametersParticle());

        createShader("block_vert.glsl", "block", GL20.GL_VERTEX_SHADER);
        createShader("block_frag.glsl", "block", GL20.GL_FRAGMENT_SHADER);
        createShaderParameter("block", new ShaderParametersBlock());

        createShader("gelatinousCube_vert.glsl", "gelatinousCube", GL20.GL_VERTEX_SHADER);
        createShader("gelatinousCube_frag.glsl", "gelatinousCube", GL20.GL_FRAGMENT_SHADER);
        createShaderParameter("gelatinousCube", new ShaderParametersGelCube());

        createShader("clouds_vert.glsl", "clouds", GL20.GL_VERTEX_SHADER);
        createShader("clouds_frag.glsl", "clouds", GL20.GL_FRAGMENT_SHADER);

        for (String s : _fragmentShader.keySet()) {
            int shaderProgram = GL20.glCreateProgram();

            GL20.glAttachShader(shaderProgram, _fragmentShader.get(s));
            GL20.glAttachShader(shaderProgram, _vertexShader.get(s));
            GL20.glLinkProgram(shaderProgram);
            GL20.glValidateProgram(shaderProgram);

            _shaderPrograms.put(s, shaderProgram);
        }
    }

    private void createShaderParameter(String title, ShaderParameters param) {
        _shaderParameters.put(title, param);
    }

    private int createShader(String filename, String title, int type) {
        Terasology.getInstance().getLogger().log(Level.INFO, "Loading shader {0} ({1}, type = {2})", new String[]{title, filename, String.valueOf(type)});

        HashMap<String, Integer> shaders;

        if (type == GL20.GL_FRAGMENT_SHADER) {
            shaders = _fragmentShader;
        } else if (type == GL20.GL_VERTEX_SHADER) {
            shaders = _vertexShader;
        } else {
            return 0;
        }

        shaders.put(title, GL20.glCreateShader(type));

        if (shaders.get(title) == 0) {
            return 0;
        }

        String code = _preProcessorPreamble + "\n";
        if (type == GL20.GL_FRAGMENT_SHADER)
            code += _includedFunctionsFragment + "\n";
        else
            code += _includedFunctionsVertex + "\n";
        code += readShader(filename);

        GL20.glShaderSource(shaders.get(title), code);
        GL20.glCompileShader(shaders.get(title));

        printLogInfo(shaders.get(title));

        return shaders.get(title);
    }

    private String readShader(String filename) {
        String line, code = "";
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(ResourceLoader.getResource("org/terasology/data/shaders/" + filename).openStream()));
            while ((line = reader.readLine()) != null) {
                code += line + "\n";
            }
        } catch (Exception e) {
            Terasology.getInstance().getLogger().log(Level.SEVERE, "Failed to read shader.");
        } finally {
            return code;
        }
    }

    private static void printLogInfo(int obj) {
        IntBuffer intBuffer = BufferUtils.createIntBuffer(1);
        GL20.glGetShader(obj, GL20.GL_INFO_LOG_LENGTH, intBuffer);

        int length = intBuffer.get();

        if (length <= 1) {
            return;
        }

        ByteBuffer infoBuffer = BufferUtils.createByteBuffer(length);
        intBuffer.flip();

        GL20.glGetShaderInfoLog(obj, intBuffer, infoBuffer);

        int actualLength = intBuffer.get();
        byte[] infoBytes = new byte[actualLength];
        infoBuffer.get(infoBytes);

        Terasology.getInstance().getLogger().log(Level.INFO, "{0}", new String(infoBytes));
    }

    /**
     * @param s Name of the shader to activate
     */
    public void enableShader(String s) {
        if (s == null) {
            GL20.glUseProgram(0);
            return;
        }

        int shader = getShader(s);
        GL20.glUseProgram(shader);

        // Set the shader parameters if available
        ShaderParameters param = _shaderParameters.get(s);
        if (param != null) {
            param.applyParameters();
        }
    }

    /**
     * @param s Nave of the shader to return
     * @return The id of the requested shader
     */
    public int getShader(String s) {
        return _shaderPrograms.get(s);
    }

    public ShaderParameters getShaderParameters(String s) {
        return _shaderParameters.get(s);
    }
}
