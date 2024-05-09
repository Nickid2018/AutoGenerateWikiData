package net.minecraft.world.level.biome;

import io.github.nickid2018.util.SneakyUtil;

public final class Biome {

    public int getSkyColor() {
        return SneakyUtil.sneakyInt();
    }

    public MobSpawnSettings getMobSettings() {
        return SneakyUtil.sneakyNotNull();
    }

    public boolean hasPrecipitation() {
        return SneakyUtil.sneakyBool();
    }

    public int getFogColor() {
        return SneakyUtil.sneakyInt();
    }

    public int getFoliageColor() {
        return SneakyUtil.sneakyInt();
    }

    public float getBaseTemperature() {
        return SneakyUtil.sneakyInt();
    }

    public int getWaterColor() {
        return SneakyUtil.sneakyInt();
    }

    public int getWaterFogColor() {
        return SneakyUtil.sneakyInt();
    }
}
