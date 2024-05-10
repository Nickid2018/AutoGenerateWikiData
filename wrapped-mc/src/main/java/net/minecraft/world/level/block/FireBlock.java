package net.minecraft.world.level.block;

import io.github.nickid2018.util.SneakyUtil;
import net.minecraft.world.level.block.state.BlockState;

public class FireBlock extends Block {

    public int getBurnOdds(BlockState blockState) {
        throw new RuntimeException();
    }

    public int getIgniteOdds(BlockState blockState) {
        throw new RuntimeException();
    }
}
