package net.minecraft.core;

import net.minecraft.resources.ResourceKey;

import java.util.stream.Stream;

public interface RegistryAccess extends HolderLookup.Provider {

    default <E> Registry<E> lookupOrThrow(ResourceKey<? extends Registry<? extends E>> resourceKey) {
        throw new RuntimeException();
    }

    Stream<RegistryEntry<?>> registries();

    record RegistryEntry<T>(ResourceKey<? extends Registry<T>> key, Registry<T> value) {
    }

    interface Frozen extends RegistryAccess {
    }
}
