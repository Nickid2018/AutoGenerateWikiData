package io.github.nickid2018.genwiki.statistic;

import lombok.SneakyThrows;
import lombok.ToString;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ToString(exclude = "blocksList", callSuper = true)
public class RandomChunkPosProvider extends ChunkPosProvider {

    private final List<SquareBlock> blocksList = new ArrayList<>();
    private final long seed;

    public RandomChunkPosProvider(int total, int blockSize) {
        this(total, blockSize, new SecureRandom().nextLong());
    }

    public RandomChunkPosProvider(int total, int blockSize, long seed) {
        super(total, blockSize);
        this.seed = seed;
        Random random = new Random(seed);

        blockSize = this.blockSize;
        int blocks = (int) Math.ceil((double) total / blockSize);
        int blockSizeSqrt = (int) Math.sqrt(blockSize);
        int blocksSqrt = (int) Math.sqrt(blocks);

        int blockID = 0;
        while (blockID < blocks) {
            SquareBlock block = new SquareBlock(
                random.nextInt(1000000) - 500000 + (blockID % blocksSqrt) * blockSizeSqrt,
                random.nextInt(1000000) - 500000 + (blockID / blocksSqrt) * blockSizeSqrt,
                blockSizeSqrt
            );
            if (blocksList
                .stream()
                .noneMatch(other -> Math.abs(other.startX() - block.startX()) < blockSizeSqrt && Math.abs(other.startZ() - block.startZ()) < blockSizeSqrt)
            ) {
                blocksList.add(block);
                blockID++;
            }
        }
    }

    @Override
    @SneakyThrows
    protected void next0(ChunkPosConsumer consumer) {
        int block = count / blockSize;
        blocksList.get(block).next(count % blockSize, consumer);
    }
}
