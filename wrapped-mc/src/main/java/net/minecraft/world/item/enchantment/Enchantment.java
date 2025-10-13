package net.minecraft.world.item.enchantment;

import io.github.nickid2018.util.SneakyUtil;
import net.minecraft.core.Holder;

public record Enchantment() {

    public int getMaxLevel() {
        return SneakyUtil.sneakyInt();
    }

    public int getMinCost(int n) {
        return SneakyUtil.sneakyInt();
    }

    public int getMaxCost(int n) {
        return SneakyUtil.sneakyInt();
    }

    public static boolean areCompatible(Holder<Enchantment> holder, Holder<Enchantment> holder2) {
        return SneakyUtil.sneakyBool();
    }
}
