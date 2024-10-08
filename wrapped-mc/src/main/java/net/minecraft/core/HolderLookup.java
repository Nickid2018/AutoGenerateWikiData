package net.minecraft.core;

import com.mojang.serialization.DynamicOps;
import io.github.nickid2018.util.SneakyUtil;
import net.minecraft.resources.RegistryOps;

public interface HolderLookup {

    interface Provider {

        default <V> RegistryOps<V> createSerializationContext(DynamicOps<V> dynamicOps) {
            return SneakyUtil.sneakyNotNull();
        }
    }
}
