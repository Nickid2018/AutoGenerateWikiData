package net.minecraft.core;

import net.minecraft.resources.ResourceKey;

import java.util.Optional;

public interface Holder<T> {

    T value();

    public Optional<ResourceKey<T>> unwrapKey();
}
