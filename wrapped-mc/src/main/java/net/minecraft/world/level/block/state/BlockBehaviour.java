package net.minecraft.world.level.block.state;

import net.minecraft.world.level.block.Block;

public class BlockBehaviour {

    public static abstract class BlockStateBase {

        public Block getBlock() {
            throw new RuntimeException();
        }
    }
}
