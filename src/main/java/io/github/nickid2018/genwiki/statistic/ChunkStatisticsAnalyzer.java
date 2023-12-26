package io.github.nickid2018.genwiki.statistic;

import io.github.nickid2018.genwiki.inject.InjectedProcess;
import io.github.nickid2018.genwiki.inject.SourceClass;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public class ChunkStatisticsAnalyzer {

    public static final int BATCH_SIZE;
    public static final int CHUNK_TOTAL;
    public static final int BLOCK_SIZE;
    public static final ChunkPosProvider.ChunkPosProviderProvider CHUNK_POS_PROVIDER_FACTORY;

    public static final Class<?> SERVER_TICK_RATE_MANAGER_CLASS;
    public static final Class<?> LEVEL_CLASS;
    public static final Class<?> SERVER_LEVEL_CLASS;
    public static final Class<?> SERVER_CHUNK_CACHE_CLASS;
    public static final Class<?> CHUNK_STATUS_CLASS;
    public static final Class<?> CHUNK_ACCESS_CLASS;
    public static final Class<?> BLOCK_POS_CLASS;
    public static final Class<?> LEVEL_READER_CLASS;
    public static final Class<?> BLOCK_STATE_BASE_CLASS;

    public static final Object CHUNK_STATUS_FEATURES;

    public static final MethodHandle TICK_RATE_MANAGER;
    public static final MethodHandle SET_FROZEN;
    public static final MethodHandle DIMENSION;
    public static final MethodHandle GET_ALL_LEVELS;
    public static final MethodHandle GET_CHUNK_SOURCE;
    public static final MethodHandle GET_CHUNK_FUTURE;
    public static final MethodHandle GET_BLOCK_STATE;
    public static final MethodHandle GET_MIN_BUILD_HEIGHT;
    public static final MethodHandle GET_HEIGHT;
    public static final MethodHandle BLOCK_POS_CONSTRUCTOR;
    public static final MethodHandle GET_BLOCK;
    public static final MethodHandle GET_STATUS;
    public static final MethodHandle GET_NOISE_BIOME;
    public static final MethodHandle GET_SEED;

    public static final VarHandle NO_SAVE;

    static {
        try {
            String batchSizeStr = System.getenv("BATCH_SIZE");
            if (batchSizeStr != null)
                BATCH_SIZE = Integer.parseInt(batchSizeStr);
            else
                BATCH_SIZE = 4;
            String chunkTotalStr = System.getenv("CHUNK_TOTAL");
            if (chunkTotalStr != null)
                CHUNK_TOTAL = Integer.parseInt(chunkTotalStr);
            else
                CHUNK_TOTAL = 25000;
            String blockSize = System.getenv("BLOCK_SIZE");
            if (blockSize != null)
                BLOCK_SIZE = Integer.parseInt(blockSize);
            else
                BLOCK_SIZE = 1089;

            String chunkPosProviderFactoryStr = System.getenv("CHUNK_POS_PROVIDER_FACTORY");
            if (chunkPosProviderFactoryStr != null)
                switch (chunkPosProviderFactoryStr) {
                    case "continuous":
                        CHUNK_POS_PROVIDER_FACTORY = ContinuousChunkPosProvider::new;
                        break;
                    case "random":
                        CHUNK_POS_PROVIDER_FACTORY = RandomChunkPosProvider::new;
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown chunk pos provider factory: " + chunkPosProviderFactoryStr);
                }
            else {
                CHUNK_POS_PROVIDER_FACTORY = ContinuousChunkPosProvider::new;
                chunkPosProviderFactoryStr = "continuous";
            }

            log.info("Batch size: {}", BATCH_SIZE);
            log.info("Chunk total: {}", CHUNK_TOTAL);
            log.info("Block size: {}", BLOCK_SIZE);
            log.info("Chunk pos provider factory: {}", chunkPosProviderFactoryStr);

            SERVER_TICK_RATE_MANAGER_CLASS = Class.forName("net.minecraft.server.ServerTickRateManager");
            LEVEL_CLASS = Class.forName("net.minecraft.world.level.Level");
            SERVER_LEVEL_CLASS = Class.forName("net.minecraft.server.level.ServerLevel");
            SERVER_CHUNK_CACHE_CLASS = Class.forName("net.minecraft.server.level.ServerChunkCache");
            CHUNK_STATUS_CLASS = Class.forName("net.minecraft.world.level.chunk.ChunkStatus");
            CHUNK_ACCESS_CLASS = Class.forName("net.minecraft.world.level.chunk.ChunkAccess");
            BLOCK_POS_CLASS = Class.forName("net.minecraft.core.BlockPos");
            LEVEL_READER_CLASS = Class.forName("net.minecraft.world.level.LevelReader");
            BLOCK_STATE_BASE_CLASS = Class.forName("net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase");

            CHUNK_STATUS_FEATURES = CHUNK_STATUS_CLASS.getField("FEATURES").get(null);

            MethodHandles.Lookup lookup = MethodHandles.lookup();
            TICK_RATE_MANAGER = lookup.unreflect(InjectedProcess.MINECRAFT_SERVER_CLASS.getMethod("tickRateManager"));
            SET_FROZEN = lookup.unreflect(SERVER_TICK_RATE_MANAGER_CLASS.getMethod("setFrozen", boolean.class));
            DIMENSION = lookup.unreflect(LEVEL_CLASS.getMethod("dimension"));
            GET_ALL_LEVELS = lookup.unreflect(InjectedProcess.MINECRAFT_SERVER_CLASS.getMethod("getAllLevels"));
            GET_CHUNK_SOURCE = lookup.unreflect(SERVER_LEVEL_CLASS.getMethod("getChunkSource"));
            GET_CHUNK_FUTURE = lookup.unreflect(SERVER_CHUNK_CACHE_CLASS.getMethod("getChunkFuture",
                    int.class, int.class, CHUNK_STATUS_CLASS, boolean.class));
            GET_BLOCK_STATE = lookup.unreflect(CHUNK_ACCESS_CLASS.getMethod("getBlockState", BLOCK_POS_CLASS));
            GET_MIN_BUILD_HEIGHT = lookup.unreflect(LEVEL_READER_CLASS.getMethod("getMinBuildHeight"));
            GET_HEIGHT = lookup.unreflect(LEVEL_READER_CLASS.getMethod("getHeight"));
            BLOCK_POS_CONSTRUCTOR = lookup.unreflectConstructor(BLOCK_POS_CLASS.getConstructor(int.class, int.class, int.class));
            GET_BLOCK = lookup.unreflect(BLOCK_STATE_BASE_CLASS.getMethod("getBlock"));
            GET_STATUS = lookup.unreflect(CHUNK_ACCESS_CLASS.getMethod("getStatus"));
            GET_NOISE_BIOME = lookup.unreflect(CHUNK_ACCESS_CLASS.getMethod("getNoiseBiome", int.class, int.class, int.class));
            GET_SEED = lookup.unreflect(SERVER_LEVEL_CLASS.getMethod("getSeed"));

            NO_SAVE = lookup.findVarHandle(SERVER_LEVEL_CLASS, "noSave", boolean.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean initialized = false;
    @SourceClass("Iterable<ServerLevel>")
    private static List<?> levels;
    private static final Map<Object, String> LEVEL_NAME = new HashMap<>();
    private static final Map<Object, ProgressBar> BAR_MAP = new HashMap<>();
    private static final Map<Object, Set<CompletableFuture<?>>> FUTURES_MAP = new HashMap<>();
    private static final Map<Object, Thread> THREAD_MAP = new HashMap<>();
    private static final Map<Object, Queue<Object>> CREATED_CHUNKS = new HashMap<>();
    private static final Map<Object, ChunkPosProvider> CHUNK_POS_PROVIDER = new HashMap<>();
    private static final Set<Object> NEXT_FLIP_NO_SAVE = new HashSet<>();

    @SneakyThrows
    public static void analyze(Object server) {
        if (!initialized) {
            Object tickRateManager = TICK_RATE_MANAGER.invoke(server);
            SET_FROZEN.invoke(tickRateManager, true);

            // REDIRECT STDOUT AND STDERR BACK TO DEFAULT
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
            System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));

            levels = StreamSupport.stream(((Iterable<?>) GET_ALL_LEVELS.invoke(server)).spliterator(), false).collect(Collectors.toList());
            for (Object level : levels) {
                Object dimension = DIMENSION.invoke(level);
                Object location = InjectedProcess.RESOURCE_KEY_LOCATION.invoke(dimension);
                String dimensionID = InjectedProcess.getResourceLocationPath(location);
                LEVEL_NAME.put(level, dimensionID);
                NO_SAVE.set(level, true);

                BAR_MAP.put(level, new ProgressBarBuilder().continuousUpdate().setStyle(ProgressBarStyle.ASCII)
                        .setInitialMax(CHUNK_TOTAL).setTaskName("Dimension " + dimensionID).build());
                FUTURES_MAP.put(level, new HashSet<>());
                CHUNK_POS_PROVIDER.put(level, CHUNK_POS_PROVIDER_FACTORY.accept(CHUNK_TOTAL, BLOCK_SIZE));
                CREATED_CHUNKS.put(level, new ConcurrentLinkedQueue<>());

                Thread thread = new Thread(() -> counterThread(level, CREATED_CHUNKS.get(level)));
                THREAD_MAP.put(level, thread);
                thread.setDaemon(true);
                thread.start();
            }

            initialized = true;
        }

        Iterator<?> levelIterator = levels.iterator();
        while (levelIterator.hasNext()) {
            Object level = levelIterator.next();

            @SourceClass("ServerChunkCache")
            Object chunkSource = GET_CHUNK_SOURCE.invoke(level);
            Set<CompletableFuture<?>> futures = FUTURES_MAP.get(level);
            Iterator<CompletableFuture<?>> iterator = futures.iterator();
            Queue<Object> createdChunk = CREATED_CHUNKS.get(level);
            ChunkPosProvider chunkPosProvider = CHUNK_POS_PROVIDER.get(level);

            ProgressBar bar = BAR_MAP.get(level);
            while (iterator.hasNext()) {
                CompletableFuture<?> future = iterator.next();
                if (future.isDone()) {
                    Optional<?> either = (Optional<?>) InjectedProcess.EITHER_LEFT.invoke(future.get());
                    createdChunk.offer(either.get());
                    iterator.remove();
                    bar.step();
                }
            }

            if (NEXT_FLIP_NO_SAVE.contains(level)) {
                NO_SAVE.set(level, true);
                NEXT_FLIP_NO_SAVE.remove(level);
            }

            for (int i = 0; i < BATCH_SIZE && chunkPosProvider.hasNext() && futures.size() < BATCH_SIZE * 40; i++) {
                chunkPosProvider.next((x, z) ->
                        futures.add((CompletableFuture<?>) GET_CHUNK_FUTURE.invoke(chunkSource, x, z,
                                CHUNK_STATUS_FEATURES, true))
                );
                if (chunkPosProvider.nowUnload()) {
                    NEXT_FLIP_NO_SAVE.add(level);
                    NO_SAVE.set(level, false);
                    log.info("Unloading chunks in dimension {}...", LEVEL_NAME.get(level));
                }
            }

            if (bar.getCurrent() >= CHUNK_TOTAL && !chunkPosProvider.hasNext()) {
                BAR_MAP.remove(level).close();
                FUTURES_MAP.remove(level);
                CHUNK_POS_PROVIDER.remove(level);
                NO_SAVE.set(level, false);
                levelIterator.remove();
            }
        }

        Iterator<Map.Entry<Object, Thread>> threadIterator = THREAD_MAP.entrySet().iterator();
        while (threadIterator.hasNext()) {
            Map.Entry<Object, Thread> entry = threadIterator.next();
            Object level = entry.getKey();
            Thread thread = entry.getValue();
            if (!thread.isAlive()) {
                threadIterator.remove();
                CREATED_CHUNKS.remove(level);
            }
        }

        if (levels.isEmpty() && THREAD_MAP.isEmpty()) {
            log.info("All done!");
            log.info("Statistic Data has been stored in 'run/runtime/*_count.json'.");
            log.info("Program will halt with exit code 0.");
            Runtime.getRuntime().halt(0); // DO NOT RUN ANY SHUTDOWN HOOKS
        }
    }

    @SneakyThrows
    private static void counterThread(Object level, Queue<Object> createdChunk) {
        String dimensionID = LEVEL_NAME.get(level);
        long worldSeed = (long) GET_SEED.invoke(level);
        ChunkPosProvider chunkPosProvider = CHUNK_POS_PROVIDER.get(level);
        Object registryBlock = InjectedProcess.getRegistry("BLOCK");
        DataCounter blockCounter = new DataCounter("block", block -> InjectedProcess.getObjectPathWithRegistry(registryBlock, block));
        DataCounter biomeCounter = new DataCounter("biome", InjectedProcess::holderToString);

        int count = 0;
        Int2ObjectMap<List<?>> blockPosMap = new Int2ObjectArrayMap<>();
        int minBuildHeight = (int) GET_MIN_BUILD_HEIGHT.invoke(level);
        int maxBuildHeight = (int) GET_HEIGHT.invoke(level) + minBuildHeight;
        for (int y = minBuildHeight; y < maxBuildHeight; y++) {
            List<Object> blockPosList = new ArrayList<>();
            blockPosMap.put(y, blockPosList);
            for (int x = 0; x < 16; x++)
                for (int z = 0; z < 16; z++)
                    blockPosList.add(BLOCK_POS_CONSTRUCTOR.invoke(x, y, z));
        }

        while (count < CHUNK_TOTAL) {
            Object chunk = createdChunk.poll();
            if (chunk == null) {
                Thread.sleep(1);
                continue;
            }

            for (Int2ObjectMap.Entry<List<?>> entry : blockPosMap.int2ObjectEntrySet()) {
                int y = entry.getIntKey();
                List<?> blockPosList = entry.getValue();
                for (Object blockPos : blockPosList) {
                    Object blockState = GET_BLOCK_STATE.invoke(chunk, blockPos);
                    Object block = GET_BLOCK.invoke(blockState);
                    blockCounter.increase(block, y);
                }
            }

            for (int y = minBuildHeight / 4; y < maxBuildHeight / 4; y++)
                for (int x = 0; x < 4; x++)
                    for (int z = 0; z < 4; z++) {
                        Object holder = GET_NOISE_BIOME.invoke(chunk, x, y, z);
                        biomeCounter.increase(holder, y);
                    }

            if (count % 10000 == 0) {
                blockCounter.write(worldSeed, dimensionID, chunkPosProvider, minBuildHeight, maxBuildHeight);
                biomeCounter.write(worldSeed, dimensionID, chunkPosProvider, minBuildHeight / 4, maxBuildHeight / 4);
            }
            count++;
        }

        blockCounter.write(worldSeed, dimensionID, chunkPosProvider, minBuildHeight, maxBuildHeight);
        biomeCounter.write(worldSeed, dimensionID, chunkPosProvider, minBuildHeight / 4, maxBuildHeight / 4);
    }
}
