package net.minecraft.world.level.biome;

import io.github.nickid2018.util.SneakyUtil;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.Map;

public class MobSpawnSettings {

    public final Map<EntityType<?>, MobSpawnCost> mobSpawnCosts = SneakyUtil.sneakyNotNull();

    public float getCreatureProbability() {
        throw new RuntimeException();
    }

    public WeightedList<SpawnerData> getMobs(MobCategory mobCategory) {
        throw new RuntimeException();
    }

    public record SpawnerData(EntityType<?> type, int minCount, int maxCount) {
    }

    public record MobSpawnCost(double energyBudget, double charge) {
    }
}
