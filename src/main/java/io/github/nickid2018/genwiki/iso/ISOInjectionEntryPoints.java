package io.github.nickid2018.genwiki.iso;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

@Slf4j
@SuppressWarnings("unused")
public class ISOInjectionEntryPoints {

    public static void clampColorInjection(Vector3f vector3f) {
        vector3f.set(1, 1, 1);
    }

    private static boolean ortho = false;
    private static int invokeCount = 0;
    private static Matrix4f orthoMatrix = new Matrix4f().ortho(-2, 2, -2, 2, -0.1f, 1000);

    public static Matrix4f getProjectionMatrixInjection(Matrix4f source) {
        invokeCount = (invokeCount + 1) % 3;
        if (invokeCount == 2) // Avoid Frustum Culling
            return source;
        if (ortho)
            return orthoMatrix;
        return source;
    }

    public static void handleChat(ClientPacketListener clientPacketListener, String chat) {
        chat = chat.toLowerCase();
        try {
            switch (chat) {
                case "persp" -> ortho = false;
                case "ortho" -> ortho = true;
                default -> {
                    String[] commands = chat.split(" ", 2);
                    String commandHead = commands[0];
                    String commandArgs = commands.length > 1 ? commands[1] : "";
                    switch (commandHead) {
                        case "wsize" -> {
                            String[] sizes = commandArgs.split("[xX]");
                            if (sizes.length == 2) {
                                int width = Integer.parseInt(sizes[0]);
                                int height = Integer.parseInt(sizes[1]);
                                long window = Minecraft.getInstance().getWindow().getWindow();
                                float[] xScale = new float[1];
                                float[] yScale = new float[1];
                                GLFW.glfwGetWindowContentScale(window, xScale, yScale);
                                GLFW.glfwSetWindowSize(
                                    window,
                                    Math.round(width / xScale[0]),
                                    Math.round(height / yScale[0])
                                );
                            }
                        }
                        case "osize" -> {
                            String[] sizes = commandArgs.split("[xX]");
                            if (sizes.length == 2) {
                                float left = -Float.parseFloat(sizes[0]) / 2;
                                float right = Float.parseFloat(sizes[0]) / 2;
                                float bottom = -Float.parseFloat(sizes[1]) / 2;
                                float top = Float.parseFloat(sizes[1]) / 2;
                                orthoMatrix = new Matrix4f().ortho(left, right, bottom, top, -0.1f, 1000);
                            }
                        }
                        case "sshot" -> {
                            RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
                            mainTarget.bindWrite(true);
                            Minecraft.getInstance().gameRenderer.renderLevel(DeltaTracker.ONE);
                            NativeImage image = new NativeImage(mainTarget.width, mainTarget.height, false);
                            RenderSystem.bindTexture(mainTarget.getColorTextureId());
                            image.downloadTexture(0, true);
                            image.flipY();
                            BufferedImage bufferedImage = new BufferedImage(
                                mainTarget.width,
                                mainTarget.height,
                                BufferedImage.TYPE_INT_ARGB
                            );
                            for (int x = 0; x < mainTarget.width; x++) {
                                for (int y = 0; y < mainTarget.height; y++) {
                                    int color = image.getPixelRGBA(x, y);
                                    if (color == 0xFF000000)
                                        color = 0;
                                    bufferedImage.setRGB(
                                        x,
                                        y,
                                        (color >>> 16 & 0xFF) | (color & 0xFF00FF00) | (color << 16 & 0xFF0000)
                                    );
                                }
                            }
                            File file = new File("screenshots");
                            file.mkdir();
                            ImageIO.write(
                                bufferedImage,
                                "png",
                                new File(file, commandArgs.isEmpty() ? "screenshot.png" : commandArgs)
                            );
                        }
                        case "call" -> clientPacketListener.sendCommand(commandArgs);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Command Error", e);
        }
    }
}
