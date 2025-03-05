package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;

import java.util.function.Supplier;

public interface GpuDevice {

    CommandEncoder createCommandEncoder();

    GpuBuffer createBuffer(Supplier<String> var1, BufferType var2, BufferUsage var3, int var4);
}
