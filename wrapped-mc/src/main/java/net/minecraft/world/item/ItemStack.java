package net.minecraft.world.item;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.ItemLike;

public class ItemStack {

    public ItemStack(ItemLike item) {
    }

    public Item getItem() {
        throw new RuntimeException();
    }

    public DataComponentMap getComponents() {
        throw new RuntimeException();
    }
}
