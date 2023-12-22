package io.github.nickid2018.genwiki.statistic;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class BlockCounter {

    private final Object2IntMap<Object> blockCounter = new Object2IntOpenHashMap<>();

    public void increase(Object block) {
        blockCounter.put(block, blockCounter.getOrDefault(block, 0) + 1);
    }

    public void increase(Object block, int count) {
        blockCounter.put(block, blockCounter.getOrDefault(block, 0) + count);
    }

    public static BlockCounter merge(Iterable<BlockCounter> counters) {
        BlockCounter counter = new BlockCounter();
        for (BlockCounter c : counters)
            for (Object2IntMap.Entry<Object> entry : c.blockCounter.object2IntEntrySet())
                counter.increase(entry.getKey(), entry.getIntValue());
        return counter;
    }
}
