package net.minecraft.world.level;

public interface LevelReader extends BlockGetter {

    default int getMinY() {
        throw new RuntimeException();
    }

    default int getHeight() {
        throw new RuntimeException();
    }
}
