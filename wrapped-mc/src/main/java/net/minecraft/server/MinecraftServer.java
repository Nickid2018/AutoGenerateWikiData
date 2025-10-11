package net.minecraft.server;

import io.github.nickid2018.util.SneakyUtil;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.WorldData;

public abstract class MinecraftServer {

    public WorldData getWorldData() {
        return SneakyUtil.sneakyNotNull();
    }

    public final ServerLevel overworld() {
        return SneakyUtil.sneakyNotNull();
    }

    public Iterable<ServerLevel> getAllLevels() {
        throw new RuntimeException();
    }

    public RegistryAccess.Frozen registryAccess() {
        throw new RuntimeException();
    }

    public ServerTickRateManager tickRateManager() {
        throw new RuntimeException();
    }
}
