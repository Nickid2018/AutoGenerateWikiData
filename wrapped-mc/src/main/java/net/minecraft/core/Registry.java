package net.minecraft.core;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

public interface Registry<T> {

    T get(ResourceKey<T> var1);

    ResourceLocation getKey(T var1);

    Set<ResourceKey<T>> registryKeySet();

    Optional<Holder<T>> getHolder(ResourceKey<T> var1);

    Holder<T> wrapAsHolder(T var1);
}
