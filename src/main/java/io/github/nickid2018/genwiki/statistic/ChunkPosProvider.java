package io.github.nickid2018.genwiki.statistic;

import lombok.Getter;
import lombok.SneakyThrows;

@Getter
public abstract class ChunkPosProvider {

    protected int count = 0;
    protected int total;

    public ChunkPosProvider(int total) {
        this.total = total;
    }

    public boolean hasNext() {
        return count < total;
    }

    public void next(ChunkPosConsumer consumer) {
        if (count >= total)
            return;
        count++;
        next0(consumer);
    }

    protected abstract void next0(ChunkPosConsumer consumer);

    public interface ChunkPosConsumer {
        void accept(int x, int z) throws Throwable;
    }
}
