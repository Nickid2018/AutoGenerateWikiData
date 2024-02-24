package io.github.nickid2018.genwiki.statistic;

import it.unimi.dsi.fastutil.Function;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
public class DataCounter {

    private final Object2ObjectMap<Object, Int2LongMap> counter = new Object2ObjectOpenHashMap<>();

    private final String name;
    private final Function<Object, String> objectToString;

    public DataCounter(String name, Function<Object, String> objectToString) {
        this.name = name;
        this.objectToString = objectToString;
    }

    public void increase(Object block, int y) {
        Int2LongMap pair = counter.computeIfAbsent(block, k -> new Int2LongOpenHashMap());
        pair.put(y, pair.getOrDefault(y, 0L) + 1);
    }

    public void increase(Object block, int y, long count) {
        Int2LongMap pair = counter.computeIfAbsent(block, k -> new Int2LongOpenHashMap());
        pair.put(y, pair.getOrDefault(y, 0L) + count);
    }

    @SneakyThrows
    public void write(long worldSeed, String levelName, ChunkPosProvider posProvider, int minHeight, int maxHeight, int chunkCount) {
        File outputFile = new File(levelName + "_" + name + "_count.json");

        StringBuilder builder = new StringBuilder();
        builder.append("{\n");

        builder.append("\t\"worldSeed\": ").append(worldSeed).append(",\n");
        builder.append("\t\"minHeight\": ").append(minHeight).append(",\n");
        builder.append("\t\"maxHeight\": ").append(maxHeight).append(",\n");
        builder.append("\t\"chunkCount\": ").append(chunkCount).append(",\n");
        builder.append("\t\"posProvider\": \"").append(posProvider).append("\",\n");

        Map<String, String> lines = new TreeMap<>();
        for (Object2ObjectMap.Entry<Object, Int2LongMap> entry : counter.object2ObjectEntrySet()) {
            Object item = entry.getKey();
            String name = objectToString.apply(item);
            LongList array = new LongArrayList();
            for (int i = minHeight; i < maxHeight; i++) {
                long count = entry.getValue().getOrDefault(i, 0L);
                array.add(count);
            }
            String data = String.join(", ", array.longStream().mapToObj(String::valueOf).toList());
            lines.put(name, data);
        }

        String dataLines = String.join(
                ",\n",
                lines.entrySet().stream()
                        .map(en -> "\t\t\"" + en.getKey() + "\": [" + en.getValue() + "]")
                        .toList()
        );

        builder.append("\t\"").append(name).append("\": {\n").append(dataLines).append("\n\t}");
        builder.append("\n}");

        try (Writer writer = new FileWriter(outputFile)) {
            writer.write(builder.toString());
            writer.flush();
        } catch (IOException e) {
            log.error("Failed to write data to file {}!", outputFile);
            log.error("Exception: ", e);
        }
    }
}
