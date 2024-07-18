package net.minecraft.world.entity;

import net.minecraft.network.syncher.SynchedEntityData;

public abstract class Entity {

    public SynchedEntityData getEntityData() {
        throw new RuntimeException();
    }
}
