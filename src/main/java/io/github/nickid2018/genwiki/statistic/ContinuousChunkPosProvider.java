package io.github.nickid2018.genwiki.statistic;

import it.unimi.dsi.fastutil.ints.IntIntPair;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ContinuousChunkPosProvider extends ChunkPosProvider {

    private final List<SquareBlock> blocksList = new ArrayList<>();

    public ContinuousChunkPosProvider(int total, int blockSize) {
        super(total, blockSize);

        blockSize = this.blockSize;
        int blocks = (int) Math.ceil((double) total / blockSize);
        int blockSizeSqrt = (int) Math.sqrt(blockSize);
        int blocksSqrt = (int) Math.sqrt(blocks);

        int startX = -blocksSqrt * blockSizeSqrt / 2;
        int startZ = -blocksSqrt * blockSizeSqrt / 2;

        for (int blockID = 0; blockID < blocks; blockID++)
            blocksList.add(new SquareBlock(
                    startX + (blockID % blocksSqrt) * blockSizeSqrt,
                    startZ + (blockID / blocksSqrt) * blockSizeSqrt,
                    blockSizeSqrt
            ));
    }

    @Override
    @SneakyThrows
    protected void next0(ChunkPosConsumer consumer) {
        int block = count / blockSize;
        blocksList.get(block).next(count % blockSize, consumer);
    }

    private record SquareBlock(int startX, int startZ, int h) {
        @SneakyThrows
        public void next(int position, ChunkPosConsumer consumer) {
            int x = position % h;
            int z = position / h;
            consumer.accept(startX + x, startZ + z);
        }
    }
}
