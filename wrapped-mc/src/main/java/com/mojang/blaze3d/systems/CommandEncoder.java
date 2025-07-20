package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.textures.GpuTexture;

public interface CommandEncoder {

    void clearColorAndDepthTextures(GpuTexture var1, int var2, GpuTexture var3, double var4);

    void copyTextureToBuffer(GpuTexture var1, GpuBuffer var2, int var3, Runnable var4, int var5);

    GpuBuffer.MappedView mapBuffer(GpuBuffer var1, boolean b, boolean b1);
}
