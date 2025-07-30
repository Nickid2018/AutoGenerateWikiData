package io.github.nickid2018.genwiki;

import com.google.common.collect.Streams;
import io.github.nickid2018.genwiki.remap.*;
import me.tongfei.progressbar.ProgressBar;
import org.jline.terminal.TerminalBuilder;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class RemapSettings {
    public static final String INJECT_POINT_CLASS = "net.minecraft.server.MinecraftServer";
    public static final String INJECT_METHOD = "tickChildren";
    public static final String INJECT_METHOD_DESC = "(Ljava/util/function/BooleanSupplier;)V";
    public static final String INJECT_SERVER_PROPERTIES = "net.minecraft.server.dedicated.DedicatedServerProperties";
    public static final String INJECT_SERVER_PROPERTIES_METHOD = "getDatapackConfig";
    public static final String INJECT_SERVER_PROPERTIES_METHOD_DESC = "(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/world/level/DataPackConfig;";
    public static final String INJECT_REGION_FILE = "net.minecraft.world.level.chunk.storage.RegionFile";
    public static final String INJECT_REGION_FILE_METHOD = "write";
    public static final String INJECT_REGION_FILE_METHOD_DESC = "(Lnet/minecraft/world/level/ChunkPos;Ljava/nio/ByteBuffer;)V";
    public static final String INJECT_REGION_FILE_METHOD2 = "close";
    public static final String INJECT_REGION_FILE_METHOD2_DESC = "()V";
    public static final String INJECT_REGION_FILE_METHOD3 = "<init>";
    public static final String INJECT_REGION_FILE_METHOD3_DESC = "(Lnet/minecraft/world/level/chunk/storage/RegionStorageInfo;Ljava/nio/file/Path;Ljava/nio/file/Path;Lnet/minecraft/world/level/chunk/storage/RegionFileVersion;Z)V";
    public static final String INJECT_REGION_FILE_METHOD4 = "flush";
    public static final String INJECT_REGION_FILE_METHOD4_DESC = "()V";

    private static void doReturn(MethodNode methodNode) {
        InsnList list = new InsnList();
        list.add(new InsnNode(Opcodes.RETURN));
        methodNode.instructions = list;
    }

    private static void commonServerSettings(RemapProgram remapProgram) {
        remapProgram.addPostTransform(
            "net.minecraft.world.flag.FeatureFlagRegistry",
            ExtendAccessTransform.FIELD
        );
        remapProgram.addPostTransform(
            "net.minecraft.world.level.biome.MobSpawnSettings",
            ExtendAccessTransform.FIELD
        );
        remapProgram.addPostTransform(
            "net.minecraft.world.level.block.state.BlockBehaviour$Properties",
            ExtendAccessTransform.FIELD
        );
        remapProgram.addPostTransform(
            "net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase",
            ExtendAccessTransform.FIELD
        );
        remapProgram.addPostTransform(
            "net.minecraft.world.level.block.FireBlock",
            ExtendAccessTransform.METHOD
        );
        remapProgram.addPostTransform(
            "net.minecraft.world.level.block.state.BlockBehaviour",
            ExtendAccessTransform.METHOD
        );
        remapProgram.addPostTransform(
            INJECT_REGION_FILE,
            new MethodTransform(
                INJECT_REGION_FILE_METHOD,
                INJECT_REGION_FILE_METHOD_DESC,
                RemapSettings::doReturn
            )
        );
        remapProgram.addPostTransform(
            INJECT_REGION_FILE,
            new MethodTransform(
                INJECT_REGION_FILE_METHOD2,
                INJECT_REGION_FILE_METHOD2_DESC,
                methodNode -> {
                    InsnList list = new InsnList();
                    list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    list.add(new FieldInsnNode(
                        Opcodes.GETFIELD,
                        "net/minecraft/world/level/chunk/storage/RegionFile",
                        "file", "Ljava/nio/channels/FileChannel;"
                    ));
                    list.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "java/nio/channels/FileChannel", "close", "()V", false
                    ));
                    list.add(new InsnNode(Opcodes.RETURN));
                    methodNode.instructions = list;
                    methodNode.tryCatchBlocks.clear();
                }
            )
        );
        remapProgram.addPostTransform(
            INJECT_REGION_FILE,
            new MethodTransform(
                INJECT_REGION_FILE_METHOD3,
                INJECT_REGION_FILE_METHOD3_DESC,
                methodNode -> {
                    InsnList list = new InsnList();
                    list.add(new FieldInsnNode(
                        Opcodes.GETSTATIC,
                        "io/github/nickid2018/genwiki/InjectionEntrypoint",
                        "NULL_PATH", "Ljava/nio/file/Path;"
                    ));
                    list.add(new VarInsnNode(Opcodes.ASTORE, 2));
                    list.add(methodNode.instructions);
                    methodNode.instructions = list;
                }
            )
        );
        remapProgram.addPostTransform(
            INJECT_REGION_FILE,
            new MethodTransform(
                INJECT_REGION_FILE_METHOD4,
                INJECT_REGION_FILE_METHOD4_DESC,
                RemapSettings::doReturn
            )
        );
        remapProgram.addInjectEntries(new IncludeJarPackages("io.github.nickid2018.genwiki.util"));
        remapProgram.addInjectEntries(new SingleFile(
            "io/github/nickid2018/genwiki/InjectionEntrypoint.class",
            "io/github/nickid2018/genwiki/InjectionEntrypoint.class"
        ));
    }

    public static void remapSettings(GenWikiMode mode, RemapProgram remapProgram) {
        if (mode == GenWikiMode.STATISTICS) {
            commonServerSettings(remapProgram);
            remapProgram.addPostTransform(
                INJECT_POINT_CLASS,
                new MethodTransform(
                    INJECT_METHOD, INJECT_METHOD_DESC, methodNode -> {
                    InsnList list = new InsnList();
                    list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    list.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "io/github/nickid2018/genwiki/InjectionEntrypoint",
                        "chunkStatisticsInjection",
                        "(Lnet/minecraft/server/MinecraftServer;)V",
                        false
                    ));
                    list.add(methodNode.instructions);
                    methodNode.instructions = list;
                }
                )
            );
            remapProgram.addPostTransform(
                "net.minecraft.world.level.chunk.storage.ChunkSerializer",
                new MethodTransform(
                    "write",
                    null,
                    methodNode -> {
                        InsnList list = new InsnList();
                        list.add(new TypeInsnNode(Opcodes.NEW, "net/minecraft/nbt/CompoundTag"));
                        list.add(new InsnNode(Opcodes.DUP));
                        list.add(new MethodInsnNode(
                            Opcodes.INVOKESPECIAL,
                            "net/minecraft/nbt/CompoundTag",
                            "<init>",
                            "()V",
                            false
                        ));
                        list.add(new InsnNode(Opcodes.ARETURN));
                        methodNode.instructions.clear();
                        methodNode.instructions.add(list);
                    }
                )
            );
            remapProgram.addPostTransform("net.minecraft.server.level.DistanceManager", ExtendAccessTransform.ALL);
            remapProgram.addPostTransform("net.minecraft.server.level.ServerChunkCache", ExtendAccessTransform.ALL);
            remapProgram.addInjectEntries(new IncludeJarPackages("io.github.nickid2018.genwiki.statistic"));
            remapProgram.addInjectEntries(new IncludeJarPackages("me.tongfei.progressbar", ProgressBar.class));
            remapProgram.addInjectEntries(new IncludeJarPackages("org.jline", TerminalBuilder.class));
        } else if (mode == GenWikiMode.AUTOVALUE) {
            commonServerSettings(remapProgram);
            remapProgram.addPostTransform(
                INJECT_POINT_CLASS,
                new MethodTransform(
                    INJECT_METHOD, INJECT_METHOD_DESC, methodNode -> {
                    InsnList list = new InsnList();
                    list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    list.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "io/github/nickid2018/genwiki/InjectionEntrypoint",
                        "extractDataInjection",
                        "(Lnet/minecraft/server/MinecraftServer;)V",
                        false
                    ));
                    list.add(methodNode.instructions);
                    methodNode.instructions = list;
                }
                )
            );
            remapProgram.addPostTransform(
                INJECT_SERVER_PROPERTIES,
                new MethodTransform(
                    INJECT_SERVER_PROPERTIES_METHOD,
                    INJECT_SERVER_PROPERTIES_METHOD_DESC,
                    methodNode -> {
                        InsnList list = new InsnList();
                        list.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "io/github/nickid2018/genwiki/InjectionEntrypoint",
                            "preprocessDataPacks",
                            "()Ljava/lang/String;",
                            false
                        ));
                        list.add(new VarInsnNode(Opcodes.ASTORE, 0));
                        methodNode.instructions.insert(list);
                    }
                )
            );
            remapProgram.addPostTransform(
                "net.minecraft.world.entity.ai.attributes.Attribute",
                ExtendAccessTransform.FIELD
            );
            remapProgram.addPostTransform(
                "net.minecraft.network.syncher.SynchedEntityData",
                ExtendAccessTransform.FIELD
            );
            remapProgram.addPostTransform(
                "net.minecraft.network.syncher.SynchedEntityData$DataItem",
                ExtendAccessTransform.FIELD
            );
            remapProgram.addPostTransform(
                "net.minecraft.world.level.block.entity.FuelValues",
                ExtendAccessTransform.FIELD
            );
            remapProgram.addInjectEntries(new IncludeJarPackages("io.github.nickid2018.genwiki.autovalue"));
        } else {
            remapProgram.addPostTransform(
                "net.minecraft.client.main.Main",
                new MethodTransform(
                    "main",
                    "([Ljava/lang/String;)V",
                    methodNode -> {
                        InsnList list = new InsnList();
                        list.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "io/github/nickid2018/genwiki/iso/ISOInjectionEntryPoints",
                            "onMinecraftBootstrap",
                            "()V",
                            false
                        ));
                        methodNode.instructions.insert(list);
                    }
                )
            );
            remapProgram.addPostTransform(
                "net.minecraft.client.renderer.GameRenderer",
                new AddMethodTransform(() -> {
                    MethodNode methodNode = new MethodNode(
                        Opcodes.ACC_PUBLIC,
                        "getProjectionMatrixWithInjection",
                        "(F)Lorg/joml/Matrix4f;",
                        null,
                        null
                    );
                    methodNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    methodNode.instructions.add(new VarInsnNode(Opcodes.FLOAD, 1));
                    methodNode.instructions.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "net/minecraft/client/renderer/GameRenderer",
                        "getProjectionMatrix",
                        "(F)Lorg/joml/Matrix4f;",
                        false
                    ));
                    methodNode.instructions.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "io/github/nickid2018/genwiki/iso/ISOInjectionEntryPoints",
                        "getProjectionMatrixInjection",
                        "(Lorg/joml/Matrix4f;)Lorg/joml/Matrix4f;",
                        false
                    ));
                    methodNode.instructions.add(new InsnNode(Opcodes.ARETURN));
                    return methodNode;
                })
            );
            remapProgram.addPostTransform(
                "net.minecraft.client.renderer.GameRenderer",
                new MethodTransform(
                    "renderLevel",
                    null,
                    methodNode -> Streams
                        .stream(methodNode.instructions.iterator())
                        .filter(insnNode -> insnNode instanceof MethodInsnNode)
                        .map(insnNode -> (MethodInsnNode) insnNode)
                        .filter(insnNode -> insnNode.name.equals("getProjectionMatrix"))
                        .findFirst()
                        .ifPresent(insnNode -> insnNode.name = "getProjectionMatrixWithInjection")
                )
            );
            remapProgram.addPostTransform(
                "net.minecraft.client.renderer.RenderStateShard",
                new MethodTransform(
                    "setupGlintTexturing",
                    null,
                    methodNode -> Streams
                        .stream(methodNode.instructions.iterator())
                        .filter(insnNode -> insnNode instanceof MethodInsnNode)
                        .map(insnNode -> (MethodInsnNode) insnNode)
                        .filter(insnNode -> insnNode.name.equals("getMillis"))
                        .findFirst()
                        .ifPresent(insnNode -> {
                            insnNode.owner = "io/github/nickid2018/genwiki/iso/ISOInjectionEntryPoints";
                            insnNode.name = "glintTimeInjection";
                        })
                )
            );
            remapProgram.addPostTransform(
                "net.minecraft.client.renderer.entity.DisplayRenderer$ItemDisplayRenderer",
                new RenameMethodTransform(
                    "submitInner",
                    "(Lnet/minecraft/client/renderer/entity/state/ItemDisplayEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IF)V",
                    "submitInnerOld"
                )
            );
            remapProgram.addPostTransform(
                "net.minecraft.client.renderer.entity.DisplayRenderer$ItemDisplayRenderer",
                new AddMethodTransform(() -> {
                    MethodNode methodNode = new MethodNode(
                        Opcodes.ACC_PUBLIC,
                        "submitInner",
                        "(Lnet/minecraft/client/renderer/entity/state/ItemDisplayEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IF)V",
                        null,
                        null
                    );
                    methodNode.instructions.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "io/github/nickid2018/genwiki/iso/ISOInjectionEntryPoints",
                        "renderItemDisplayInjection",
                        "()V",
                        false
                    ));
                    methodNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    methodNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    methodNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
                    methodNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3));
                    methodNode.instructions.add(new VarInsnNode(Opcodes.ILOAD, 4));
                    methodNode.instructions.add(new VarInsnNode(Opcodes.FLOAD, 5));
                    methodNode.instructions.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "net/minecraft/client/renderer/entity/DisplayRenderer$ItemDisplayRenderer",
                        "submitInnerOld",
                        "(Lnet/minecraft/client/renderer/entity/state/ItemDisplayEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IF)V",
                        false
                    ));
                    methodNode.instructions.add(new InsnNode(Opcodes.RETURN));
                    return methodNode;
                })
            );
            remapProgram.addPostTransform(
                "net.minecraft.client.multiplayer.ClientPacketListener",
                new MethodTransform(
                    "sendChat",
                    "(Ljava/lang/String;)V",
                    methodNode -> {
                        InsnList list = new InsnList();
                        list.add(new VarInsnNode(Opcodes.ALOAD, 1));
                        list.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "io/github/nickid2018/genwiki/iso/ISOInjectionEntryPoints",
                            "handleChat",
                            "(Ljava/lang/String;)V",
                            false
                        ));
                        methodNode.instructions.insertBefore(methodNode.instructions.getFirst(), list);
                    }
                )
            );
            remapProgram.addPostTransform(
                "net.minecraft.client.multiplayer.ClientPacketListener",
                new MethodTransform(
                    "tick",
                    "()V",
                    methodNode -> {
                        InsnList list = new InsnList();
                        list.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "io/github/nickid2018/genwiki/iso/ISOInjectionEntryPoints",
                            "listenerTick",
                            "()V",
                            false
                        ));
                        methodNode.instructions.insertBefore(methodNode.instructions.getFirst(), list);
                    }
                )
            );
            remapProgram.addPostTransform(
                "net.minecraft.client.renderer.fog.FogRenderer",
                new MethodTransform(
                    "setupFog",
                    null,
                    methodNode -> {
                        methodNode.instructions.clear();
                        methodNode.tryCatchBlocks.clear();
                        methodNode.instructions.add(new TypeInsnNode(Opcodes.NEW, "org/joml/Vector4f"));
                        methodNode.instructions.add(new InsnNode(Opcodes.DUP));
                        methodNode.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "org/joml/Vector4f", "<init>", "()V", false));
                        methodNode.instructions.add(new InsnNode(Opcodes.ARETURN));
                    }
                )
            );
            remapProgram.addPostTransform(
                "net.minecraft.client.renderer.fog.FogRenderer",
                new MethodTransform(
                    "computeFogColor",
                    null,
                    methodNode -> {
                        methodNode.instructions.clear();
                        methodNode.tryCatchBlocks.clear();
                        methodNode.instructions.add(new TypeInsnNode(Opcodes.NEW, "org/joml/Vector4f"));
                        methodNode.instructions.add(new InsnNode(Opcodes.DUP));
                        methodNode.instructions.add(new InsnNode(Opcodes.FCONST_0));
                        methodNode.instructions.add(new MethodInsnNode(
                            Opcodes.INVOKESPECIAL,
                            "org/joml/Vector4f",
                            "<init>",
                            "(F)V",
                            false
                        ));
                        methodNode.instructions.add(new InsnNode(Opcodes.ARETURN));
                    }
                )
            );
            remapProgram.addPostTransform(
                "net.minecraft.client.renderer.LevelRenderer",
                new MethodTransform(
                    "addSkyPass",
                    null,
                    methodNode -> {
                        methodNode.instructions.clear();
                        methodNode.instructions.add(new InsnNode(Opcodes.RETURN));
                    }
                )
            );
            remapProgram.addPostTransform(
                "net.minecraft.server.MinecraftServer",
                new MethodTransform(
                    "tickServer",
                    "(Ljava/util/function/BooleanSupplier;)V",
                    methodNode -> {
                        FieldInsnNode findAutoSaveIntervalPoint = null;
                        for (AbstractInsnNode node : methodNode.instructions) {
                            if (node instanceof FieldInsnNode fieldInsnNode) {
                                if (fieldInsnNode.getOpcode() == Opcodes.PUTFIELD &&
                                    fieldInsnNode.name.equals("ticksUntilAutosave")) {
                                    findAutoSaveIntervalPoint = fieldInsnNode;
                                    break;
                                }
                            }
                        }
                        if (findAutoSaveIntervalPoint != null) {
                            methodNode.instructions.insertBefore(
                                findAutoSaveIntervalPoint, new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    "io/github/nickid2018/genwiki/iso/ISOInjectionEntryPoints",
                                    "handleAutoSaveInterval",
                                    "(I)I",
                                    false
                                )
                            );
                        }
                    }
                )
            );
            remapProgram.addInjectEntries(new IncludeJarPackages("io.github.nickid2018.genwiki.iso"));
            remapProgram.addInjectEntries(new IncludeJarPackages("io.github.nickid2018.genwiki.util"));
            remapProgram.addInjectEntries(new SingleFile(
                "transparency.fsh",
                "assets/minecraft/shaders/post/transparency.fsh"
            ));
            remapProgram.addInjectEntries(new SingleFile(
                "lightmap.fsh",
                "assets/minecraft/shaders/core/lightmap.fsh"
            ));
        }
    }
}
