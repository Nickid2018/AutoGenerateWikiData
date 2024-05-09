package net.minecraft.world.level;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;

public abstract class Level implements LevelAccessor {

    public RegistryAccess registryAccess() {
        throw new RuntimeException();
    }

    public ResourceKey<Level> dimension() {
        throw new RuntimeException();
    }
}
