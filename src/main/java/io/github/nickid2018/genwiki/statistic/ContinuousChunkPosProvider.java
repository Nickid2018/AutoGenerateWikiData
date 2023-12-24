package io.github.nickid2018.genwiki.statistic;

import lombok.SneakyThrows;

public class ContinuousChunkPosProvider extends ChunkPosProvider {

    private final int startX;
    private final int startZ;
    private final int len;

    public ContinuousChunkPosProvider(int total) {
        super(total);
        len = (int) Math.sqrt(total);
        startX = -len / 2;
        startZ = -len / 2;
    }

    @Override
    @SneakyThrows
    protected void next0(ChunkPosConsumer consumer) {
        int x = startX + count % len;
        int z = startZ + count / len;
        consumer.accept(x, z);
    }
}
