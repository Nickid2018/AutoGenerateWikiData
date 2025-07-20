package com.mojang.blaze3d.buffers;

import java.nio.ByteBuffer;

public abstract class GpuBuffer implements AutoCloseable {

    public interface MappedView extends AutoCloseable {
        ByteBuffer data();

        @Override
        void close();
    }
}
