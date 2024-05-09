package net.minecraft.world.level.block.entity;

import io.github.nickid2018.util.SneakyUtil;
import net.minecraft.world.item.Item;

import java.util.Map;

public abstract class AbstractFurnaceBlockEntity {

    public static Map<Item, Integer> getFuel() {
        return SneakyUtil.sneakyNotNull();
    }
}
