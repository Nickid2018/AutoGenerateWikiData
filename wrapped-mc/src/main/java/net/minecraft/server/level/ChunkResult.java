package net.minecraft.server.level;

public interface ChunkResult<T> {

    T orElse(T var1);
}
