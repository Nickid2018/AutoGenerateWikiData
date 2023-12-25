package io.github.nickid2018.genwiki.statistic;

import io.github.nickid2018.genwiki.inject.InjectedProcess;
import it.unimi.dsi.fastutil.Function;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.SneakyThrows;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

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
    public void write(String levelName, int minHeight, int maxHeight) {
        File outputFile = new File(levelName + "_" + name + "_count.json");

        StringBuilder builder = new StringBuilder();
        builder.append("{\n").append("\t\"minHeight\": ").append(minHeight).append(",\n")
                .append("\t\"maxHeight\": ").append(maxHeight).append(",\n");
        builder.append("\t\"").append(name).append("\": {\n");

        for (Object2ObjectMap.Entry<Object, Int2LongMap> entry : counter.object2ObjectEntrySet()) {
            Object item = entry.getKey();
            String name = objectToString.apply(item);
            LongList array = new LongArrayList();
            for (int i = minHeight; i < maxHeight; i++) {
                long count = entry.getValue().getOrDefault(i, 0L);
                array.add(count);
            }
            String data = String.join(", ", array.longStream().mapToObj(String::valueOf).toList());
            builder.append("\t\t\"").append(name).append("\": [").append(data).append("],\n");
        }

        builder.deleteCharAt(builder.lastIndexOf(","));
        builder.append("\t}\n").append("}");

        try (Writer writer = new FileWriter(outputFile)) {
            writer.write(builder.toString());
            writer.flush();
        }
    }
}
