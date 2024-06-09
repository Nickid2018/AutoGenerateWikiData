package com.mojang.blaze3d.pipeline;

public abstract class RenderTarget {

    public int width;
    public int height;

    public void bindWrite(boolean bl) {
        throw new RuntimeException();
    }

    public int getColorTextureId() {
        throw new RuntimeException();
    }
}
