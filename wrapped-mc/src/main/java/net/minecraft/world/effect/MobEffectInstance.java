package net.minecraft.world.effect;

import net.minecraft.core.Holder;

public class MobEffectInstance implements Comparable<MobEffectInstance> {

    public MobEffectInstance(Holder<MobEffect> holder) {
    }

    public Holder<MobEffect> getEffect() {
        throw new RuntimeException();
    }

    public int getDuration() {
        throw new RuntimeException();
    }

    public int getAmplifier() {
        throw new RuntimeException();
    }

    @Override
    public int compareTo(MobEffectInstance o) {
        throw new RuntimeException();
    }
}
