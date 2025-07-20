package io.github.nickid2018.genwiki.iso;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import io.github.nickid2018.genwiki.util.LanguageUtils;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import org.apache.commons.io.IOUtils;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
@SuppressWarnings("unused")
public class ISOInjectionEntryPoints {

    private static final Vector3f DIFFUSE_LIGHT_0 = new Vector3f(0.2f, 1.0f, -0.7f).normalize();
    private static final Vector3f DIFFUSE_LIGHT_1 = new Vector3f(-0.2f, 1.0f, 0.7f).normalize();

    public static void onMinecraftBootstrap() {
        log.info("ISO Injection Entry Points Loaded");
    }

    public static void clampColorInjection(Vector3f vector3f) {
        vector3f.set(1, 1, 1);
    }

    private static boolean ortho = false;
    private static boolean noSave = false;
    private static boolean flatLight = false;
    private static boolean blockLight = false;
    private static long glintTimer = -1;
    private static long nextCommandCanExecute = 0;
    private static Matrix4f orthoMatrix = new Matrix4f().ortho(-2, 2, -2, 2, -0.1f, 1000);

    public static void setupLevelDiffuseLighting(Vector3f vector3f, Vector3f vector3f2, Matrix4f matrix4f) {
        RenderSystem.assertOnRenderThread();
        RenderSystem.setShaderLights(
            matrix4f.transformDirection(vector3f, new Vector3f()),
            matrix4f.transformDirection(vector3f2, new Vector3f())
        );
    }

    public static Matrix4f getProjectionMatrixInjection(Matrix4f source) {
        if (ortho)
            return orthoMatrix;
        return source;
    }

    public static long glintTimeInjection() {
        if (glintTimer >= 0)
            return glintTimer * 50;
        return Util.getMillis();
    }

    public static void renderItemDisplayInjection() {
        if (flatLight)
            Lighting.setupForFlatItems();
        if (blockLight) {
            RenderSystem.assertOnRenderThread();
            Matrix4f matrix4f = new Matrix4f().rotateYXZ(1.0821041f, 3.2375858f, 0.0f).rotateYXZ(
                -0.3926991f,
                2.3561945f,
                0.0f
            );
            setupLevelDiffuseLighting(DIFFUSE_LIGHT_0, DIFFUSE_LIGHT_1, matrix4f);
        }
    }

    public static int handleAutoSaveInterval(int source) {
        if (noSave)
            return 1;
        return source;
    }

    public static void handleChat(String chat) {
        chat = chat.trim();
        if (chat.toLowerCase().startsWith("run"))
            readFileAndRun(chat.substring(3).trim());
        else
            doCommand(chat);
    }

    private static final Queue<String> commandQueue = new ConcurrentLinkedDeque<>();

    public static void listenerTick() {
        if (System.currentTimeMillis() < nextCommandCanExecute)
            return;
        if (!commandQueue.isEmpty()) {
            String command = commandQueue.poll();
            doCommand(command);
        }
    }

    public static void doCommand(String chat) {
        try {
            switch (chat.toLowerCase()) {
                case "persp" -> ortho = false;
                case "ortho" -> ortho = true;
                case "nosave" -> noSave = true;
                case "save" -> noSave = false;
                case "flatlight" -> {
                    flatLight = true;
                    blockLight = false;
                }
                case "blocklight" -> {
                    flatLight = false;
                    blockLight = true;
                }
                case "levellight" -> {
                    flatLight = false;
                    blockLight = false;
                }
                case "skiptick" -> {
                    // Do nothing
                }
                case "glinttick" -> {
                    if (glintTimer < 0)
                        glintTimer = 0;
                    else
                        glintTimer++;
                }
                case "resetglint" -> glintTimer = -1;
                default -> {
                    String[] commands = chat.split(" ", 2);
                    String commandHead = commands[0];
                    String commandArgs = commands.length > 1 ? commands[1] : "";
                    switch (commandHead.toLowerCase()) {
                        case "wsize" -> {
                            String[] sizes = commandArgs.split("[xX]");
                            if (sizes.length == 2) {
                                int width = Integer.parseInt(sizes[0]);
                                int height = Integer.parseInt(sizes[1]);
                                long window = Minecraft.getInstance().getWindow().getWindow();
                                GLFW.glfwRestoreWindow(window);
                                GLFW.glfwSetWindowSize(window, width, height);
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
                            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
                            GpuTexture texture = mainTarget.getColorTexture();
                            encoder.clearColorAndDepthTextures(
                                mainTarget.getColorTexture(),
                                0,
                                mainTarget.getDepthTexture(),
                                1.0
                            );
                            Minecraft.getInstance().gameRenderer.renderLevel(DeltaTracker.ONE);
                            GpuBuffer buffer = RenderSystem.getDevice().createBuffer(
                                () -> "SSHOT",
                                9,
                                mainTarget.width * mainTarget.height * texture.getFormat().pixelSize()
                            );
                            encoder.copyTextureToBuffer(
                                mainTarget.getColorTexture(), buffer, 0, LanguageUtils.sneakyExceptionRunnable(
                                    () -> {
                                        try (GpuBuffer.MappedView readView = encoder.mapBuffer(buffer, true ,false)) {
                                            NativeImage image = new NativeImage(
                                                mainTarget.width,
                                                mainTarget.height,
                                                false
                                            );
                                            for (int i = 0; i < mainTarget.height; ++i) {
                                                for (int j = 0; j < mainTarget.width; ++j) {
                                                    int color = readView
                                                        .data()
                                                        .getInt((j + i * mainTarget.width) * texture
                                                            .getFormat()
                                                            .pixelSize());
                                                    image.setPixelABGR(j, mainTarget.height - i - 1, color);
                                                }
                                            }
                                            File file = new File("screenshots");
                                            file.mkdir();
                                            image.writeToFile(new File(
                                                file,
                                                commandArgs.isEmpty() ? "screenshot.png" : commandArgs
                                            ).toPath());
                                        }
                                        buffer.close();
                                    }, e -> log.error("Error when taking screenshot", e)
                                ), 0
                            );
                        }
                        case "call" -> Minecraft.getInstance().player.connection.sendCommand(commandArgs);
                        case "sleep" -> {
                            long sleepTime = Long.parseLong(commandArgs);
                            nextCommandCanExecute = System.currentTimeMillis() + sleepTime;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Command Error", e);
        }
    }

    public static void readFileAndRun(String file) {
        try {
            File commandFile = new File(file);
            String command = IOUtils.toString(commandFile.toURI(), StandardCharsets.UTF_8);
            Arrays
                .stream(command.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !s.startsWith("#"))
                .forEach(commandQueue::offer);
        } catch (Exception e) {
            log.error("Read File Error", e);
        }
    }
}
