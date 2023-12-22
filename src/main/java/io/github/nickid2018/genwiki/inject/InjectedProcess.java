package io.github.nickid2018.genwiki.inject;

import io.github.nickid2018.genwiki.autovalue.*;
import io.github.nickid2018.genwiki.statistic.ChunkStatisticsAnalyzer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class InjectedProcess {

    private static final Map<String, Object> REGISTRY = new HashMap<>();
    public static Object featureFlagSet;

    public static final Class<?> TAG_KEY_CLASS;
    public static final Class<?> RESOURCE_KEY_CLASS;
    public static final Class<?> MINECRAFT_SERVER_CLASS;

    public static final MethodHandle ENUM_ORDINAL;
    public static final MethodHandle ENUM_NAME;

    @SourceClass("Set<ResourceKey<T>>")
    public static final MethodHandle REGISTRY_KEY_SET;
    @SourceClass("T")
    public static final MethodHandle REGISTRY_GET;
    @SourceClass("ResourceLocation")
    public static final MethodHandle RESOURCE_KEY_LOCATION;
    @SourceClass("String")
    public static final MethodHandle RESOURCE_LOCATION_PATH;

    public static final MethodHandle SERVER_OVERWORLD;
    public static final MethodHandle REGISTRY_ACCESS;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            TAG_KEY_CLASS = Class.forName("net.minecraft.tags.TagKey");
            ENUM_ORDINAL = lookup.unreflect(Enum.class.getMethod("ordinal"));
            ENUM_NAME = lookup.unreflect(Enum.class.getMethod("name"));
            Class<?> registryClass = Class.forName("net.minecraft.core.Registry");
            RESOURCE_KEY_CLASS = Class.forName("net.minecraft.resources.ResourceKey");
            Class<?> resourceLocationClass = Class.forName("net.minecraft.resources.ResourceLocation");
            REGISTRY_KEY_SET = lookup.unreflect(registryClass.getMethod("registryKeySet"));
            REGISTRY_GET = lookup.unreflect(registryClass.getMethod("get", RESOURCE_KEY_CLASS));
            RESOURCE_KEY_LOCATION = lookup.unreflect(RESOURCE_KEY_CLASS.getMethod("location"));
            RESOURCE_LOCATION_PATH = lookup.unreflect(resourceLocationClass.getMethod("getPath"));

            MINECRAFT_SERVER_CLASS = Class.forName("net.minecraft.server.MinecraftServer");
            SERVER_OVERWORLD = lookup.unreflect(MINECRAFT_SERVER_CLASS.getMethod("overworld"));
            Class<?> registryAccessClass = Class.forName("net.minecraft.world.level.Level");
            REGISTRY_ACCESS = lookup.unreflect(registryAccessClass.getMethod("registryAccess"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    @SourceClass("Set<ResourceKey<T>>")
    public static Set<?> getRegistryKeySet(Object registry) {
        return (Set<?>) REGISTRY_KEY_SET.invoke(registry);
    }

    @SneakyThrows
    public static String getResourceLocationPath(Object resourceKey) {
        return (String) RESOURCE_LOCATION_PATH.invoke(resourceKey);
    }

    public static Object getRegistry(String name) {
        return REGISTRY.get(name);
    }

    @SuppressWarnings("unused")
    @SneakyThrows
    public static String preprocessDataPacks() {
        log.info("Writing data packs...");
        Class<?> registryClass = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
        Field[] fields = registryClass.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Object obj = field.get(null);
            if (obj != null) {
                log.info("Get registry: {}", field.getName());
                REGISTRY.put(field.getName(), obj);
            }
        }

        Object featureFlagRegistry = Class.forName("net.minecraft.world.flag.FeatureFlags").getField("REGISTRY").get(null);
        Class<?> featureFlagRegistryClass = Class.forName("net.minecraft.world.flag.FeatureFlagRegistry");
        Field names = featureFlagRegistryClass.getDeclaredField("names");
        Method fromNames = featureFlagRegistryClass.getDeclaredMethod("fromNames", Iterable.class);
        names.setAccessible(true);
        Map<?, ?> namesObj = (Map<?, ?>) names.get(featureFlagRegistry);
        Set<?> namesIterable = namesObj.keySet();

        String ret =  namesObj.keySet().stream().map(RESOURCE_LOCATION_PATH::invoke).map(Object::toString).collect(Collectors.joining(","));
        featureFlagSet = fromNames.invoke(featureFlagRegistry, namesIterable);

        return ret;
    }

    @SneakyThrows
    @SuppressWarnings("unused")
    public static void extractDataInjection(Object server) {
        log.info("Trapped server instance: {}", server);

        if (InjectionConstant.OUTPUT_FOLDER.isDirectory())
            FileUtils.deleteDirectory(InjectionConstant.OUTPUT_FOLDER);
        InjectionConstant.OUTPUT_FOLDER.mkdirs();

        BlockDataExtractor.extractBlockData();
        ItemDataExtractor.extractItemData(server);
        EntityDataExtractor.extractEntityData(server);
        EnchantmentDataExtractor.extractEnchantmentData();

        throw new RuntimeException("Program exited, wiki data has been written.");
    }

    @SneakyThrows
    @SuppressWarnings("unused")
    public static void chunkStatisticsInjection(Object server) {
        if (InjectionConstant.OUTPUT_FOLDER.isDirectory())
            FileUtils.deleteDirectory(InjectionConstant.OUTPUT_FOLDER);
        InjectionConstant.OUTPUT_FOLDER.mkdirs();

        ChunkStatisticsAnalyzer.analyze(server);
    }
}
