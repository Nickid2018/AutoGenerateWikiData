package io.github.nickid2018.genwiki.statistic;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.nickid2018.genwiki.inject.InjectedProcess;
import io.github.nickid2018.genwiki.inject.InjectionConstant;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
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
        JsonObject object = new JsonObject();
        object.addProperty("minHeight", minHeight);
        object.addProperty("maxHeight", maxHeight);
        Object blockRegistry = InjectedProcess.getRegistry("BLOCK");
        for (Object2ObjectMap.Entry<Object, Int2LongMap> entry : blockCounter.object2ObjectEntrySet()) {
            Object block = entry.getKey();
            Object location = InjectedProcess.REGISTRY_GET_KEY.invoke(blockRegistry, block);
            String name = InjectedProcess.getResourceLocationPath(location);
            JsonArray array = new JsonArray();
            object.add(name, array);
            for (int i = minHeight; i <= maxHeight; i++) {
                long count = entry.getValue().getOrDefault(i, 0L);
                array.add(count);
            }
        }

        try (Writer writer = new FileWriter(outputFile)) {
            writer.write(object.toString());
            writer.flush();
        }
    }
}
