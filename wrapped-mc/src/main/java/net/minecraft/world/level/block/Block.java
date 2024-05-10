package net.minecraft.world.level.block;

import io.github.nickid2018.util.SneakyUtil;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class Block extends BlockBehaviour implements ItemLike {

    public StateDefinition<Block, BlockState> getStateDefinition() {
        throw new RuntimeException();
    }

    public final BlockState defaultBlockState() {
        return SneakyUtil.sneakyNotNull();
    }
}
