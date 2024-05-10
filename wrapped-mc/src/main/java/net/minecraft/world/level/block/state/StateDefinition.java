package net.minecraft.world.level.block.state;

import com.google.common.collect.ImmutableList;

public class StateDefinition<O, S> {

    public ImmutableList<S> getPossibleStates() {
        throw new RuntimeException();
    }
}
