package io.github.nickid2018.genwiki;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.nickid2018.genwiki.remap.MojangMapping;
import io.github.nickid2018.genwiki.remap.RemapProgram;
import io.github.nickid2018.genwiki.util.JsonUtils;
import joptsimple.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.*;

@Slf4j
public class GenerateWikiData {

    @SneakyThrows
    public static void main(String[] args) {
        OptionParser parser = new OptionParser();
        NonOptionArgumentSpec<String> versionSpec = parser.nonOptions("Minecraft version");
        ArgumentAcceptingOptionSpec<GenWikiMode> modeSpec = parser
            .accepts("mode", "Running mode")
            .withOptionalArg()
            .ofType(GenWikiMode.class)
            .defaultsTo(GenWikiMode.AUTOVALUE);
        OptionSpecBuilder doNotRun = parser.accepts("not-run", "Do not run server");
        OptionSpecBuilder doForceReDownload = parser.accepts(
            "force-redownload",
            "Force re-download server jar and mapping"
        );
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
        boolean forceReDownload = options.has(doForceReDownload);
        GenWikiMode mode = modeSpec.value(options);
        String version = versionSpec.value(options);

        log.info("Running mode: {}", mode);
        log.info("Generate Wiki Data, Minecraft Version: {}", version);
        if (notRun)
            log.info("Do not run jar, file will be generated in {}", InitializeEnvironment.REMAPPED_FOLDER);

        InitializeEnvironment.createDirectories(mode.isClient);
        Map<String, File> files = InitializeEnvironment.downloadBaseFiles(mode.isClient, version, forceReDownload);

        File remappedFile = new File(
            InitializeEnvironment.REMAPPED_FOLDER,
            version + (mode.isClient ? "-client" : "-server") + ".jar"
        );
        try {
            log.info("Remapping jar...");
            doRemap(files.get("jar"), files.get("mapping"), remappedFile, mode);
        } catch (Exception e) {
            log.error("Failed to remap jar!", e);
            System.exit(1);
        }

        if (!notRun) {
            if (mode.isClient) {
                InitializeEnvironment.downloadAssetsIndex(files.get("index"));
                InitializeEnvironment.downloadLibraries(files.get("manifest"));
                runWikiGeneratorClient(remappedFile.getAbsolutePath(), version, files.get("manifest"));
            } else {
                runWikiGeneratorServer(remappedFile.getAbsolutePath());
            }
        }
    }

    private static void doRemap(File input, File mapping, File output, GenWikiMode mode) throws Exception {
        MojangMapping format = new MojangMapping(new FileInputStream(mapping));
        RemapProgram program = new RemapProgram(format, input, output);
        RemapSettings.remapSettings(mode, program);
        if (mode.isClient) {
            program.setSourceJarFile(input);
            program.setRemappedFile(output);
        }
        if (!mode.isClient)
            program.extractServer();
        program.fillRemapFormat();
        program.remapClasses();
        if (!mode.isClient)
            program.rePackServer();
        log.info("Remapped jar to {}", output.getAbsolutePath());
    }

    private static ProcessBuilder newProcess() {
        String javaExec = System.getProperty("java.home") + "/bin/java";
        if (System.getProperty("os.name").startsWith("Windows"))
            javaExec += ".exe";

        ProcessBuilder builder;
        if (System.getenv("JVM_ARGS") != null) {
            String[] jvmArgs = System.getenv("JVM_ARGS").split(" ");
            List<String> list = new ArrayList<>();
            list.add(javaExec);
            list.addAll(List.of(jvmArgs));
            builder = new ProcessBuilder(list);
        } else
            builder = new ProcessBuilder(javaExec);

        return builder;
    }

