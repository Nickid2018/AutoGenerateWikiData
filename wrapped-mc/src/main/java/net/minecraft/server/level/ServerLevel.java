package net.minecraft.server.level;

import net.minecraft.world.level.Level;

public class ServerLevel extends Level {

    public boolean noSave;

    public long getSeed() {
        throw new RuntimeException();
    }

    public ServerChunkCache getChunkSource() {
        throw new RuntimeException();
    }
}
