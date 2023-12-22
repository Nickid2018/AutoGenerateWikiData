package io.github.nickid2018.genwiki.statistic;

import io.github.nickid2018.genwiki.inject.InjectedProcess;
import io.github.nickid2018.genwiki.inject.SourceClass;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.SneakyThrows;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ChunkStatisticsAnalyzer {

    public static final Class<?> SERVER_TICK_RATE_MANAGER_CLASS;
    public static final Class<?> LEVEL_CLASS;
    public static final Class<?> SERVER_LEVEL_CLASS;
    public static final Class<?> SERVER_CHUNK_CACHE_CLASS;
    public static final Class<?> CHUNK_STATUS_CLASS;

    public static final Object CHUNK_STATUS_FEATURES;

    public static final MethodHandle TICK_RATE_MANAGER;
    public static final MethodHandle SET_FROZEN;
    public static final MethodHandle DIMENSION;
    public static final MethodHandle GET_ALL_LEVELS;
    public static final MethodHandle GET_CHUNK_SOURCE;
    public static final MethodHandle GET_CHUNK_FUTURE;

    static {
        try {
            SERVER_TICK_RATE_MANAGER_CLASS = Class.forName("net.minecraft.server.ServerTickRateManager");
            LEVEL_CLASS = Class.forName("net.minecraft.world.level.Level");
            SERVER_LEVEL_CLASS = Class.forName("net.minecraft.server.level.ServerLevel");
            SERVER_CHUNK_CACHE_CLASS = Class.forName("net.minecraft.server.level.ServerChunkCache");
            CHUNK_STATUS_CLASS = Class.forName("net.minecraft.world.level.chunk.ChunkStatus");

            CHUNK_STATUS_FEATURES = CHUNK_STATUS_CLASS.getField("FEATURES").get(null);

            MethodHandles.Lookup lookup = MethodHandles.lookup();
            TICK_RATE_MANAGER = lookup.unreflect(InjectedProcess.MINECRAFT_SERVER_CLASS.getMethod("tickRateManager"));
            SET_FROZEN = lookup.unreflect(SERVER_TICK_RATE_MANAGER_CLASS.getMethod("setFrozen", boolean.class));
            DIMENSION = lookup.unreflect(LEVEL_CLASS.getMethod("dimension"));
            GET_ALL_LEVELS = lookup.unreflect(InjectedProcess.MINECRAFT_SERVER_CLASS.getMethod("getAllLevels"));
            GET_CHUNK_SOURCE = lookup.unreflect(SERVER_LEVEL_CLASS.getMethod("getChunkSource"));
            GET_CHUNK_FUTURE = lookup.unreflect(SERVER_CHUNK_CACHE_CLASS.getMethod("getChunkFuture",
                    int.class, int.class, CHUNK_STATUS_CLASS, boolean.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean initialized = false;
    @SourceClass("Iterable<ServerLevel>")
    private static List<?> levels;
    private static Map<Object, ProgressBar> barMap = new HashMap<>();
    private static Object2IntMap<Object> submittedChunks = new Object2IntArrayMap<>();
    private static Map<Object, Set<CompletableFuture<?>>> futuresMap = new HashMap<>();
    private static Random random = new Random();

    @SneakyThrows
    public static void analyze(Object server) {
        if (!initialized) {
            Object tickRateManager = TICK_RATE_MANAGER.invoke(server);
            SET_FROZEN.invoke(tickRateManager, true);
            levels = StreamSupport.stream(((Iterable<?>) GET_ALL_LEVELS.invoke(server)).spliterator(), false).collect(Collectors.toList());
            for (Object level : levels) {
                Object dimension = DIMENSION.invoke(level);
                Object location = InjectedProcess.RESOURCE_KEY_LOCATION.invoke(dimension);
                String dimensionID = InjectedProcess.getResourceLocationPath(location);
                barMap.put(level, new ProgressBarBuilder()
                        .setStyle(ProgressBarStyle.ASCII).setInitialMax(25000).setTaskName("Dimension " + dimensionID).build());
                futuresMap.put(level, new HashSet<>());
                submittedChunks.put(level, 0);
            }
            initialized = true;
        }

        Iterator<?> levelIterator = levels.iterator();
        while (levelIterator.hasNext()) {
            Object level = levelIterator.next();

            @SourceClass("ServerChunkCache")
            Object chunkSource = GET_CHUNK_SOURCE.invoke(level);
            Set<CompletableFuture<?>> futures = futuresMap.get(level);
            Iterator<CompletableFuture<?>> iterator = futures.iterator();

            ProgressBar bar = barMap.get(level);
            while (iterator.hasNext()) {
                CompletableFuture<?> future = iterator.next();
                if (future.isDone()) {
                    if (submittedChunks.getInt(level) % 1000 == 0)
                        System.out.println(future);
                    iterator.remove();
                    bar.step();
                }
            }

            int submitted = submittedChunks.getInt(level);
            if (submitted < 25000) {
                for (int i = 0; i < 100; i++) {
                    int x = random.nextInt(1000000) - 500000;
                    int z = random.nextInt(1000000) - 500000;
                    CompletableFuture<?> future = (CompletableFuture<?>) GET_CHUNK_FUTURE.invoke(chunkSource, x, z,
                            CHUNK_STATUS_FEATURES, true);
                    futures.add(future);
                    submitted++;
                }
                submittedChunks.put(level, submitted);
            }

            if (bar.getCurrent() >= 25000 && submitted >= 25000) {
                barMap.remove(level).close();
                futuresMap.remove(level);
                submittedChunks.removeInt(level);
                levelIterator.remove();
            }
        }

        if (levels.isEmpty())
            throw new RuntimeException("Program exited, chunk data has been written.");
    }
}
