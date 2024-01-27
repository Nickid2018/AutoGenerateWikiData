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
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class InjectedProcess {

    private static final Map<String, Object> REGISTRY = new HashMap<>();
    private static final Map<String, Object> REGISTRY_KEY = new HashMap<>();
    public static Object featureFlagSet;

    public static final Class<?> TAG_KEY_CLASS;
    public static final Class<?> RESOURCE_KEY_CLASS;
    public static final Class<?> MINECRAFT_SERVER_CLASS;
    public static final Class<?> HOLDER_CLASS;
    public static final Class<?> EITHER_CLASS;
    public static final Class<?> BLOCK_POS_CLASS;
    public static final Class<?> DIRECTION_CLASS;

    public static final MethodHandle ENUM_ORDINAL;
    public static final MethodHandle ENUM_NAME;

    @SourceClass("Set<ResourceKey<T>>")
    public static final MethodHandle REGISTRY_KEY_SET;
    @SourceClass("T")
    public static final MethodHandle REGISTRY_GET;
    public static final MethodHandle REGISTRY_GET_KEY;
    @SourceClass("ResourceLocation")
    public static final MethodHandle RESOURCE_KEY_LOCATION;
    @SourceClass("String")
    public static final MethodHandle RESOURCE_LOCATION_PATH;
    @SourceClass("Registry<T>")
    public static final MethodHandle REGISTRY_OR_THROW;
    @SourceClass("Iterable<ServerLevel>")
    public static final MethodHandle GET_ALL_LEVELS;

    public static final MethodHandle SERVER_OVERWORLD;
    public static final MethodHandle REGISTRY_ACCESS;
    public static final MethodHandle HOLDER_VALUE;
    public static final MethodHandle HOLDER_UNWRAP_KEY;
    public static final MethodHandle EITHER_LEFT;

    public static final Path NULL_PATH;
    public static final Object BLOCK_POS_ZERO;
    public static final Map<String, Object> DIRECTION_MAP = new HashMap<>();

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
            REGISTRY_GET_KEY = lookup.unreflect(registryClass.getMethod("getKey", Object.class));
            RESOURCE_KEY_LOCATION = lookup.unreflect(RESOURCE_KEY_CLASS.getMethod("location"));
            RESOURCE_LOCATION_PATH = lookup.unreflect(resourceLocationClass.getMethod("getPath"));
            Class<?> registryAccessClass = Class.forName("net.minecraft.core.RegistryAccess");
            REGISTRY_OR_THROW = lookup.unreflect(registryAccessClass.getMethod("registryOrThrow", RESOURCE_KEY_CLASS));

            MINECRAFT_SERVER_CLASS = Class.forName("net.minecraft.server.MinecraftServer");
            SERVER_OVERWORLD = lookup.unreflect(MINECRAFT_SERVER_CLASS.getMethod("overworld"));
            GET_ALL_LEVELS = lookup.unreflect(MINECRAFT_SERVER_CLASS.getMethod("getAllLevels"));
            Class<?> levelClass = Class.forName("net.minecraft.world.level.Level");
            REGISTRY_ACCESS = lookup.unreflect(levelClass.getMethod("registryAccess"));

            HOLDER_CLASS = Class.forName("net.minecraft.core.Holder");
            HOLDER_VALUE = lookup.unreflect(HOLDER_CLASS.getMethod("value"));
            HOLDER_UNWRAP_KEY = lookup.unreflect(HOLDER_CLASS.getMethod("unwrapKey"));

            EITHER_CLASS = Class.forName("com.mojang.datafixers.util.Either");
            EITHER_LEFT = lookup.unreflect(EITHER_CLASS.getMethod("left"));

            BLOCK_POS_CLASS = Class.forName("net.minecraft.core.BlockPos");
            BLOCK_POS_ZERO = BLOCK_POS_CLASS.getField("ZERO").get(null);

            DIRECTION_CLASS = Class.forName("net.minecraft.core.Direction");
            for (Object obj : DIRECTION_CLASS.getEnumConstants()) {
                String name = (String) ENUM_NAME.invoke(obj);
                DIRECTION_MAP.put(name.toLowerCase(), obj);
            }

            if (System.getProperty("os.name").toLowerCase().contains("win"))
                NULL_PATH = Path.of("nul");
            else
                NULL_PATH = Path.of("/dev/null");

            Class<?> builtinRegistry = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
            Field[] fields = builtinRegistry.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object obj = field.get(null);
                if (obj != null) {
                    log.info("Get registry: {}", field.getName());
                    REGISTRY.put(field.getName(), obj);
                }
            }

            Class<?> syncRegistry = Class.forName("net.minecraft.core.registries.Registries");
            Field[] syncFields = syncRegistry.getDeclaredFields();
            for (Field field : syncFields) {
                field.setAccessible(true);
                Object obj = field.get(null);
                if (RESOURCE_KEY_CLASS.isInstance(obj)) {
                    log.info("Get registry key: {}", field.getName());
                    REGISTRY_KEY.put(field.getName(), obj);
                }
            }
        } catch (Throwable e) {
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

    @SneakyThrows
    public static String getObjectPathWithRegistry(Object registry, Object obj) {
        return getResourceLocationPath(REGISTRY_GET_KEY.invoke(registry, obj));
    }

    public static Object getRegistry(String name) {
        return REGISTRY.get(name);
    }

    @SneakyThrows
    public static Object getSyncRegistry(Object level, String name) {
        return REGISTRY_OR_THROW.invoke(REGISTRY_ACCESS.invoke(level), REGISTRY_KEY.get(name));
    }

    @SneakyThrows
    public static String holderToString(Object holder) {
        return getResourceLocationPath(RESOURCE_KEY_LOCATION.invoke(((Optional<?>) HOLDER_UNWRAP_KEY.invoke(holder)).get()));
    }

    @SuppressWarnings("unused")
    @SneakyThrows
    public static String preprocessDataPacks() {
        log.info("Writing data packs...");

        Object featureFlagRegistry = Class.forName("net.minecraft.world.flag.FeatureFlags").getField("REGISTRY").get(null);
        Class<?> featureFlagRegistryClass = Class.forName("net.minecraft.world.flag.FeatureFlagRegistry");
        Field names = featureFlagRegistryClass.getDeclaredField("names");
        Method fromNames = featureFlagRegistryClass.getDeclaredMethod("fromNames", Iterable.class);
        names.setAccessible(true);
        Map<?, ?> namesObj = (Map<?, ?>) names.get(featureFlagRegistry);
        Set<?> namesIterable = namesObj.keySet();

        String ret = namesObj.keySet().stream().map(RESOURCE_LOCATION_PATH::invoke).map(Object::toString).collect(Collectors.joining(","));
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

        BlockDataExtractor.extractBlockData(server);
        ItemDataExtractor.extractItemData(server);
        EntityDataExtractor.extractEntityData();
        BiomeDataExtractor.extractBiomeData(server);
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
