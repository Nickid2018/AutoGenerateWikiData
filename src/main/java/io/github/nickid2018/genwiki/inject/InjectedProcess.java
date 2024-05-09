package io.github.nickid2018.genwiki.inject;

import io.github.nickid2018.genwiki.autovalue.*;
import io.github.nickid2018.genwiki.statistic.ChunkStatisticsAnalyzer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import org.apache.commons.io.FileUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class InjectedProcess {

    public static FeatureFlagSet featureFlagSet;

    public static final Class<?> TAG_KEY_CLASS;
    public static final Class<?> BLOCK_POS_CLASS;
    public static final Class<?> DIRECTION_CLASS;

    public static final MethodHandle ENUM_ORDINAL;
    public static final MethodHandle ENUM_NAME;

    public static final Path NULL_PATH;
    public static final Map<String, Object> DIRECTION_MAP = new HashMap<>();

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            TAG_KEY_CLASS = Class.forName("net.minecraft.tags.TagKey");
            ENUM_ORDINAL = lookup.unreflect(Enum.class.getMethod("ordinal"));
            ENUM_NAME = lookup.unreflect(Enum.class.getMethod("name"));

            BLOCK_POS_CLASS = Class.forName("net.minecraft.core.BlockPos");

            DIRECTION_CLASS = Class.forName("net.minecraft.core.Direction");
            for (Object obj : DIRECTION_CLASS.getEnumConstants()) {
                String name = (String) ENUM_NAME.invoke(obj);
                DIRECTION_MAP.put(name.toLowerCase(), obj);
            }

            if (System.getProperty("os.name").toLowerCase().contains("win"))
                NULL_PATH = Path.of("nul");
            else
                NULL_PATH = Path.of("/dev/null");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unused")
    @SneakyThrows
    public static String preprocessDataPacks() {
        log.info("Writing data packs...");
        featureFlagSet = FeatureFlags.REGISTRY.fromNames(FeatureFlags.REGISTRY.names.keySet());
        return FeatureFlags.REGISTRY.names
            .keySet().stream()
            .map(ResourceLocation::getPath)
            .map(Object::toString)
            .collect(Collectors.joining(","));
    }

    @SneakyThrows
    @SuppressWarnings("unused")
    public static void extractDataInjection(MinecraftServer server) {
        log.info("Trapped server instance: {}", server);

        if (InjectionConstant.OUTPUT_FOLDER.isDirectory())
            FileUtils.deleteDirectory(InjectionConstant.OUTPUT_FOLDER);
        InjectionConstant.OUTPUT_FOLDER.mkdirs();

        BlockDataExtractor.extractBlockData(server);
        ItemDataExtractor.extractItemData(server);
        EntityDataExtractor.extractEntityData(server);
        BiomeDataExtractor.extractBiomeData(server);
        EnchantmentDataExtractor.extractEnchantmentData(server);

        throw new RuntimeException("Program exited, wiki data has been written.");
    }

    @SneakyThrows
    @SuppressWarnings("unused")
    public static void chunkStatisticsInjection(MinecraftServer server) {
        if (InjectionConstant.OUTPUT_FOLDER.isDirectory())
            FileUtils.deleteDirectory(InjectionConstant.OUTPUT_FOLDER);
        InjectionConstant.OUTPUT_FOLDER.mkdirs();

        ChunkStatisticsAnalyzer.analyze(server);
    }
}
