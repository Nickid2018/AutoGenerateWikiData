package net.minecraft.world.entity;

import io.github.nickid2018.util.SneakyUtil;
import net.minecraft.world.effect.MobEffectInstance;

public abstract class LivingEntity extends Entity {

    public final boolean addEffect(MobEffectInstance mobEffectInstance) {
        return SneakyUtil.sneakyBool();
    }
}
