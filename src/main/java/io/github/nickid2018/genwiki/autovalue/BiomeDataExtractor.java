package io.github.nickid2018.genwiki.autovalue;

import io.github.nickid2018.genwiki.autovalue.wikidata.*;
import lombok.SneakyThrows;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;

import java.util.List;
import java.util.Map;

public class BiomeDataExtractor {
    private static final ColorWikiData SKY_COLOR = new ColorWikiData();
    private static final BooleanWikiData HAS_PRECIPITATION = new BooleanWikiData();
    private static final ColorWikiData FOG_COLOR = new ColorWikiData();
    private static final ColorWikiData FOLIAGE_COLOR = new ColorWikiData();
    private static final NumberWikiData BASE_TEMPERATURE = new NumberWikiData();
    private static final ColorWikiData WATER_COLOR = new ColorWikiData();
    private static final ColorWikiData WATER_FOG_COLOR = new ColorWikiData();
    private static final NumberWikiData CREATURE_PROBABILITY = new NumberWikiData().setFallback(0.1f);
    private static final SpawnWikiData SPAWN_DATA = new SpawnWikiData();

    @SneakyThrows
    public static void extractBiomeData(MinecraftServer serverObj) {
        Iterable<ServerLevel> levels = serverObj.getAllLevels();
        DefaultedRegistry<EntityType<?>> entityRegistry = BuiltInRegistries.ENTITY_TYPE;

        for (ServerLevel level : levels) {
            Registry<Biome> biomeRegistry = level.registryAccess().lookupOrThrow(Registries.BIOME);
            for (ResourceKey<Biome> biomeKey : biomeRegistry.registryKeySet()) {
                String biomeID = biomeKey.location().getPath();
                Biome biome = biomeRegistry.getValue(biomeKey);
                SKY_COLOR.put(biomeID, biome.getSkyColor());
                HAS_PRECIPITATION.put(biomeID, biome.hasPrecipitation());
                FOG_COLOR.put(biomeID, biome.getFogColor());
                FOLIAGE_COLOR.put(biomeID, biome.getFoliageColor());
                BASE_TEMPERATURE.put(biomeID, biome.getBaseTemperature());
                WATER_COLOR.put(biomeID, biome.getWaterColor());
                WATER_FOG_COLOR.put(biomeID, biome.getWaterFogColor());

                MobSpawnSettings mobSettings = biome.getMobSettings();
                CREATURE_PROBABILITY.put(biomeID, mobSettings.getCreatureProbability());

                for (MobCategory category : MobCategory.class.getEnumConstants()) {
                    String categoryName = category.name();
                    if (categoryName.equals("MISC"))
                        continue;
                    WeightedList<MobSpawnSettings.SpawnerData> spawnerList = mobSettings.getMobs(category);
                    List<Weighted<MobSpawnSettings.SpawnerData>> spawnerDataList = spawnerList.unwrap();
                    if (spawnerDataList.isEmpty())
                        continue;
                    for (Weighted<MobSpawnSettings.SpawnerData> spawnerData : spawnerDataList) {
                        SPAWN_DATA.add(
                            biomeID,
                            categoryName,
                            entityRegistry.getKey(spawnerData.value().type()).getPath(),
                            spawnerData.weight(),
                            spawnerData.value().minCount(),
                            spawnerData.value().maxCount()
                        );
                    }
                }

                for (Map.Entry<EntityType<?>, MobSpawnSettings.MobSpawnCost> entry : mobSettings.mobSpawnCosts.entrySet()) {
                    SPAWN_DATA.addSpawnCost(
                        biomeID,
                        entityRegistry.getKey(entry.getKey()).getPath(),
                        entry.getValue().energyBudget(),
                        entry.getValue().charge()
                    );
                }
            }
        }

        WikiData.write(SKY_COLOR, "biome_sky_color.txt");
        WikiData.write(HAS_PRECIPITATION, "biome_has_precipitation.txt");
        WikiData.write(FOG_COLOR, "biome_fog_color.txt");
        WikiData.write(FOLIAGE_COLOR, "biome_foliage_color.txt");
        WikiData.write(BASE_TEMPERATURE, "biome_base_temperature.txt");
        WikiData.write(WATER_COLOR, "biome_water_color.txt");
        WikiData.write(WATER_FOG_COLOR, "biome_water_fog_color.txt");
        WikiData.write(CREATURE_PROBABILITY, "biome_creature_probability.txt");
        WikiData.write(SPAWN_DATA, "biome_spawn_data.txt");
    }
}
