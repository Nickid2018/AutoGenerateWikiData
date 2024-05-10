package net.minecraft.world.level.block.state.properties;

import java.util.Collection;

public abstract class Property<T extends Comparable<T>> {

    public abstract Collection<T> getPossibleValues();

    public String getName() {
        throw new RuntimeException();
    }

    public abstract String getName(T var1);
}
