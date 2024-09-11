package net.minecraft.world.level.block.state.properties;

import java.util.List;

public abstract class Property<T extends Comparable<T>> {

    public abstract List<T> getPossibleValues();

    public String getName() {
        throw new RuntimeException();
    }

    public abstract String getName(T var1);
}
