package io.github.nickid2018.genwiki.iso;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.extern.slf4j.Slf4j;
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

    public static void onMinecraftBootstrap() {
        log.info("ISO Injection Entry Points Loaded");
    }

    public static void clampColorInjection(Vector3f vector3f) {
        vector3f.set(1, 1, 1);
    }

    private static boolean ortho = false;
    private static boolean noSave = false;
    private static long nextCommandCanExecute = 0;
    private static boolean flatLight = false;
    private static boolean blockLight = false;
    private static Matrix4f orthoMatrix = new Matrix4f().ortho(-2, 2, -2, 2, -0.1f, 1000);

    public static Matrix4f getProjectionMatrixInjection(Matrix4f source) {
        if (ortho)
            return orthoMatrix;
        return source;
    }

    public static void renderItemDisplayInjection() {
        if (flatLight)
            Lighting.setupForFlatItems();
        if (blockLight)
            Lighting.setupFor3DItems();
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
                            mainTarget.bindWrite(true);
                            Minecraft.getInstance().gameRenderer.renderLevel(DeltaTracker.ONE);
                            NativeImage image = new NativeImage(mainTarget.width, mainTarget.height, false);
                            RenderSystem.bindTexture(mainTarget.getColorTextureId());
                            image.downloadTexture(0, false);
                            image.flipY();
                            File file = new File("screenshots");
                            file.mkdir();
                            image.writeToFile(new File(
                                file,
                                commandArgs.isEmpty() ? "screenshot.png" : commandArgs
                            ).toPath());
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
