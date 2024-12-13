package io.github.nickid2018.genwiki.statistic;

import lombok.SneakyThrows;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString(exclude = "blocksList", callSuper = true)
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
}
