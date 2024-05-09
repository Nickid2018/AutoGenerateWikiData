package net.minecraft.world.level.biome;

import io.github.nickid2018.util.SneakyUtil;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.Map;

public class MobSpawnSettings {

    public final Map<EntityType<?>, MobSpawnCost> mobSpawnCosts = SneakyUtil.sneakyNotNull();

    public float getCreatureProbability() {
        throw new RuntimeException();
    }

    public WeightedRandomList<SpawnerData> getMobs(MobCategory mobCategory) {
        throw new RuntimeException();
    }

    public static class SpawnerData implements WeightedEntry {

        public final EntityType<?> type = SneakyUtil.sneakyNotNull();
        public final int minCount = SneakyUtil.sneakyInt();
        public final int maxCount = SneakyUtil.sneakyInt();

        @Override
        public Weight getWeight() {
            throw new RuntimeException();
        }
    }

    public record MobSpawnCost(double energyBudget, double charge) {
    }
}
