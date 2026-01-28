package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.buffers.GpuBuffer;
import io.github.nickid2018.util.SneakyUtil;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class GpuDevice {

    public CommandEncoder createCommandEncoder() {
        return SneakyUtil.sneakyNotNull();
    }

    public GpuBuffer createBuffer(@Nullable Supplier<String> label, int usage, long size) {
        return SneakyUtil.sneakyNotNull();
    }
}
