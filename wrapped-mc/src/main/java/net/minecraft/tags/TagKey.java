package net.minecraft.tags;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

public record TagKey<T>(ResourceKey<? extends Registry<T>> registry, Identifier location) {
}
