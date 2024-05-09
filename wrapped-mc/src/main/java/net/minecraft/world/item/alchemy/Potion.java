package net.minecraft.world.item.alchemy;

import io.github.nickid2018.util.SneakyUtil;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.List;
import java.util.Optional;

public class Potion {

    public static String getName(Optional<Holder<Potion>> optional, String string) {
        return SneakyUtil.sneakyNotNull();
    }

    public List<MobEffectInstance> getEffects() {
        throw new RuntimeException();
    }
}
