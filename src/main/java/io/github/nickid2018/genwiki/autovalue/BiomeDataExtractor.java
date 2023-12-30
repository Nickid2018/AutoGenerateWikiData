package io.github.nickid2018.genwiki.autovalue;

import io.github.nickid2018.genwiki.inject.InjectedProcess;
import io.github.nickid2018.genwiki.inject.SourceClass;
import lombok.SneakyThrows;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class BiomeDataExtractor {

    public static final Class<?> BIOME_CLASS;
    public static final Class<?> MOB_SPAWN_SETTINGS_CLASS;
    public static final Class<?> MOB_SPAWN_COST_CLASS;
    public static final Class<?> WEIGHTED_RANDOM_LIST_CLASS;
    public static final Class<?> SPAWNER_DATA_CLASS;

    public static final MethodHandle BIOME_GET_SKY_COLOR;
    public static final MethodHandle BIOME_GET_MOB_SETTINGS;
    public static final MethodHandle BIOME_HAS_PRECIPITATION;
    public static final MethodHandle BIOME_GET_FOG_COLOR;
    public static final MethodHandle BIOME_GET_FOLIAGE_COLOR;
    public static final MethodHandle BIOME_GET_BASE_TEMPERATURE;
    public static final MethodHandle BIOME_GET_WATER_COLOR;
    public static final MethodHandle BIOME_GET_WATER_FOG_COLOR;
    public static final MethodHandle MOB_SPAWN_SETTINGS_GET_CREATURE_PROBABILITY;
    public static final MethodHandle MOB_SPAWN_SETTINGS_GET_MOBS;
    public static final MethodHandle WEIGHTED_RANDOM_LIST_UNWRAP;
    public static final MethodHandle WEIGHTED_ENTRY_GET_WEIGHT;
    public static final MethodHandle WEIGHT_AS_INT;

    public static final VarHandle MOB_SPAWN_SETTINGS_MOB_SPAWN_COSTS;
    public static final VarHandle MOB_SPAWN_COST_ENERGY_BUDGET;
    public static final VarHandle MOB_SPAWN_COST_CHARGE;
    public static final VarHandle SPAWNER_DATA_TYPE;
    public static final VarHandle SPAWNER_DATA_MIN_COUNT;
    public static final VarHandle SPAWNER_DATA_MAX_COUNT;

    public static final Map<String, Object> MOB_CATEGORIES_SPAWN = new TreeMap<>();

    static {
        try {
            BIOME_CLASS = Class.forName("net.minecraft.world.level.biome.Biome");
            MOB_SPAWN_SETTINGS_CLASS = Class.forName("net.minecraft.world.level.biome.MobSpawnSettings");
            MOB_SPAWN_COST_CLASS = Class.forName("net.minecraft.world.level.biome.MobSpawnSettings$MobSpawnCost");
            WEIGHTED_RANDOM_LIST_CLASS = Class.forName("net.minecraft.util.random.WeightedRandomList");
            SPAWNER_DATA_CLASS = Class.forName("net.minecraft.world.level.biome.MobSpawnSettings$SpawnerData");
            Class<?> WEIGHTED_ENTRY_CLASS = Class.forName("net.minecraft.util.random.WeightedEntry");
            Class<?> WEIGHT_CLASS = Class.forName("net.minecraft.util.random.Weight");

            MethodHandles.Lookup lookup = MethodHandles.lookup();
            BIOME_GET_SKY_COLOR = lookup.unreflect(BIOME_CLASS.getMethod("getSkyColor"));
            BIOME_GET_MOB_SETTINGS = lookup.unreflect(BIOME_CLASS.getMethod("getMobSettings"));
            BIOME_HAS_PRECIPITATION = lookup.unreflect(BIOME_CLASS.getMethod("hasPrecipitation"));
            BIOME_GET_FOG_COLOR = lookup.unreflect(BIOME_CLASS.getMethod("getFogColor"));
            BIOME_GET_FOLIAGE_COLOR = lookup.unreflect(BIOME_CLASS.getMethod("getFoliageColor"));
            BIOME_GET_BASE_TEMPERATURE = lookup.unreflect(BIOME_CLASS.getMethod("getBaseTemperature"));
            BIOME_GET_WATER_COLOR = lookup.unreflect(BIOME_CLASS.getMethod("getWaterColor"));
            BIOME_GET_WATER_FOG_COLOR = lookup.unreflect(BIOME_CLASS.getMethod("getWaterFogColor"));
            MOB_SPAWN_SETTINGS_GET_CREATURE_PROBABILITY = lookup.unreflect(MOB_SPAWN_SETTINGS_CLASS.getMethod("getCreatureProbability"));
            MOB_SPAWN_SETTINGS_GET_MOBS = lookup.unreflect(MOB_SPAWN_SETTINGS_CLASS.getMethod("getMobs", EntityDataExtractor.MOB_CATEGORY_CLASS));
            WEIGHTED_RANDOM_LIST_UNWRAP = lookup.unreflect(WEIGHTED_RANDOM_LIST_CLASS.getMethod("unwrap"));
            WEIGHTED_ENTRY_GET_WEIGHT = lookup.unreflect(WEIGHTED_ENTRY_CLASS.getMethod("getWeight"));
            WEIGHT_AS_INT = lookup.unreflect(WEIGHT_CLASS.getMethod("asInt"));

            MethodHandles.Lookup privateLookup1 = MethodHandles.privateLookupIn(MOB_SPAWN_SETTINGS_CLASS, lookup);
            MOB_SPAWN_SETTINGS_MOB_SPAWN_COSTS = privateLookup1.findVarHandle(MOB_SPAWN_SETTINGS_CLASS, "mobSpawnCosts", Map.class);
            MethodHandles.Lookup privateLookup2 = MethodHandles.privateLookupIn(MOB_SPAWN_COST_CLASS, lookup);
            MOB_SPAWN_COST_ENERGY_BUDGET = privateLookup2.findVarHandle(MOB_SPAWN_COST_CLASS, "energyBudget", double.class);
            MOB_SPAWN_COST_CHARGE = privateLookup2.findVarHandle(MOB_SPAWN_COST_CLASS, "charge", double.class);
            SPAWNER_DATA_TYPE = lookup.findVarHandle(SPAWNER_DATA_CLASS, "type", EntityDataExtractor.ENTITY_TYPE_CLASS);
            SPAWNER_DATA_MIN_COUNT = lookup.findVarHandle(SPAWNER_DATA_CLASS, "minCount", int.class);
            SPAWNER_DATA_MAX_COUNT = lookup.findVarHandle(SPAWNER_DATA_CLASS, "maxCount", int.class);

            for (Object obj : EntityDataExtractor.MOB_CATEGORY_CLASS.getEnumConstants()) {
                String name = (String) InjectedProcess.ENUM_NAME.invoke(obj);
                if (name.equals("MISC"))
                    continue;
                MOB_CATEGORIES_SPAWN.put(name, obj);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

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
    public static void extractBiomeData(Object serverObj) {
        Iterable<?> levels = (Iterable<?>) InjectedProcess.GET_ALL_LEVELS.invoke(serverObj);
        @SourceClass("DefaultedRegistry<EntityType<?>>")
        Object entityRegistry = InjectedProcess.getRegistry("ENTITY_TYPE");

        for (Object level : levels) {
            @SourceClass("Registry<Biome>")
            Object biomeRegistry = InjectedProcess.getSyncRegistry(level, "BIOME");
            @SourceClass("Set<ResourceKey<Biome>>")
            Set<?> biomeKeySet = InjectedProcess.getRegistryKeySet(biomeRegistry);
            for (@SourceClass("ResourceKey<Biome>") Object biomeKey : biomeKeySet) {
                Object location = InjectedProcess.RESOURCE_KEY_LOCATION.invoke(biomeKey);
                String biomeID = InjectedProcess.getResourceLocationPath(location);
                Object biome = InjectedProcess.REGISTRY_GET.invoke(biomeRegistry, biomeKey);
                SKY_COLOR.put(biomeID, (int) BIOME_GET_SKY_COLOR.invoke(biome));
                HAS_PRECIPITATION.put(biomeID, (boolean) BIOME_HAS_PRECIPITATION.invoke(biome));
                FOG_COLOR.put(biomeID, (int) BIOME_GET_FOG_COLOR.invoke(biome));
                FOLIAGE_COLOR.put(biomeID, (int) BIOME_GET_FOLIAGE_COLOR.invoke(biome));
                BASE_TEMPERATURE.put(biomeID, (float) BIOME_GET_BASE_TEMPERATURE.invoke(biome));
                WATER_COLOR.put(biomeID, (int) BIOME_GET_WATER_COLOR.invoke(biome));
                WATER_FOG_COLOR.put(biomeID, (int) BIOME_GET_WATER_FOG_COLOR.invoke(biome));

                Object mobSettings = BIOME_GET_MOB_SETTINGS.invoke(biome);
                CREATURE_PROBABILITY.put(biomeID, (float) MOB_SPAWN_SETTINGS_GET_CREATURE_PROBABILITY.invoke(mobSettings));

                for (Map.Entry<String, Object> category : MOB_CATEGORIES_SPAWN.entrySet()) {
                    String categoryName = category.getKey();
                    Object categoryObj = category.getValue();
                    @SourceClass("WeightedRandomList<SpawnerData>")
                    Object spawnerList = MOB_SPAWN_SETTINGS_GET_MOBS.invoke(mobSettings, categoryObj);
                    @SourceClass("List<SpawnerData>")
                    List<?> spawnerDataList = (List<?>) WEIGHTED_RANDOM_LIST_UNWRAP.invoke(spawnerList);
                    if (spawnerDataList.isEmpty())
                        continue;
                    for (Object spawnerData : spawnerDataList) {
                        Object type = SPAWNER_DATA_TYPE.get(spawnerData);
                        String entityID = InjectedProcess.getObjectPathWithRegistry(entityRegistry, type);
                        int weight = (int) WEIGHT_AS_INT.invoke(WEIGHTED_ENTRY_GET_WEIGHT.invoke(spawnerData));
                        int min = (int) SPAWNER_DATA_MIN_COUNT.get(spawnerData);
                        int max = (int) SPAWNER_DATA_MAX_COUNT.get(spawnerData);
                        SPAWN_DATA.add(biomeID, categoryName, entityID, weight, min, max);
                    }
                }
                @SourceClass("Map<EntityType<?>, MobSpawnCost>")
                Map<?, ?> mobSpawnCosts = (Map<?, ?>) MOB_SPAWN_SETTINGS_MOB_SPAWN_COSTS.get(mobSettings);
                for (Map.Entry<?, ?> entry : mobSpawnCosts.entrySet()) {
                    Object type = entry.getKey();
                    String entityID = InjectedProcess.getObjectPathWithRegistry(entityRegistry, type);
                    @SourceClass("MobSpawnCost")
                    Object cost = entry.getValue();
                    double budget = (double) MOB_SPAWN_COST_ENERGY_BUDGET.get(cost);
                    double charge = (double) MOB_SPAWN_COST_CHARGE.get(cost);
                    SPAWN_DATA.addSpawnCost(biomeID, entityID, budget, charge);
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
