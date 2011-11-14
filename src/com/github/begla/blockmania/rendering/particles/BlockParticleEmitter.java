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
package com.github.begla.blockmania.rendering.particles;

import com.github.begla.blockmania.blocks.BlockManager;
import com.github.begla.blockmania.rendering.ShaderManager;
import com.github.begla.blockmania.rendering.TextureManager;
import com.github.begla.blockmania.world.World;
import org.lwjgl.opengl.GL11;

/**
 * Emits block particles.
 *
 * @author Benjamin Glatzel <benjamin.glatzel@me.com>
 */
public class BlockParticleEmitter extends ParticleEmitter {

    private World _parent;
    private byte _currentBlockType = BlockManager.getInstance().getBlock("Dirt").getId();

    public BlockParticleEmitter(World parent) {
        _parent = parent;
    }

    public void emitParticles(int amount, byte blockType) {
        _currentBlockType = blockType;
        super.emitParticles(amount);
    }

    public void render() {
        TextureManager.getInstance().bindTexture("terrain");

        ShaderManager.getInstance().enableShader("particle");
        GL11.glPushMatrix();
        GL11.glTranslatef(-_parent.getWorldProvider().getRenderingReferencePoint().x,-_parent.getWorldProvider().getRenderingReferencePoint().y, -_parent.getWorldProvider().getRenderingReferencePoint().z);
        super.render();
        GL11.glPopMatrix();
        ShaderManager.getInstance().enableShader(null);
    }

    public World getParent() {
        return _parent;
    }

    @Override
    protected Particle createParticle() {
        return new BlockParticle(256, _origin, _currentBlockType, this);
    }
}
