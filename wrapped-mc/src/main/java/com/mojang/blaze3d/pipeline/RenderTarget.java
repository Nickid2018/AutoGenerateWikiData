package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.textures.GpuTexture;
import io.github.nickid2018.util.SneakyUtil;

public abstract class RenderTarget {

    public int width;
    public int height;

    public GpuTexture getColorTexture() {
        return SneakyUtil.sneakyNotNull();
    }

    public GpuTexture getDepthTexture() {
        return SneakyUtil.sneakyNotNull();
    }
}
