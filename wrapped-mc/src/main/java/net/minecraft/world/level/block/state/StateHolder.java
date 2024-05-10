package net.minecraft.world.level.block.state;

import net.minecraft.world.level.block.state.properties.Property;

import java.util.Map;

public abstract class StateHolder<O, S> {

    public Map<Property<?>, Comparable<?>> getValues() {
        throw new RuntimeException();
    }
}
