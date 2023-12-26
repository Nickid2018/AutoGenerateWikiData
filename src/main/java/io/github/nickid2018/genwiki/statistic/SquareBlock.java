package io.github.nickid2018.genwiki.statistic;

import lombok.SneakyThrows;

record SquareBlock(int startX, int startZ, int h) {
    @SneakyThrows
    public void next(int position, ChunkPosProvider.ChunkPosConsumer consumer) {
        int x = position % h;
        int z = position / h;
        consumer.accept(startX + x, startZ + z);
    }
}
