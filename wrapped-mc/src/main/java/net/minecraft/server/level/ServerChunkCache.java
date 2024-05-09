package net.minecraft.server.level;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.concurrent.CompletableFuture;

public class ServerChunkCache {

    public CompletableFuture<ChunkResult<ChunkAccess>> getChunkFuture(int n, int n2, ChunkStatus chunkStatus, boolean bl) {
        throw new RuntimeException();
    }
}
