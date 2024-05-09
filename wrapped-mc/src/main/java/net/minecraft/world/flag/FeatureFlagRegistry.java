package net.minecraft.world.flag;

import io.github.nickid2018.util.SneakyUtil;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class FeatureFlagRegistry {

    public final Map<ResourceLocation, ?> names = SneakyUtil.sneakyNotNull();

    public FeatureFlagSet fromNames(Iterable<ResourceLocation> iterable) {
        throw new RuntimeException();
    }
}
