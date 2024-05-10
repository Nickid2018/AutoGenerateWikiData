package net.minecraft.world.phys.shapes;

import net.minecraft.world.phys.AABB;

import java.util.List;

public abstract class VoxelShape {

    public List<AABB> toAabbs() {
        throw new RuntimeException();
    }

    public boolean isEmpty() {
        throw new RuntimeException();
    }
}
