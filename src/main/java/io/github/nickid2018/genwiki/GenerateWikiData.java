package io.github.nickid2018.genwiki;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.nickid2018.genwiki.remap.MojangMapping;
import io.github.nickid2018.genwiki.remap.RemapProgram;
import io.github.nickid2018.genwiki.util.JsonUtils;
import io.github.nickid2018.genwiki.util.WebUtils;
import joptsimple.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GenerateWikiData {
    public static final String VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest.json";

    public static final File SERVER_FOLDER = new File("servers");
    public static final File MAPPING_FOLDER = new File("mappings");
    public static final File REMAPPED_FOLDER = new File("remapped");
    public static final File RUNTIME_FOLDER = new File("runtime");

    @SneakyThrows
    public static void main(String[] args) {
        OptionParser parser = new OptionParser();
        NonOptionArgumentSpec<String> versionSpec = parser.nonOptions("Minecraft version");
        ArgumentAcceptingOptionSpec<String> modeSpec = parser
            .accepts("mode", "Generate chunk statistics")
            .withOptionalArg().defaultsTo("autovalue");
        OptionSpecBuilder doNotRun = parser.accepts("not-run", "Do not run server");
        AbstractOptionSpec<Void> helpSpec = parser.accepts("help", "Show help").forHelp();

        OptionSet options;
        try {
            options = parser.parse(args);
        } catch (Exception e) {
            parser.printHelpOn(System.err);
            System.exit(1);
            return;
        }

        if (options.has(helpSpec)) {
            parser.printHelpOn(System.out);
            return;
        }

        boolean notRun = options.has(doNotRun);
        String version = versionSpec.value(options);
        String mode = modeSpec.value(options);

        boolean isChunkStatistics = mode.equalsIgnoreCase("statistics");
        log.info("Generate Wiki Data, Minecraft Version: {}", version);
        log.info("Running mode: {}", mode);
        if (notRun)
            log.info("Do not run server, file will be generated in {}/{}.jar", REMAPPED_FOLDER, version);

        File serverFile = new File(SERVER_FOLDER, version + ".jar");
        File serverMapping = new File(MAPPING_FOLDER, version + ".txt");
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

        File remappedFile = new File(REMAPPED_FOLDER, version + ".jar");
        if (notRun || !remappedFile.exists()) {
            try {
                log.info("Remapping server jar...");
                doRemap(serverFile, serverMapping, remappedFile, isChunkStatistics);
            } catch (Exception e) {
                log.error("Failed to remap server jar!", e);
                System.exit(1);
            }
        } else
            log.info("Remapped server jar already exists, skip remapping.");

        if (!notRun)
            try {
                runWikiGenerator(remappedFile.getAbsolutePath());
            } catch (Exception e) {
                log.error("Failed to run wiki generator!", e);
                System.exit(1);
            }
    }

    private static void downloadServerJar(String version) throws Exception {
        if (!SERVER_FOLDER.exists())
            SERVER_FOLDER.mkdirs();
        if (!MAPPING_FOLDER.exists())
            MAPPING_FOLDER.mkdirs();

        JsonElement versionManifest = WebUtils.getJson(VERSION_MANIFEST);
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

        JsonObject serverManifest = WebUtils.getJson(serverURL).getAsJsonObject();
        String serverDownloadURL = JsonUtils
            .getStringInPath(serverManifest, "downloads.server.url")
            .orElseThrow(() -> new IOException("Cannot find server jar download url!"));
        String serverMappingURL = JsonUtils
            .getStringInPath(serverManifest, "downloads.server_mappings.url")
            .orElseThrow(() -> new IOException("Cannot find server jar mapping url!"));

        File serverFile = new File(SERVER_FOLDER, version + ".jar");
        WebUtils.downloadFile(serverDownloadURL, serverFile);
        log.info("Downloaded server jar to {}", serverFile.getAbsolutePath());
        File serverMapping = new File(MAPPING_FOLDER, version + ".txt");
        WebUtils.downloadFile(serverMappingURL, serverMapping);
        log.info("Downloaded server jar mapping to {}", serverMapping.getAbsolutePath());
    }

    private static void doRemap(File input, File mapping, File output, boolean isChunkStatistics) throws Exception {
        if (!REMAPPED_FOLDER.exists())
            REMAPPED_FOLDER.mkdirs();
        MojangMapping format = new MojangMapping(new FileInputStream(mapping));
        RemapProgram program = new RemapProgram(format, input, output);
        RemapSettings.remapSettings(isChunkStatistics, program);
        program.extractServer();
        program.fillRemapFormat();
        program.remapClasses();
        program.rePackServer();
        log.info("Remapped server jar to {}", output.getAbsolutePath());
    }

    private static void runWikiGenerator(String file) throws Exception {
        if (RUNTIME_FOLDER.exists())
            FileUtils.deleteDirectory(RUNTIME_FOLDER);
        RUNTIME_FOLDER.mkdirs();

        try (FileWriter w = new FileWriter(new File(RUNTIME_FOLDER, "eula.txt"))) {
            w.write("eula=true");
        }
        try (FileWriter w = new FileWriter(new File(RUNTIME_FOLDER, "server.properties"))) {
            w.write("max-tick-time=-1\nsync-chunk-writes=false");
        }

        String javaExec = System.getProperty("java.home") + "/bin/java";
        if (System.getProperty("os.name").startsWith("Windows"))
            javaExec += ".exe";

        ProcessBuilder builder;
        if (System.getenv("JVM_ARGS") != null) {
            String[] jvmArgs = System.getenv("JVM_ARGS").split(" ");
            List<String> list = new ArrayList<>();
            list.add(javaExec);
            list.addAll(List.of(jvmArgs));
            list.add("-jar");
            list.add(file);
            list.add("-nogui");
            builder = new ProcessBuilder(list);
        } else
            builder = new ProcessBuilder(javaExec, "-jar", file, "-nogui");

        log.info("Launch server with command: '{}'", String.join(" ", builder.command()));

        Process process = builder
            .directory(RUNTIME_FOLDER)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .start();

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
