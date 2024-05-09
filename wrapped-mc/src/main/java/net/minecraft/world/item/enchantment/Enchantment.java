package net.minecraft.world.item.enchantment;

import io.github.nickid2018.util.SneakyUtil;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;

public record Enchantment() {

    public boolean isPrimaryItem(ItemStack itemStack) {
        return SneakyUtil.sneakyBool();
    }

    public boolean isSupportedItem(ItemStack itemStack) {
        return SneakyUtil.sneakyBool();
    }

    public int getWeight() {
        return SneakyUtil.sneakyInt();
    }

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
