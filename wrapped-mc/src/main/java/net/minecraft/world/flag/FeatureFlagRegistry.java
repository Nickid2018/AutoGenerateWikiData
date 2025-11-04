package net.minecraft.world.flag;

import io.github.nickid2018.util.SneakyUtil;
import net.minecraft.resources.Identifier;

import java.util.Map;

public class FeatureFlagRegistry {

    public final Map<Identifier, ?> names = SneakyUtil.sneakyNotNull();

    public FeatureFlagSet fromNames(Iterable<Identifier> iterable) {
        throw new RuntimeException();
    }
}
