package net.minecraft.server.level;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ServerLevel extends Level {

    public boolean noSave;

    public long getSeed() {
        throw new RuntimeException();
    }

    public ServerChunkCache getChunkSource() {
        throw new RuntimeException();
    }

    @Override
    public BlockState getBlockState(BlockPos var1) {
        throw new RuntimeException();
    }
}
