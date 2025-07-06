package io.github.nickid2018.genwiki;

import io.github.nickid2018.genwiki.autovalue.*;
import io.github.nickid2018.genwiki.statistic.ChunkStatisticsAnalyzer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.GameRules;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.stream.Collectors;

@Slf4j
public class InjectionEntrypoint {

    public static final File OUTPUT_FOLDER = new File("../output");
    public static FeatureFlagSet featureFlagSet;

    @SuppressWarnings("unused")
    public static final Path NULL_PATH = Path.of(
        System.getProperty("os.name").toLowerCase().contains("win") ? "nul" : "/dev/null"
    );

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

        if (OUTPUT_FOLDER.isDirectory())
            FileUtils.deleteDirectory(OUTPUT_FOLDER);
        OUTPUT_FOLDER.mkdirs();

        RegistriesExporter.exportRegistries();
        BlockDataExtractor.extractBlockData(server);
        ItemDataExtractor.extractItemData(server);
        EntityDataExtractor.extractEntityData(server);
        BiomeDataExtractor.extractBiomeData(server);
        EnchantmentDataExtractor.extractEnchantmentData(server);
        GameRuleDataExtractor.extractGameRuleData(server);

        throw new RuntimeException("Program exited, wiki data has been written.");
    }

    @SneakyThrows
    @SuppressWarnings("unused")
    public static void chunkStatisticsInjection(MinecraftServer server) {
        ChunkStatisticsAnalyzer.analyze(server);
    }
}
