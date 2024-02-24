package io.github.nickid2018.genwiki.statistic;

import lombok.SneakyThrows;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ToString(exclude = "blocksList", callSuper=true)
public class RandomChunkPosProvider extends ChunkPosProvider {

    private final List<SquareBlock> blocksList = new ArrayList<>();
    private final long seed;

    public RandomChunkPosProvider(int total, int blockSize) {
        super(total, blockSize);

        blockSize = this.blockSize;
        int blocks = (int) Math.ceil((double) total / blockSize);
        int blockSizeSqrt = (int) Math.sqrt(blockSize);
        int blocksSqrt = (int) Math.sqrt(blocks);

        Random random = new Random();
        seed = random.nextLong();
        random.setSeed(seed);

        for (int blockID = 0; blockID < blocks; blockID++)
            blocksList.add(new SquareBlock(
                    random.nextInt(1000000) - 500000 + (blockID % blocksSqrt) * blockSizeSqrt,
                    random.nextInt(1000000) - 500000 + (blockID / blocksSqrt) * blockSizeSqrt,
                    blockSizeSqrt
            ));
    }

    public RandomChunkPosProvider(int total, int blockSize, long seed) {
        super(total, blockSize);
        this.seed = seed;
        Random random = new Random(seed);

        blockSize = this.blockSize;
        int blocks = (int) Math.ceil((double) total / blockSize);
        int blockSizeSqrt = (int) Math.sqrt(blockSize);
        int blocksSqrt = (int) Math.sqrt(blocks);

        for (int blockID = 0; blockID < blocks; blockID++)
            blocksList.add(new SquareBlock(
                    random.nextInt(1000000) - 500000 + (blockID % blocksSqrt) * blockSizeSqrt,
                    random.nextInt(1000000) - 500000 + (blockID / blocksSqrt) * blockSizeSqrt,
                    blockSizeSqrt
            ));
    }

    @Override
    @SneakyThrows
    protected void next0(ChunkPosConsumer consumer) {
        int block = count / blockSize;
        blocksList.get(block).next(count % blockSize, consumer);
    }
}
