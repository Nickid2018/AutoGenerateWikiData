package com.mojang.blaze3d.systems;

import io.github.nickid2018.util.SneakyUtil;
import org.joml.Vector3f;

public class RenderSystem {

    public static void assertOnRenderThread() {
    }

    public static GpuDevice getDevice() {
        return SneakyUtil.sneakyNotNull();
    }

    public static void setShaderLights(Vector3f vector3f, Vector3f vector3f2) {
        throw new RuntimeException();
    }
}
