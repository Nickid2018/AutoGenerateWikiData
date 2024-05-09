package net.minecraft.world.level;

public interface LevelReader {

    default int getMinBuildHeight() {
        throw new RuntimeException();
    }

    default int getHeight() {
        throw new RuntimeException();
    }
}
