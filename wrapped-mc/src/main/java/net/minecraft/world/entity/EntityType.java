package net.minecraft.world.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class EntityType<T extends Entity> {

    public MobCategory getCategory() {
        throw new RuntimeException();
    }

    public T spawn(ServerLevel serverLevel, BlockPos blockPos, EntitySpawnReason mobSpawnType) {
        throw new RuntimeException();
    }
}
