package io.github.nickid2018.genwiki;

import java.io.File;

public class Constants {

    public static final String VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest.json";

    public static final File SERVER_FOLDER = new File("servers");
    public static final File MAPPING_FOLDER = new File("mappings");
    public static final File REMAPPED_FOLDER = new File("remapped");
    public static final File RUNTIME_FOLDER = new File("runtime");

    public static final String INJECT_POINT_CLASS = "net.minecraft.server.MinecraftServer";
    public static final String INJECT_POINT_METHOD = "prepareLevels";
    public static final String INJECT_POINT_METHOD_DESC = "(Lnet/minecraft/server/level/progress/ChunkProgressListener;)V";
    public static final String INJECT_SERVER_PROPERTIES = "net.minecraft.server.dedicated.DedicatedServerProperties";
    public static final String INJECT_SERVER_PROPERTIES_METHOD = "getDatapackConfig";
    public static final String INJECT_SERVER_PROPERTIES_METHOD_DESC = "(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/world/level/DataPackConfig;";
    public static final String INJECT_CHUNK_STATISTICS_METHOD = "tickChildren";
    public static final String INJECT_CHUNK_STATISTICS_METHOD_DESC = "(Ljava/util/function/BooleanSupplier;)V";
    public static final String INJECT_REGION_FILE = "net.minecraft.world.level.chunk.storage.RegionFile";
    public static final String INJECT_REGION_FILE_METHOD = "write";
    public static final String INJECT_REGION_FILE_METHOD_DESC = "(Lnet/minecraft/world/level/ChunkPos;Ljava/nio/ByteBuffer;)V";
    public static final String INJECT_REGION_FILE_METHOD2 = "close";
    public static final String INJECT_REGION_FILE_METHOD2_DESC = "()V";
    public static final String INJECT_REGION_FILE_METHOD3 = "<init>";
    public static final String INJECT_REGION_FILE_METHOD3_DESC = "(Lnet/minecraft/world/level/chunk/storage/RegionStorageInfo;Ljava/nio/file/Path;Ljava/nio/file/Path;Lnet/minecraft/world/level/chunk/storage/RegionFileVersion;Z)V";
    public static final String INJECT_REGION_FILE_METHOD4 = "flush";
    public static final String INJECT_REGION_FILE_METHOD4_DESC = "()V";
}
