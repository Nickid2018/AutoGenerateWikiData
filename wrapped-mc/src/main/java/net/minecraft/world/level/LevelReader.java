package net.minecraft.world.level;

public interface LevelReader extends BlockGetter {

    default int getMinBuildHeight() {
        throw new RuntimeException();
    }

    default int getHeight() {
        throw new RuntimeException();
    }
}
