package net.minecraft.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import io.github.nickid2018.util.SneakyUtil;
import net.minecraft.client.renderer.GameRenderer;

public class Minecraft {

    public final GameRenderer gameRenderer = SneakyUtil.sneakyNotNull();

    public static Minecraft getInstance() {
        return SneakyUtil.sneakyNotNull();
    }

    public Window getWindow() {
        throw new RuntimeException();
    }

    public RenderTarget getMainRenderTarget() {
        throw new RuntimeException();
    }
}
