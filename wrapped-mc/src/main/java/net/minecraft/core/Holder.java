package net.minecraft.core;

import net.minecraft.resources.ResourceKey;

import java.util.Optional;

public interface Holder<T> {

    T value();

    Optional<ResourceKey<T>> unwrapKey();
}