    private static void runAndWait(ProcessBuilder processBuilder) throws Exception {
        log.info("Launch server with command: '{}'", String.join(" ", processBuilder.command()));

        Process process = processBuilder
            .directory(InitializeEnvironment.RUNTIME_FOLDER)
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

    private static void runWikiGeneratorServer(String file) throws Exception {
        try (FileWriter w = new FileWriter(new File(InitializeEnvironment.RUNTIME_FOLDER, "eula.txt"))) {
            w.write("eula=true");
        }
        try (FileWriter w = new FileWriter(new File(InitializeEnvironment.RUNTIME_FOLDER, "server.properties"))) {
            w.write("max-tick-time=-1\nsync-chunk-writes=false");
        }

        ProcessBuilder builder = newProcess();
        builder.command().add("-jar");
        builder.command().add(file);
        builder.command().add("-nogui");
        runAndWait(builder);
    }

    private static void runWikiGeneratorClient(String file, String version, File manifest) throws Exception {
        JsonObject json = JsonParser.parseReader(new FileReader(manifest)).getAsJsonObject();
        Set<String> collectedLibraries = new HashSet<>();

        String system = System.getProperty("os.name").toLowerCase();
        for (JsonElement library : json.getAsJsonArray("libraries")) {
            String path = JsonUtils.getStringInPath(library.getAsJsonObject(), "downloads.artifact.path").orElse("");
            if (library.getAsJsonObject().has("rules")) {
                boolean allowed = false;
                for (JsonElement rule : library.getAsJsonObject().getAsJsonArray("rules")) {
                    String action = JsonUtils.getStringOrElse(rule.getAsJsonObject(), "action", "");
                    String os = JsonUtils.getStringInPath(rule.getAsJsonObject(), "os.name").orElse("");
                    boolean matchOS = system.contains(os);
                    boolean actionFlip = action.equals("allow");
                    if (matchOS == actionFlip) {
                        allowed = true;
                        break;
                    }
                }
                if (!allowed)
                    continue;
            }
            if (path.contains("glfw") && path.contains("natives") && System.getenv("USE_SYSTEM_GLFW") != null) {
                log.info("Using system GLFW library! Skip {}", path);
                continue;
            }
            collectedLibraries.add(InitializeEnvironment.LIBRARIES_FOLDER.getAbsolutePath() + "/" + path);
        }

        collectedLibraries.add(file);

        IOUtils.copy(
            Objects.requireNonNull(GenerateWikiData.class.getResourceAsStream("/options.txt")),
            new FileOutputStream(new File(InitializeEnvironment.RUNTIME_FOLDER, "options.txt"))
        );

        String runtime = InitializeEnvironment.RUNTIME_FOLDER.getAbsolutePath();
        ProcessBuilder builder = newProcess();
        builder.command().add("-Dfile.encoding=UTF-8");
        builder.command().add("-Dstdout.encoding=UTF-8");
        builder.command().add("-Dstderr.encoding=UTF-8");
        builder.command().add("-Djava.library.path=" + runtime + "/native");
        builder.command().add("-Djna.tmpdir=" + runtime + "/native");
        builder.command().add("-Dorg.lwjgl.system.SharedLibraryExtractPath=" + runtime + "/native");
        builder.command().add("-Dio.netty.native.workdir=" + runtime + "/native");
        builder.command().add("-Dminecraft.client.jar=" + file);
        builder.command().add("-Dminecraft.launcher.brand=GenWiki");
        builder.command().add("-Dminecraft.launcher.version=1.0");
        builder.command().add("-cp");
        builder.command().add(String.join(File.pathSeparator, collectedLibraries));
        builder.command().add(JsonUtils.getStringOrElse(json, "mainClass", "net.minecraft.client.main.Main"));
        builder.command().add("--version");
        builder.command().add(version);
        builder.command().add("--assetsDir");
        builder.command().add(InitializeEnvironment.ASSETS_FOLDER.getAbsolutePath());
        builder.command().add("--assetIndex");
        builder.command().add(JsonUtils.getStringInPath(json, "assetIndex.id").orElse("legacy"));
        builder.command().add("--username");
        builder.command().add("Player");
        builder.command().add("--accessToken");
        builder.command().add("");
        builder.command().add("--gameDir");
        builder.command().add(InitializeEnvironment.RUNTIME_FOLDER.getAbsolutePath());
        runAndWait(builder);
    }
}
