package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.textures.GpuTexture;

public abstract class RenderTarget {

    public int width;
    public int height;

    public void bindWrite(boolean bl) {
        throw new RuntimeException();
    }

    public GpuTexture getColorTexture() {
        throw new RuntimeException();
    }
}
