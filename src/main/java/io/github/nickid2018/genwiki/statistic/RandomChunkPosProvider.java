package io.github.nickid2018.genwiki.statistic;

import lombok.SneakyThrows;

import java.util.Random;

public class RandomChunkPosProvider extends ChunkPosProvider {

    private final Random random = new Random();

    public RandomChunkPosProvider(int total, int blockSize) {
        super(total, blockSize);
    }

    @Override
    @SneakyThrows
    protected void next0(ChunkPosConsumer consumer) {
        consumer.accept(random.nextInt(1000000) - 500000, random.nextInt(1000000) - 500000);
    }
}
