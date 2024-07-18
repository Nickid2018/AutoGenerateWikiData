package net.minecraft.network.syncher;

import io.github.nickid2018.util.SneakyUtil;

public class SynchedEntityData {

    public final DataItem<?>[] itemsById = SneakyUtil.sneakyNotNull();

    public static class DataItem<T> {
        public final EntityDataAccessor<T> accessor = SneakyUtil.sneakyNotNull();
    }
}
