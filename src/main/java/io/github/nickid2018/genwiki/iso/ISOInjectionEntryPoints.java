package io.github.nickid2018.genwiki.iso;

import com.mojang.blaze3d.pipeline.RenderTarget;
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
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private static int invokeCount = 0;
    private static boolean noSave = false;
    private static Matrix4f orthoMatrix = new Matrix4f().ortho(-2, 2, -2, 2, -0.1f, 1000);

    public static Matrix4f getProjectionMatrixInjection(Matrix4f source) {
        invokeCount = (invokeCount + 1) % 3;
        if (invokeCount == 2) // Avoid Frustum Culling
            return source;
        if (ortho)
            return orthoMatrix;
        return source;
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
                    }
                }
            }
        } catch (Exception e) {
            log.error("Command Error", e);
        }
    }

    private static final ExecutorService commandExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("Command Executor");
        return t;
    });

    public static void readFileAndRun(String file) {
        try {
            File commandFile = new File(file);
            String command = IOUtils.toString(commandFile.toURI(), StandardCharsets.UTF_8);
            List<String> collectedLines = Arrays
                .stream(command.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !s.startsWith("#"))
                .toList();
            commandExecutor.execute(() -> {
                for (String line : collectedLines) {
                    if (line.toLowerCase().startsWith("sleep"))
                        try {
                            Thread.sleep(Long.parseLong(line.substring(5).trim()));
                        } catch (Exception e) {
                            log.error("Sleep Error", e);
                        }
                    else
                        commandQueue.add(line);
                }
            });
        } catch (Exception e) {
            log.error("Read File Error", e);
        }
    }
}
