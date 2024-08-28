package net.minecraft.core;

import net.minecraft.resources.ResourceKey;

public interface RegistryAccess extends HolderLookup.Provider {

    default <E> Registry<E> lookupOrThrow(ResourceKey<? extends Registry<? extends E>> resourceKey) {
        throw new RuntimeException();
    }

    interface Frozen extends RegistryAccess {
    }
}
