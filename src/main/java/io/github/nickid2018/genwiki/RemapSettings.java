package io.github.nickid2018.genwiki;

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
        remapProgram.addInjectEntries(new SingleFile("io.github.nickid2018.genwiki.InjectionEntrypoint"));
    }

    public static void remapSettings(GenWikiMode mode, RemapProgram remapProgram) {
        if (mode == GenWikiMode.STATISTICS) {
            commonServerSettings(remapProgram);
            remapProgram.addPostTransform(
                INJECT_POINT_CLASS,
                new MethodTransform(INJECT_METHOD, INJECT_METHOD_DESC, methodNode -> {
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
                })
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
                new MethodTransform(INJECT_METHOD, INJECT_METHOD_DESC, methodNode -> {
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
                })
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
            remapProgram.addInjectEntries(new IncludeJarPackages("io.github.nickid2018.genwiki.autovalue"));
        }
    }
}
