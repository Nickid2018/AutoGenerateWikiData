package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.buffers.GpuBuffer;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public interface GpuDevice {

    CommandEncoder createCommandEncoder();

    GpuBuffer createBuffer(@Nullable Supplier<String> var1, int var2, long var3);
}
