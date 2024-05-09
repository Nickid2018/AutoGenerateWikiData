package net.minecraft.world.item;

import net.minecraft.world.level.ItemLike;

public class Item implements ItemLike {

    public ItemStack getDefaultInstance() {
        throw new RuntimeException();
    }
}
