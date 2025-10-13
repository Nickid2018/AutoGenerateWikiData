package net.minecraft.core.registries;

import io.github.nickid2018.util.SneakyUtil;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

public class Registries {

    public static final ResourceKey<Registry<Enchantment>> ENCHANTMENT = SneakyUtil.sneakyNotNull();
}
