package io.github.nickid2018.genwiki.statistic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class StatisticsSettings {

    private static StatisticsSettings instance;

    public static synchronized StatisticsSettings getInstance() {
        if (instance == null)
            instance = new StatisticsSettings();
        return instance;
    }

    private StatisticsSettings() {
        boolean useLocalFileSettings = Boolean.parseBoolean(System.getProperty("genwiki.statistics.fileSettings", "false"));
        if (useLocalFileSettings)
            loadWithFile(propGetOrDefault("genwiki.statistics.settingsFile", "statistics.properties"));
        else
            loadFromEnv();
        logToConsole();
    }

    @Getter
    private int batchSize;
    @Getter
    private int chunkTotal;
    @Getter
    private int saveInterval;
    @Getter
    private Set<String> dimensions;

    private final Map<String, ChunkPosProvider> providerMap = new HashMap<>();
    private Supplier<ChunkPosProvider> fallbackProvider = () -> new ContinuousChunkPosProvider(chunkTotal, 1089);

    @SneakyThrows
    private void loadWithFile(String file) {
        try (FileReader reader = new FileReader(file)) {
            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
            batchSize = jsonGetOrDefault(object, "batch_size", 4);
            chunkTotal = jsonGetOrDefault(object, "chunk_total", 25000);
            saveInterval = jsonGetOrDefault(object, "save_interval", 10000);
            int blockSize = jsonGetOrDefault(object, "block_size", 1089);
            fallbackProvider = () -> new ContinuousChunkPosProvider(chunkTotal, blockSize);
            if (object.has("providers_override")) {
                JsonObject providers = object.getAsJsonObject("providers_override");
                for (Map.Entry<String, JsonElement> entry : providers.entrySet()) {
                    String dimension = entry.getKey();
                    JsonObject providerData = entry.getValue().getAsJsonObject();
                    String provider = jsonGetOrDefault(providerData, "provider", "continuous").toLowerCase(Locale.ROOT);
                    int providerBlockSize = jsonGetOrDefault(providerData, "block_size", 1089);
                    if (provider.equals("continuous"))
                        providerMap.put(dimension, new ContinuousChunkPosProvider(chunkTotal, providerBlockSize));
                    else if (provider.equals("random")) {
                        if (providerData.has("seed"))
                            providerMap.put(dimension, new RandomChunkPosProvider(chunkTotal, providerBlockSize, providerData.get("seed").getAsLong()));
                        else
                            providerMap.put(dimension, new RandomChunkPosProvider(chunkTotal, providerBlockSize));
                    } else
                        throw new IllegalArgumentException("Unknown provider: " + provider);
                }
            }
            if (object.has("dimensions")) {
                dimensions = object.getAsJsonArray("dimensions").asList().stream().map(JsonElement::getAsString).collect(Collectors.toSet());
            }
        }
    }

    private void loadFromEnv() {
        batchSize = envGetOrDefault("BATCH_SIZE", 4);
        chunkTotal = envGetOrDefault("CHUNK_TOTAL", 25000);
        saveInterval = envGetOrDefault("SAVE_INTERVAL", 10000);
        String provider = envGetOrDefault("CHUNK_POS_PROVIDER_FACTORY", "continuous");
        int blockSize = envGetOrDefault("BLOCK_SIZE", 1089);
        fallbackProvider = switch (provider.toLowerCase(Locale.ROOT)) {
            case "continuous" -> () -> new ContinuousChunkPosProvider(chunkTotal, blockSize);
            case "random" -> () -> new RandomChunkPosProvider(chunkTotal, blockSize);
            default -> throw new IllegalArgumentException("Unknown provider: " + provider);
        };
        String dimensions = envGetOrDefault("DIMENSIONS", "");
        if (!dimensions.isEmpty())
            this.dimensions = Set.of(dimensions.split(","));
    }

    public ChunkPosProvider getChunkPosProvider(String dimension) {
        return providerMap.getOrDefault(dimension, fallbackProvider.get());
    }

    private void logToConsole() {
        log.info("----- Statistics Settings -----");
        log.info("Batch Size: {}", batchSize);
        log.info("Chunk Total: {}", chunkTotal);
        log.info("Save Interval: {}", saveInterval);
        log.info("Dimensions: {}", dimensions);
        log.info("Fallback Chunk Position Provider: {}", fallbackProvider.get());
        for (Map.Entry<String, ChunkPosProvider> entry : providerMap.entrySet())
            log.info("Override Chunk Position Provider for {}: {}", entry.getKey(), entry.getValue());
        log.info("-------------------------------");
    }

    private static String envGetOrDefault(String key, String def) {
        String value = System.getenv(key);
        return value == null ? def : value;
    }

    private static int envGetOrDefault(String key, int def) {
        String value = System.getenv(key);
        return value == null ? def : Integer.parseInt(value);
    }

    private static String propGetOrDefault(String key, String def) {
        String value = System.getProperty(key);
        return value == null ? def : value;
    }

    private static int jsonGetOrDefault(JsonObject object, String key, int def) {
        return object.has(key) ? object.get(key).getAsInt() : def;
    }

    private static String jsonGetOrDefault(JsonObject object, String key, String def) {
        return object.has(key) ? object.get(key).getAsString() : def;
    }
}
