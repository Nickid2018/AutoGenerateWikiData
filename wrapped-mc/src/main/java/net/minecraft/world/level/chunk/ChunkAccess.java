package net.minecraft.world.level.chunk;

import net.minecraft.core.Holder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public abstract class ChunkAccess implements BlockGetter {

    public abstract ChunkStatus getStatus();

    public ChunkPos getPos() {
        throw new RuntimeException();
    }

    public Holder<Biome> getNoiseBiome(int n, int n2, int n3) {
        throw new RuntimeException();
    }
}
