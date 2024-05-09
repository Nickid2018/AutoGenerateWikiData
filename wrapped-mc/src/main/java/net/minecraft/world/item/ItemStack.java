package net.minecraft.world.item;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.ItemLike;

import java.util.function.BiConsumer;

public class ItemStack {

    public ItemStack(ItemLike item) {
    }

    public void forEachModifier(EquipmentSlot equipmentSlot, BiConsumer<Holder<?>, AttributeModifier> biConsumer) {
        throw new RuntimeException();
    }

    public Item getItem() {
        throw new RuntimeException();
    }

    public int getMaxStackSize() {
        throw new RuntimeException();
    }

    public int getMaxDamage() {
        throw new RuntimeException();
    }

    public Rarity getRarity() {
        throw new RuntimeException();
    }

    public DataComponentMap getComponents() {
        throw new RuntimeException();
    }

    public boolean is(TagKey<Item> tagKey) {
        throw new RuntimeException();
    }
}
