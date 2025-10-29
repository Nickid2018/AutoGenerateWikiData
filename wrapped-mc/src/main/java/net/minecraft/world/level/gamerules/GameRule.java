package net.minecraft.world.level.gamerules;

import io.github.nickid2018.util.SneakyUtil;

public class GameRule<T> {

    public T defaultValue() {
        return SneakyUtil.sneakyNotNull();
    }

    public String getDescriptionId() {
        return SneakyUtil.sneakyNotNull();
    }

    public GameRuleCategory category() {
        return SneakyUtil.sneakyNotNull();
    }
}
