package io.github.nickid2018.genwiki.statistic;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;

@Getter
@ToString(exclude = "count")
public abstract class ChunkPosProvider {

    protected int count = 0;
    protected int total;
    protected int blockSize;

    public ChunkPosProvider(int total, int blockSize) {
        this.total = total;
        this.blockSize = (int) Math.pow(Math.ceil(Math.sqrt(blockSize)), 2);
    }

    public boolean hasNext() {
        return count < total;
    }

    public void next(ChunkPosConsumer consumer) {
        if (count >= total)
            return;
        next0(consumer);
        count++;
    }

    protected abstract void next0(ChunkPosConsumer consumer);

    public interface ChunkPosConsumer {
        void accept(int x, int z) throws Throwable;
    }
}
