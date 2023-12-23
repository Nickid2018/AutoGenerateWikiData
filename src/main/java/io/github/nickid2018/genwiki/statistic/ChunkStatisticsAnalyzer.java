package io.github.nickid2018.genwiki.statistic;

import io.github.nickid2018.genwiki.inject.InjectedProcess;
import io.github.nickid2018.genwiki.inject.SourceClass;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ChunkStatisticsAnalyzer {

    public static final int BATCH_SIZE;
    public static final int CHUNK_TOTAL;

    public static final Class<?> SERVER_TICK_RATE_MANAGER_CLASS;
    public static final Class<?> LEVEL_CLASS;
    public static final Class<?> SERVER_LEVEL_CLASS;
    public static final Class<?> SERVER_CHUNK_CACHE_CLASS;
    public static final Class<?> CHUNK_STATUS_CLASS;
    public static final Class<?> CHUNK_ACCESS_CLASS;
    public static final Class<?> BLOCK_POS_CLASS;
    public static final Class<?> LEVEL_READER_CLASS;
    public static final Class<?> EITHER_CLASS;
    public static final Class<?> BLOCK_STATE_BASE_CLASS;

    public static final Object CHUNK_STATUS_FULL;

    public static final MethodHandle TICK_RATE_MANAGER;
    public static final MethodHandle SET_FROZEN;
    public static final MethodHandle SET_TICK_RATE;
    public static final MethodHandle DIMENSION;
    public static final MethodHandle GET_ALL_LEVELS;
    public static final MethodHandle GET_CHUNK_SOURCE;
    public static final MethodHandle GET_CHUNK_FUTURE;
    public static final MethodHandle GET_BLOCK_STATE;
    public static final MethodHandle GET_MIN_BUILD_HEIGHT;
    public static final MethodHandle GET_HEIGHT;
    public static final MethodHandle BLOCK_POS_CONSTRUCTOR;
    public static final MethodHandle EITHER_LEFT;
    public static final MethodHandle GET_BLOCK;
    public static final MethodHandle GET_STATUS;

    static {
        try {
            String batchSizeStr = System.getenv("BATCH_SIZE");
            if (batchSizeStr != null)
                BATCH_SIZE = Integer.parseInt(batchSizeStr);
            else
                BATCH_SIZE = 4;
            System.out.println("Batch size: " + BATCH_SIZE);
            String chunkTotalStr = System.getenv("CHUNK_TOTAL");
            if (chunkTotalStr != null)
                CHUNK_TOTAL = Integer.parseInt(chunkTotalStr);
            else
                CHUNK_TOTAL = 25000;
            System.out.println("Chunk total: " + CHUNK_TOTAL);

            SERVER_TICK_RATE_MANAGER_CLASS = Class.forName("net.minecraft.server.ServerTickRateManager");
            LEVEL_CLASS = Class.forName("net.minecraft.world.level.Level");
            SERVER_LEVEL_CLASS = Class.forName("net.minecraft.server.level.ServerLevel");
            SERVER_CHUNK_CACHE_CLASS = Class.forName("net.minecraft.server.level.ServerChunkCache");
            CHUNK_STATUS_CLASS = Class.forName("net.minecraft.world.level.chunk.ChunkStatus");
            CHUNK_ACCESS_CLASS = Class.forName("net.minecraft.world.level.chunk.ChunkAccess");
            BLOCK_POS_CLASS = Class.forName("net.minecraft.core.BlockPos");
            LEVEL_READER_CLASS = Class.forName("net.minecraft.world.level.LevelReader");
            EITHER_CLASS = Class.forName("com.mojang.datafixers.util.Either");
            BLOCK_STATE_BASE_CLASS = Class.forName("net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase");

            CHUNK_STATUS_FULL = CHUNK_STATUS_CLASS.getField("FULL").get(null);

            MethodHandles.Lookup lookup = MethodHandles.lookup();
            TICK_RATE_MANAGER = lookup.unreflect(InjectedProcess.MINECRAFT_SERVER_CLASS.getMethod("tickRateManager"));
            SET_FROZEN = lookup.unreflect(SERVER_TICK_RATE_MANAGER_CLASS.getMethod("setFrozen", boolean.class));
            SET_TICK_RATE = lookup.unreflect(SERVER_TICK_RATE_MANAGER_CLASS.getMethod("setTickRate", float.class));
            DIMENSION = lookup.unreflect(LEVEL_CLASS.getMethod("dimension"));
            GET_ALL_LEVELS = lookup.unreflect(InjectedProcess.MINECRAFT_SERVER_CLASS.getMethod("getAllLevels"));
            GET_CHUNK_SOURCE = lookup.unreflect(SERVER_LEVEL_CLASS.getMethod("getChunkSource"));
            GET_CHUNK_FUTURE = lookup.unreflect(SERVER_CHUNK_CACHE_CLASS.getMethod("getChunkFuture",
                    int.class, int.class, CHUNK_STATUS_CLASS, boolean.class));
            GET_BLOCK_STATE = lookup.unreflect(CHUNK_ACCESS_CLASS.getMethod("getBlockState", BLOCK_POS_CLASS));
            GET_MIN_BUILD_HEIGHT = lookup.unreflect(LEVEL_READER_CLASS.getMethod("getMinBuildHeight"));
            GET_HEIGHT = lookup.unreflect(LEVEL_READER_CLASS.getMethod("getHeight"));
            BLOCK_POS_CONSTRUCTOR = lookup.unreflectConstructor(BLOCK_POS_CLASS.getConstructor(int.class, int.class, int.class));
            EITHER_LEFT = lookup.unreflect(EITHER_CLASS.getMethod("left"));
            GET_BLOCK = lookup.unreflect(BLOCK_STATE_BASE_CLASS.getMethod("getBlock"));
            GET_STATUS = lookup.unreflect(CHUNK_ACCESS_CLASS.getMethod("getStatus"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean initialized = false;
    @SourceClass("Iterable<ServerLevel>")
    private static List<?> levels;
    private static final Map<Object, ProgressBar> BAR_MAP = new HashMap<>();
    private static final Object2IntMap<Object> SUBMITTED_CHUNKS = new Object2IntArrayMap<>();
    private static final Map<Object, Set<CompletableFuture<?>>> FUTURES_MAP = new HashMap<>();
    private static final Map<Object, BlockCounter> BLOCK_COUNTER_MAP = new HashMap<>();
    private static final Map<Object, Thread> THREAD_MAP = new HashMap<>();
    private static final Map<Object, Queue<Object>> CREATED_CHUNKS = new HashMap<>();
    private static final Random RANDOM = new Random();

    @SneakyThrows
    public static void analyze(Object server) {
        if (!initialized) {
            Object tickRateManager = TICK_RATE_MANAGER.invoke(server);
            SET_TICK_RATE.invoke(tickRateManager, 1000000f);
            SET_FROZEN.invoke(tickRateManager, true);
            levels = StreamSupport.stream(((Iterable<?>) GET_ALL_LEVELS.invoke(server)).spliterator(), false).collect(Collectors.toList());
            for (Object level : levels) {
                Object dimension = DIMENSION.invoke(level);
                Object location = InjectedProcess.RESOURCE_KEY_LOCATION.invoke(dimension);
                String dimensionID = InjectedProcess.getResourceLocationPath(location);
                BAR_MAP.put(level, new ProgressBarBuilder().continuousUpdate().setStyle(ProgressBarStyle.ASCII)
                        .setInitialMax(CHUNK_TOTAL).setTaskName("Dimension " + dimensionID).build());
                FUTURES_MAP.put(level, new HashSet<>());
                SUBMITTED_CHUNKS.put(level, 0);
                CREATED_CHUNKS.put(level, new ConcurrentLinkedQueue<>());
                BLOCK_COUNTER_MAP.put(level, new BlockCounter());
                Thread thread = new Thread(() -> counterThread(BLOCK_COUNTER_MAP.get(level), level, CREATED_CHUNKS.get(level)));
                thread.setDaemon(true);
                THREAD_MAP.put(level, thread);
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

            ProgressBar bar = BAR_MAP.get(level);
            while (iterator.hasNext()) {
                CompletableFuture<?> future = iterator.next();
                if (future.isDone()) {
                    Optional<?> either = (Optional<?>) EITHER_LEFT.invoke(future.get());
                    createdChunk.offer(either.get());
                    iterator.remove();
                    bar.step();
                }
            }

            int submitted = SUBMITTED_CHUNKS.getInt(level);
            if (submitted < CHUNK_TOTAL) {
                for (int i = 0; i < BATCH_SIZE && submitted < CHUNK_TOTAL; i++) {
                    int x = RANDOM.nextInt(1000000) - 500000;
                    int z = RANDOM.nextInt(1000000) - 500000;
                    CompletableFuture<?> future = (CompletableFuture<?>) GET_CHUNK_FUTURE.invoke(chunkSource, x, z,
                            CHUNK_STATUS_FULL, true);
                    futures.add(future);
                    submitted++;
                }
                SUBMITTED_CHUNKS.put(level, submitted);
            }

            if (bar.getCurrent() >= CHUNK_TOTAL && submitted >= CHUNK_TOTAL) {
                BAR_MAP.remove(level).close();
                FUTURES_MAP.remove(level);
                SUBMITTED_CHUNKS.removeInt(level);
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
                BLOCK_COUNTER_MAP.remove(level);
                CREATED_CHUNKS.remove(level);
            }
        }

        if (levels.isEmpty() && THREAD_MAP.isEmpty())
            throw new RuntimeException("Program exited, chunk data has been written.");
    }

    @SneakyThrows
    private static void counterThread(BlockCounter counter, Object level, Queue<Object> createdChunk) {
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
            if (chunk == null)
                continue;
            count++;

            Object status = GET_STATUS.invoke(chunk);
            if (status != CHUNK_STATUS_FULL) {
                System.out.println("Chunk status is not features, is " + status + ", skipping.");
                continue;
            }

            for (Int2ObjectMap.Entry<List<?>> entry : blockPosMap.int2ObjectEntrySet()) {
                int y = entry.getIntKey();
                List<?> blockPosList = entry.getValue();
                for (Object blockPos : blockPosList) {
                    Object blockState = GET_BLOCK_STATE.invoke(chunk, blockPos);
                    Object block = GET_BLOCK.invoke(blockState);
                    counter.increase(block, y);
                }
            }
        }

        Object dimension = DIMENSION.invoke(level);
        Object location = InjectedProcess.RESOURCE_KEY_LOCATION.invoke(dimension);
        String dimensionID = InjectedProcess.getResourceLocationPath(location);
        counter.write(dimensionID, minBuildHeight, maxBuildHeight);
    }
}
