package net.minecraft.client.player;

import io.github.nickid2018.util.SneakyUtil;
import net.minecraft.client.multiplayer.ClientPacketListener;

public class LocalPlayer {

    public final ClientPacketListener connection = SneakyUtil.sneakyNotNull();
}
