package net.minecraft.server.level;

import io.github.nickid2018.util.SneakyUtil;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.concurrent.CompletableFuture;

public class ServerChunkCache {

    public final DistanceManager distanceManager = SneakyUtil.sneakyNotNull();

    public CompletableFuture<ChunkResult<ChunkAccess>> getChunkFuture(int n, int n2, ChunkStatus chunkStatus, boolean bl) {
        throw new RuntimeException();
    }
}
