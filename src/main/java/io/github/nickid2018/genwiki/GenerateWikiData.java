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
import java.util.Map;

@Slf4j
public class GenerateWikiData {

    @SneakyThrows
    public static void main(String[] args) {
        OptionParser parser = new OptionParser();
        NonOptionArgumentSpec<String> versionSpec = parser.nonOptions("Minecraft version");
        ArgumentAcceptingOptionSpec<GenWikiMode> modeSpec = parser
            .accepts("mode", "Generate chunk statistics")
            .withOptionalArg()
            .ofType(GenWikiMode.class)
            .defaultsTo(GenWikiMode.AUTOVALUE);
        OptionSpecBuilder doNotRun = parser.accepts("not-run", "Do not run server");
        OptionSpecBuilder doForceReDownload = parser.accepts("force-redownload", "Force re-download server jar and mapping");
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
        String version = versionSpec.value(options);
        GenWikiMode mode = modeSpec.value(options);

        log.info("Generate Wiki Data, Minecraft Version: {}", version);
        log.info("Running mode: {}", mode);
        if (notRun)
            log.info("Do not run jar, file will be generated in {}", InitializeEnvironment.REMAPPED_FOLDER);

        InitializeEnvironment.createDirectories(mode.isClient);
        Map<String, File> files = InitializeEnvironment.downloadBaseFiles(mode.isClient, version, forceReDownload);

        File remappedFile = new File(InitializeEnvironment.REMAPPED_FOLDER, version + (mode.isClient ? "-client" : "-server") + ".jar");
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
            }
            try {
                runWikiGenerator(remappedFile.getAbsolutePath());
            } catch (Exception e) {
                log.error("Failed to run wiki generator!", e);
                System.exit(1);
            }
        }
    }

    private static void doRemap(File input, File mapping, File output, GenWikiMode mode) throws Exception {
        MojangMapping format = new MojangMapping(new FileInputStream(mapping));
        RemapProgram program = new RemapProgram(format, input, output);
        RemapSettings.remapSettings(mode, program);
        if (mode.isClient) {
            program.setServerFile(input);
            program.setRemappedFile(output);
        }
        if (!mode.isClient)
            program.extractServer();
        program.fillRemapFormat();
        program.remapClasses();
        if (!mode.isClient)
            program.rePackServer();
        log.info("Remapped server jar to {}", output.getAbsolutePath());
    }

    private static void runWikiGenerator(String file) throws Exception {
        try (FileWriter w = new FileWriter(new File(InitializeEnvironment.RUNTIME_FOLDER, "eula.txt"))) {
            w.write("eula=true");
        }
        try (FileWriter w = new FileWriter(new File(InitializeEnvironment.RUNTIME_FOLDER, "server.properties"))) {
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
}
