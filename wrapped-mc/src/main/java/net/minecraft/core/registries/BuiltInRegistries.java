package net.minecraft.core.registries;

import io.github.nickid2018.util.SneakyUtil;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.block.Block;

public class BuiltInRegistries {

    public static final DefaultedRegistry<Item> ITEM = SneakyUtil.sneakyNotNull();
    public static final Registry<CreativeModeTab> CREATIVE_MODE_TAB = SneakyUtil.sneakyNotNull();
    public static final DefaultedRegistry<EntityType<?>> ENTITY_TYPE = SneakyUtil.sneakyNotNull();
    public static final Registry<MobEffect> MOB_EFFECT = SneakyUtil.sneakyNotNull();
    public static final Registry<Potion> POTION = SneakyUtil.sneakyNotNull();
    public static final DefaultedRegistry<Block> BLOCK = SneakyUtil.sneakyNotNull();
}
