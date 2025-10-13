package net.minecraft.world.level.chunk;

import net.minecraft.core.Holder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.biome.Biome;

public abstract class ChunkAccess implements BlockGetter {

    public Holder<Biome> getNoiseBiome(int n, int n2, int n3) {
        throw new RuntimeException();
    }
}
