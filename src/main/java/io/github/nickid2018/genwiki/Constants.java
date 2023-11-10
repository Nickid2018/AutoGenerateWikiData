package io.github.nickid2018.genwiki;

import java.io.File;

public class Constants {

    public static final String VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest.json";

    public static final File SERVER_FOLDER = new File("servers");
    public static final File MAPPING_FOLDER = new File("mappings");
    public static final File REMAPPED_FOLDER = new File("remapped");
    public static final File RUNTIME_FOLDER = new File("runtime");

    public static final String INJECT_POINT_CLASS = "net.minecraft.server.MinecraftServer";
    public static final String INJECT_POINT_METHOD = "createLevels";
    public static final String INJECT_POINT_METHOD_DESC = "(Lnet/minecraft/server/level/progress/ChunkProgressListener;)V";
    public static final String INJECT_SERVER_PROPERTIES = "net.minecraft.server.dedicated.DedicatedServerProperties";
    public static final String INJECT_SERVER_PROPERTIES_METHOD = "getDatapackConfig";
    public static final String INJECT_SERVER_PROPERTIES_METHOD_DESC = "(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/world/level/DataPackConfig;";

    public static final File DEBUG_CLASS_PATH = new File("injected.class");
}
