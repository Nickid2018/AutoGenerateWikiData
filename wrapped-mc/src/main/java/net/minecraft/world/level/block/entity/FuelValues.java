package net.minecraft.world.level.block.entity;

import io.github.nickid2018.util.SneakyUtil;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import net.minecraft.world.item.Item;

public class FuelValues {

    public final Object2IntSortedMap<Item> values = SneakyUtil.sneakyNotNull();
}
