package net.minecraft.world.effect;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.function.BiConsumer;

public class MobEffect {

    public MobEffectCategory getCategory() {
        throw new RuntimeException();
    }

    public int getColor() {
        throw new RuntimeException();
    }

    public boolean isInstantenous() {
        throw new RuntimeException();
    }

    public void createModifiers(int n, BiConsumer<Holder<?>, AttributeModifier> biConsumer) {
        throw new RuntimeException();
    }
}
