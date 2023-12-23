package io.github.nickid2018.genwiki.statistic;

import com.google.gson.*;
import io.github.nickid2018.genwiki.inject.InjectedProcess;
import io.github.nickid2018.genwiki.inject.InjectionConstant;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.*;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Path;

public class BlockCounter {

    private final Object2ObjectMap<Object, Int2LongMap> blockCounter = new Object2ObjectOpenHashMap<>();

    public void increase(Object block, int y) {
        Int2LongMap pair = blockCounter.computeIfAbsent(block, k -> new Int2LongOpenHashMap());
        pair.put(y, pair.getOrDefault(y, 0L) + 1);
    }

    public void increase(Object block, int y, long count) {
        Int2LongMap pair = blockCounter.computeIfAbsent(block, k -> new Int2LongOpenHashMap());
        pair.put(y, pair.getOrDefault(y, 0L) + count);
    }

    @SneakyThrows
    public void write(String levelName, int minHeight, int maxHeight) {
        File outputFile = new File(levelName + "_blockCount.json");

        StringBuilder builder = new StringBuilder();
        builder.append("{\n").append("\t\"minHeight\": ").append(minHeight).append(",\n")
                .append("\t\"maxHeight\": ").append(maxHeight).append(",\n");
        builder.append("\t\"blocks\": {\n");

        Object blockRegistry = InjectedProcess.getRegistry("BLOCK");
        for (Object2ObjectMap.Entry<Object, Int2LongMap> entry : blockCounter.object2ObjectEntrySet()) {
            Object block = entry.getKey();
            Object location = InjectedProcess.REGISTRY_GET_KEY.invoke(blockRegistry, block);
            String name = InjectedProcess.getResourceLocationPath(location);
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
