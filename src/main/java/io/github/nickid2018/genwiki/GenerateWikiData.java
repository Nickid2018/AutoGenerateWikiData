package io.github.nickid2018.genwiki;

import com.google.gson.JsonElement;
import io.github.nickid2018.genwiki.util.JsonUtils;
import io.github.nickid2018.genwiki.util.WebUtils;
import io.github.nickid2018.mcde.format.MappingFormat;
import io.github.nickid2018.mcde.format.MojangMappingFormat;
import io.github.nickid2018.mcde.remapper.FileProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

@Slf4j
public class GenerateWikiData {

    public static void main(String[] args) {
        if (args.length == 0) {
            log.error("Please specify the Minecraft version!");
            System.exit(1);
        }
        String version = args[0];
        boolean isChunkStatistics = args.length > 1 && args[1].equalsIgnoreCase("statistics");
        log.info("Generate Wiki Data, Minecraft Version: {}", version);

        File serverFile = new File(Constants.SERVER_FOLDER, version + ".jar");
        File serverMapping = new File(Constants.MAPPING_FOLDER, version + ".txt");
        if (!serverFile.exists() || !serverMapping.exists()) {
            try {
                log.info("Downloading server jar and mapping...");
                downloadServerJar(version);
            } catch (Exception e) {
                log.error("Failed to download server jar or mapping!", e);
                System.exit(1);
            }
        } else
            log.info("Server jar and mapping already exists, skip downloading.");

        File remappedFile = new File(Constants.REMAPPED_FOLDER, version + ".jar");
        if (!remappedFile.exists()) {
            try {
                log.info("Remapping server jar...");
                doRemap(serverFile, serverMapping, remappedFile, isChunkStatistics);
            } catch (Exception e) {
                log.error("Failed to remap server jar!", e);
                System.exit(1);
            }
        } else
            log.info("Remapped server jar already exists, skip remapping.");

        try {
            runWikiGenerator(remappedFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to run wiki generator!", e);
            System.exit(1);
        }
    }

    private static void downloadServerJar(String version) throws Exception {
        if (!Constants.SERVER_FOLDER.exists())
            Constants.SERVER_FOLDER.mkdirs();
        if (!Constants.MAPPING_FOLDER.exists())
            Constants.MAPPING_FOLDER.mkdirs();

        JsonElement versionManifest = WebUtils.getJson(Constants.VERSION_MANIFEST);
        String serverURL = null;
        for (JsonElement element : versionManifest.getAsJsonObject().getAsJsonArray("versions")) {
            String id = JsonUtils.getStringOrElse(element.getAsJsonObject(), "id", "");
            if (id.equalsIgnoreCase(version)) {
                serverURL = JsonUtils.getStringOrNull(element.getAsJsonObject(), "url");
                break;
            }
        }

        if (serverURL == null) {
            log.error("Cannot find server jar for version {}!", version);
            return;
        }

        JsonElement serverManifest = WebUtils.getJson(serverURL);
        String serverDownloadURL = JsonUtils.getStringInPath(serverManifest.getAsJsonObject(), "downloads.server.url")
                .orElseThrow(() -> new IOException("Cannot find server jar download url!"));
        String serverMappingURL = JsonUtils.getStringInPath(serverManifest.getAsJsonObject(),
                        "downloads.server_mappings.url")
                .orElseThrow(() -> new IOException("Cannot find server jar mapping url!"));

        File serverFile = new File(Constants.SERVER_FOLDER, version + ".jar");
        WebUtils.downloadFile(serverDownloadURL, serverFile);
        log.info("Downloaded server jar to {}", serverFile.getAbsolutePath());
        File serverMapping = new File(Constants.MAPPING_FOLDER, version + ".txt");
        WebUtils.downloadFile(serverMappingURL, serverMapping);
        log.info("Downloaded server jar mapping to {}", serverMapping.getAbsolutePath());
    }

    private static void doRemap(File input, File mapping, File output, boolean isChunkStatistics) throws Exception {
        if (!Constants.REMAPPED_FOLDER.exists())
            Constants.REMAPPED_FOLDER.mkdirs();
        MappingFormat format = new MojangMappingFormat(new FileInputStream(mapping));
        FileProcessor.processServer(input, format, output, isChunkStatistics);
        log.info("Remapped server jar to {}", output.getAbsolutePath());
    }

    private static void runWikiGenerator(String file) throws Exception {
        if (Constants.RUNTIME_FOLDER.exists())
            FileUtils.deleteDirectory(Constants.RUNTIME_FOLDER);
        Constants.RUNTIME_FOLDER.mkdirs();

        try (FileWriter w = new FileWriter(new File(Constants.RUNTIME_FOLDER, "eula.txt"))) {
            w.write("eula=true");
        }
        try (FileWriter w = new FileWriter(new File(Constants.RUNTIME_FOLDER, "server.properties"))) {
            w.write("max-tick-time=-1\nsync-chunk-writes=false");
        }

        String jvmArgs;
        if (System.getenv("JVM_ARGS") != null)
            jvmArgs = System.getenv("JVM_ARGS");
        else
            jvmArgs = "-Xmx6G -Xms2G";

        ProcessBuilder builder = new ProcessBuilder(
                "java", jvmArgs, "-jar", file, "-nogui"
        );
        builder.directory(Constants.RUNTIME_FOLDER);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        log.info("Launch server with command: '{}'", String.join(" ", builder.command()));

        Process process = builder.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (process.isAlive()) {
                    process.getOutputStream().write("stop\n".getBytes());
                    process.getOutputStream().flush();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
        process.waitFor();
    }
}
