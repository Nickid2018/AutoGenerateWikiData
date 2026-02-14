package io.github.nickid2018.genwiki;

import com.google.gson.*;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import io.github.nickid2018.genwiki.remap.MojangMapping;
import io.github.nickid2018.genwiki.remap.RemapProgram;
import io.github.nickid2018.genwiki.util.ClassUtils;
import io.github.nickid2018.genwiki.util.ConfigUtils;
import io.github.nickid2018.genwiki.util.JsonUtils;
import io.github.nickid2018.genwiki.util.LanguageUtils;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import joptsimple.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
                InitializeEnvironment.downloadLibraries(files.get("manifest"));
                if (mode == GenWikiMode.ISO) {
                    InitializeEnvironment.downloadAssetsIndex(files.get("index"));
                    runWikiGeneratorClient(remappedFile.getAbsolutePath(), version, files.get("manifest"));
                } else {
                    runDataGeneratorClient(remappedFile.getAbsolutePath(), version, files.get("manifest"));
                }
            } else if (mode == GenWikiMode.STATISTICS) {
                doStatistics(remappedFile.getAbsolutePath());
            } else {
                runWikiGeneratorServer(remappedFile.getAbsolutePath());
            }
        }
    }

    private static void doRemap(File input, File mapping, File output, GenWikiMode mode) throws Exception {
        MojangMapping format = mapping != null ? new MojangMapping(new FileInputStream(mapping)) : new MojangMapping();
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
        program.validate(ClassUtils.readJarContent("remap/api.txt"), mode.isClient);
        if (!mode.isClient)
            program.rePackServer();
        log.info("Remapped jar to {}", output.getAbsolutePath());
    }

    private static String findJavaPath() {
        String javaExec = System.getProperty("java.home") + "/bin/java";
        if (System.getProperty("os.name").startsWith("Windows"))
            javaExec += ".exe";
        return javaExec;
    }

    private static ProcessBuilder newProcess() {
        String javaExec = findJavaPath();

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

    private static int runAndWait(ProcessBuilder processBuilder) throws Exception {
        log.info("Launch server with command: '{}'", String.join(" ", processBuilder.command()));

        Process process = processBuilder
                .directory(InitializeEnvironment.RUNTIME_FOLDER)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .start();

        Thread deathHook = new Thread(() -> {
            if (process.isAlive())
                process.destroy();
        });

        Runtime.getRuntime().addShutdownHook(deathHook);
        process.waitFor();
        Runtime.getRuntime().removeShutdownHook(deathHook);

        return process.exitValue();
    }

    private static void runWikiGeneratorServer(String file) throws Exception {
        runWikiGeneratorServer(file, InitializeEnvironment.RUNTIME_FOLDER);
    }

    private static void runWikiGeneratorServer(String file, File folder) throws Exception {
        try (FileWriter w = new FileWriter(new File(folder, "eula.txt"))) {
            w.write("eula=true");
        }
        try (FileWriter w = new FileWriter(new File(folder, "server.properties"))) {
            w.write("max-tick-time=-1\nsync-chunk-writes=false");
        }

        ProcessBuilder builder = newProcess();
        builder.command().add("--enable-native-access=ALL-UNNAMED");
        builder.command().add("-jar");
        builder.command().add(file);
        builder.command().add("nogui");
        runAndWait(builder);
    }

    private static Set<String> prepareLibraries(String file, File manifest) throws Exception {
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
        return collectedLibraries;
    }

    private static void runDataGeneratorClient(String file, String version, File manifest) throws Exception {
        Set<String> collectedLibraries = prepareLibraries(file, manifest);
        ProcessBuilder builder = newProcess();
        builder.command().add("-Dfile.encoding=UTF-8");
        builder.command().add("-Dstdout.encoding=UTF-8");
        builder.command().add("-Dstderr.encoding=UTF-8");
        builder.command().add("--enable-native-access=ALL-UNNAMED");
        builder.command().add("-cp");
        builder.command().add(String.join(File.pathSeparator, collectedLibraries));
        builder.command().add("net.minecraft.client.data.Main");
        builder.command().add("--client");
        builder.command().add("--output");
        builder.command().add(".");
        runAndWait(builder);
    }

    private static void runWikiGeneratorClient(String file, String version, File manifest) throws Exception {
        JsonObject json = JsonParser.parseReader(new FileReader(manifest)).getAsJsonObject();
        Set<String> collectedLibraries = prepareLibraries(file, manifest);

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
        builder.command().add("--enable-native-access=ALL-UNNAMED");
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

    @SneakyThrows
    private static void doStatistics(String file) {
        int totalChunks = ConfigUtils.envGetOrDefault("CHUNK_TOTAL", 100000);
        int worlds = ConfigUtils.envGetOrDefault("WORLD_TOTAL", 2);
        int async = ConfigUtils.envGetOrDefault("ASYNC_COUNT", 1);

        if (totalChunks % worlds != 0) {
            log.error("Chunks count cannot be divided by worlds count");
            System.exit(-1);
        }

        if (!InitializeEnvironment.RUNTIME_FOLDER.isDirectory())
            InitializeEnvironment.RUNTIME_FOLDER.mkdirs();
        File runtimeOutput = new File(InitializeEnvironment.RUNTIME_FOLDER, "output");
        if (!runtimeOutput.isDirectory())
            runtimeOutput.mkdirs();
        if (InitializeEnvironment.OUTPUT_FOLDER.isDirectory())
            FileUtils.deleteDirectory(InitializeEnvironment.OUTPUT_FOLDER);
        InitializeEnvironment.OUTPUT_FOLDER.mkdirs();

        int chunksSub = totalChunks / worlds;
        AtomicInteger lastWorlds = new AtomicInteger(worlds);
        AtomicInteger completedWorlds = new AtomicInteger(0);
        Queue<File> completedWorldQueue = new ConcurrentLinkedQueue<>();
        Thread[] threads = new Thread[async];
        for (int i = 0; i < async; i++) {
            int finalI = i;
            Thread worker = new Thread(LanguageUtils.sneakyExceptionRunnable(
                    () -> {
                        File subDir = new File(InitializeEnvironment.RUNTIME_FOLDER, "sub-" + finalI);
                        File outputLogFile = new File(InitializeEnvironment.RUNTIME_FOLDER, "sub-" + finalI + "output");
                        File outputErrorFile = new File(InitializeEnvironment.RUNTIME_FOLDER, "sub-" + finalI + "error");
                        FileUtils.deleteDirectory(subDir);

                        ProcessBuilder builder = newProcess()
                                .directory(subDir)
                                .redirectError(outputErrorFile)
                                .redirectOutput(outputLogFile);
                        builder.environment().put("CHUNK_TOTAL", String.valueOf(chunksSub));
                        builder.command().add("-jar");
                        builder.command().add(file);
                        builder.command().add("nogui");

                        log.info(
                                "Launch sub process with config: '{}' (ENV: {}, CWD: {})",
                                String.join(" ", builder.command()),
                                builder.environment(),
                                builder.directory()
                        );

                        while (lastWorlds.decrementAndGet() >= 0) {
                            subDir.mkdirs();
                            try (FileWriter w = new FileWriter(new File(subDir, "eula.txt"))) {
                                w.write("eula=true");
                            }
                            try (FileWriter w = new FileWriter(new File(subDir, "server.properties"))) {
                                w.write("""
                                        max-tick-time=-1
                                        sync-chunk-writes=false
                                        pause-when-empty-seconds=1000000000
                                        server-port=%d
                                        """.formatted(finalI + 25565));
                            }

                            Process process = builder.start();

                            Thread deathHook = new Thread(() -> {
                                if (process.isAlive())
                                    process.destroy();
                            });

                            Runtime.getRuntime().addShutdownHook(deathHook);
                            process.waitFor();
                            Runtime.getRuntime().removeShutdownHook(deathHook);

                            int exit = process.exitValue();
                            boolean hasCrashReport = new File(subDir, "crash-reports").isDirectory();
                            if (exit == 0 && !hasCrashReport) {
                                File subOutput = new File(
                                        InitializeEnvironment.OUTPUT_FOLDER,
                                        "sub-" + completedWorlds.incrementAndGet()
                                );
                                File[] files = subDir.listFiles(f -> f.getName().endsWith("_count.json"));
                                if (files == null)
                                    files = new File[0];
                                for (File output : files)
                                    FileUtils.moveFileToDirectory(output, subOutput, true);
                                completedWorldQueue.offer(subOutput);
                            } else if (exit != 0) {
                                log.warn("Async SubProcess #{} returns a non-zero exit value {}", finalI, exit);
                                lastWorlds.incrementAndGet();
                            } else {
                                log.warn("Async SubProcess #{} crashed!", finalI);
                                lastWorlds.incrementAndGet();
                                for (File crashFile : Objects.requireNonNull(new File(
                                        subDir,
                                        "crash-reports"
                                ).listFiles())) {
                                    FileUtils.moveFileToDirectory(crashFile, InitializeEnvironment.OUTPUT_FOLDER, true);
                                }
                            }

                            FileUtils.deleteDirectory(subDir);
                        }
                    },
                    t -> log.error("Async SubProcess #{} Error", finalI, t)
            ));
            worker.setName("SubProcess #" + i);
            worker.setDaemon(true);
            worker.start();
            threads[i] = worker;
        }

        Set<File> processedData = new HashSet<>();
        try (
                ProgressBar taskBar = ProgressBar
                        .builder()
                        .continuousUpdate()
                        .setTaskName("Sub Task")
                        .setInitialMax(worlds)
                        .showSpeed(new DecimalFormat("#.##"))
                        .build()
        ) {
            while (true) {
                boolean needUpdate = !completedWorldQueue.isEmpty();
                if (needUpdate) {
                    while (!completedWorldQueue.isEmpty())
                        processedData.add(completedWorldQueue.poll());
                }

                int aliveCount = 0;
                for (Thread t : threads) {
                    if (t.isAlive()) {
                        aliveCount++;
                    }
                }
                if (aliveCount == 0)
                    break;
                taskBar.stepTo(worlds - lastWorlds.get() - aliveCount);

                if (needUpdate) {
                    File baseDir = processedData.stream().findFirst().orElse(null);
                    if (baseDir == null)
                        continue;
                    String[] names = baseDir.list((dir, name) -> name.endsWith("_count.json"));
                    if (names == null)
                        continue;
                    for (String name : names)
                        doFileCollect(name, processedData);
                }

                Thread.sleep(1000);
            }
        }

        while (!completedWorldQueue.isEmpty())
            processedData.add(completedWorldQueue.poll());
        File baseDir = processedData.stream().findFirst().orElse(null);
        if (baseDir == null)
            return;
        String[] names = baseDir.list((dir, name) -> name.endsWith("_count.json"));
        if (names == null)
            return;
        for (String name : names)
            doFileCollect(name, processedData);
    }

    private static void doFileCollect(String name, Set<File> collectedFileLists) {
        try {
            Set<JsonObject> data = collectedFileLists
                    .stream()
                    .map(dir -> new File(dir, name))
                    .filter(File::exists)
                    .map(LanguageUtils.exceptionOrElse(
                            file -> JsonParser.parseReader(new FileReader(file)),
                            (file, e) -> {
                                log.warn("Cannot read {}", file, e);
                                return null;
                            }
                    ))
                    .filter(Objects::nonNull)
                    .map(JsonElement::getAsJsonObject)
                    .collect(Collectors.toSet());
            JsonArray metadata = new JsonArray();
            String dataName = name.replaceAll(".+_(.+)_count.json", "$1");
            long totalChunkCount = data.stream().map(o -> o.get("chunkCount")).mapToLong(JsonElement::getAsLong).sum();
            Set<Long> minHeightSet = data
                    .stream()
                    .map(o -> o.get("minHeight"))
                    .map(JsonElement::getAsLong)
                    .collect(Collectors.toSet());
            Set<Long> maxHeightSet = data
                    .stream()
                    .map(o -> o.get("maxHeight"))
                    .map(JsonElement::getAsLong)
                    .collect(Collectors.toSet());
            if (minHeightSet.size() != 1 || maxHeightSet.size() != 1) {
                log.error("Height data mismatches!");
                return;
            }
            long minHeight = minHeightSet.stream().findFirst().orElse(0L);
            long maxHeight = maxHeightSet.stream().findFirst().orElse(0L);
            Object2ObjectMap<String, LongList> counter = new Object2ObjectOpenHashMap<>();
            data.stream()
                    .map(o -> o.getAsJsonObject(dataName))
                    .map(JsonObject::entrySet)
                    .forEach(entries -> entries.forEach(entry -> {
                        LongList list = counter.computeIfAbsent(entry.getKey(), s -> new LongArrayList());
                        JsonArray array = entry.getValue().getAsJsonArray();
                        if (list.isEmpty()) {
                            array.forEach(e -> list.add(e.getAsLong()));
                        } else {
                            if (list.size() != array.size()) {
                                log.warn("Length mismatch!");
                            } else {
                                for (int i = 0; i < list.size(); i++) {
                                    list.set(i, array.get(i).getAsLong() + list.getLong(i));
                                }
                            }
                        }
                    }));
            data.forEach(o -> {
                o.remove(dataName);
                o.remove("minHeight");
                o.remove("maxHeight");
                metadata.add(o);
            });
            StringWriter sw = new StringWriter();
            JsonWriter jw = new JsonWriter(sw);
            jw.setIndent("  ");
            Streams.write(metadata, jw);
            String metadataJson = Arrays
                    .stream(sw.toString().split("\n"))
                    .skip(1)
                    .map(s -> "  " + s)
                    .collect(Collectors.joining("\n"));
            String countsJson = counter
                    .entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> "    \"%s\": [%s]"
                            .formatted(
                                    entry.getKey(),
                                    entry
                                            .getValue()
                                            .longStream()
                                            .mapToObj(Long::toString)
                                            .collect(Collectors.joining(", "))
                            )
                    ).collect(Collectors.joining(",\n"));
            String output = """
                    {
                      "chunkCount": %d,
                      "minHeight": %d,
                      "maxHeight": %d,
                      "metadata": [
                    %s,
                      "%s": {
                    %s
                      }
                    }
                    """.formatted(totalChunkCount, minHeight, maxHeight, metadataJson, dataName, countsJson);
            try (FileWriter writer = new FileWriter(new File(InitializeEnvironment.OUTPUT_FOLDER, name))) {
                writer.write(output);
            }
        } catch (Exception e) {
            log.error("Cannot collect files", e);
        }
    }
}
