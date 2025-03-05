package com.mojang.blaze3d.systems;

import io.github.nickid2018.util.SneakyUtil;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class RenderSystem {

    public static void assertOnRenderThread() {
    }

    public static GpuDevice getDevice() {
        return SneakyUtil.sneakyNotNull();
    }
}
