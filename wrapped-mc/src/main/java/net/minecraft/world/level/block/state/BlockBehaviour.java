package net.minecraft.world.level.block.state;

import io.github.nickid2018.util.SneakyUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockBehaviour {

    public Properties properties() {
        throw new RuntimeException();
    }

    public static abstract class BlockStateBase extends StateHolder<Block, BlockState> {
        public boolean legacySolid;
        public final MapColor mapColor = SneakyUtil.sneakyNotNull();

        public Block getBlock() {
            throw new RuntimeException();
        }

        public boolean is(TagKey<Block> tagKey) {
            throw new RuntimeException();
        }

        public boolean isFaceSturdy(BlockGetter blockGetter, BlockPos blockPos, Direction direction, SupportType supportType) {
            throw new RuntimeException();
        }

        public VoxelShape getFaceOcclusionShape(Direction direction) {
            throw new RuntimeException();
        }

        public boolean canOcclude() {
            throw new RuntimeException();
        }

        public boolean blocksMotion() {
            throw new RuntimeException();
        }
    }

    public interface StatePredicate {
        boolean test(BlockState var1, BlockGetter var2, BlockPos var3);
    }

    public static class Properties {
        public float explosionResistance;
        public float destroyTime;
        public boolean requiresCorrectToolForDrops;
        public boolean ignitedByLava;
        public PushReaction pushReaction;
        public NoteBlockInstrument instrument;
        public boolean replaceable;
        public StatePredicate isRedstoneConductor;
        public StatePredicate isSuffocating;
    }
}
