package net.minecraft.world.item.enchantment;

import io.github.nickid2018.util.SneakyUtil;

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
}
