package net.minecraft.core.component;

import com.mojang.serialization.Codec;

public interface DataComponentType<T> {

    Codec<T> codec();
}
