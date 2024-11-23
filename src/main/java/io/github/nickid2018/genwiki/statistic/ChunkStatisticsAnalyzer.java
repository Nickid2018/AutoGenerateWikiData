package io.github.nickid2018.genwiki.statistic;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public class ChunkStatisticsAnalyzer {

    public static final StatisticsSettings SETTINGS = StatisticsSettings.getInstance();

    private static boolean initialized = false;
    private static List<ServerLevel> levels;
    private static final Map<Object, String> LEVEL_NAME = new HashMap<>();
    private static final Map<Object, ProgressBar> BAR_MAP = new HashMap<>();
    private static final Map<Object, Set<CompletableFuture<ChunkResult<ChunkAccess>>>> FUTURES_MAP = new HashMap<>();
    private static final Map<Object, Thread> THREAD_MAP = new HashMap<>();
    private static final Map<Object, Queue<ChunkAccess>> CREATED_CHUNKS = new HashMap<>();
    private static final Map<Object, ChunkPosProvider> CHUNK_POS_PROVIDER = new HashMap<>();
    private static final Set<Object> NEXT_FLIP_NO_SAVE = new HashSet<>();

    @SneakyThrows
    public static void analyze(MinecraftServer server) {
        if (!initialized) {
            ServerTickRateManager tickRateManager = server.tickRateManager();
            tickRateManager.setFrozen(true);
            tickRateManager.setTickRate(1000000f);

            // REDIRECT STDOUT AND STDERR BACK TO DEFAULT
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
            System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));

            levels = StreamSupport.stream(server.getAllLevels().spliterator(), false).collect(Collectors.toList());
            Iterator<ServerLevel> levelIterator = levels.iterator();
            while (levelIterator.hasNext()) {
                ServerLevel level = levelIterator.next();
                String dimensionID = level.dimension().location().getPath();
                if (SETTINGS.getDimensions() != null && !SETTINGS.getDimensions().contains(dimensionID)) {
                    levelIterator.remove();
                    continue;
                }
                LEVEL_NAME.put(level, dimensionID);
                level.noSave = true;

                BAR_MAP.put(
                    level,
                    new ProgressBarBuilder()
                        .continuousUpdate()
                        .setStyle(ProgressBarStyle.ASCII)
                        .setInitialMax(SETTINGS.getChunkTotal())
                        .setTaskName("Dimension " + dimensionID)
                        .build()
                );
                FUTURES_MAP.put(level, new HashSet<>());
                CHUNK_POS_PROVIDER.put(level, SETTINGS.getChunkPosProvider(dimensionID));
                CREATED_CHUNKS.put(level, new ConcurrentLinkedQueue<>());

                Thread thread = new Thread(() -> counterThread(level, CREATED_CHUNKS.get(level)));
                THREAD_MAP.put(level, thread);
                thread.setDaemon(true);
                thread.start();
            }

            initialized = true;
        }

        Iterator<ServerLevel> levelIterator = levels.iterator();
        while (levelIterator.hasNext()) {
            ServerLevel level = levelIterator.next();

            ServerChunkCache chunkSource = level.getChunkSource();
            Set<CompletableFuture<ChunkResult<ChunkAccess>>> futures = FUTURES_MAP.get(level);
            Iterator<CompletableFuture<ChunkResult<ChunkAccess>>> iterator = futures.iterator();
            Queue<ChunkAccess> createdChunk = CREATED_CHUNKS.get(level);
            ChunkPosProvider chunkPosProvider = CHUNK_POS_PROVIDER.get(level);

            ProgressBar bar = BAR_MAP.get(level);
            while (iterator.hasNext()) {
                CompletableFuture<ChunkResult<ChunkAccess>> future = iterator.next();
                if (future.isDone()) {
                    ChunkAccess chunk = future.get().orElse(null);
                    createdChunk.offer(chunk);
                    iterator.remove();

                    ChunkPos chunkPos = chunk.getPos();
                    long chunkPosLong = chunkPos.toLong();
                    chunkSource.distanceManager.getTickets(chunkPosLong).forEach(
                        ticket -> chunkSource.distanceManager.removeTicket(chunkPosLong, ticket)
                    );
                    bar.step();
                }
            }

            if (NEXT_FLIP_NO_SAVE.contains(level)) {
                level.noSave = true;
                NEXT_FLIP_NO_SAVE.remove(level);
            }

            for (int i = 0; i < SETTINGS.getBatchSize() && chunkPosProvider.hasNext() && futures.size() < SETTINGS.getBatchSize() * 40; i++) {
                chunkPosProvider.next(
                    (x, z) -> futures.add(chunkSource.getChunkFuture(x, z, ChunkStatus.FEATURES, true))
                );
                if (chunkPosProvider.nowUnload()) {
                    NEXT_FLIP_NO_SAVE.add(level);
                    level.noSave = false;
                    log.trace("Unloading chunks in dimension {}...", LEVEL_NAME.get(level));
                }
            }

            if (bar.getCurrent() >= SETTINGS.getChunkTotal() && !chunkPosProvider.hasNext()) {
                BAR_MAP.remove(level).close();
                FUTURES_MAP.remove(level);
                CHUNK_POS_PROVIDER.remove(level);
                level.noSave = false;
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
    private static void counterThread(ServerLevel level, Queue<ChunkAccess> createdChunk) {
        String dimensionID = LEVEL_NAME.get(level);
        long worldSeed = level.getSeed();
        ChunkPosProvider chunkPosProvider = CHUNK_POS_PROVIDER.get(level);

        DataCounter<Block> blockCounter = new DataCounter<>(
            "block",
            block -> BuiltInRegistries.BLOCK.getKey(block).getPath()
        );
        DataCounter<Holder<Biome>> biomeCounter = new DataCounter<>(
            "biome",
            b -> b.unwrapKey().orElseThrow().location().getPath()
        );

        int count = 0;
        Int2ObjectMap<List<BlockPos>> blockPosMap = new Int2ObjectArrayMap<>();
        int minBuildHeight = level.getMinY();
        int maxBuildHeight = level.getHeight() + minBuildHeight;
        for (int y = minBuildHeight; y < maxBuildHeight; y++) {
            List<BlockPos> blockPosList = new ArrayList<>();
            blockPosMap.put(y, blockPosList);
            for (int x = 0; x < 16; x++)
                for (int z = 0; z < 16; z++)
                    blockPosList.add(new BlockPos(x, y, z));
        }

        while (count < SETTINGS.getChunkTotal()) {
            ChunkAccess chunk = createdChunk.poll();
            if (chunk == null) {
                Thread.sleep(1);
                continue;
            }

            for (Int2ObjectMap.Entry<List<BlockPos>> entry : blockPosMap.int2ObjectEntrySet()) {
                int y = entry.getIntKey();
                List<BlockPos> blockPosList = entry.getValue();
                for (BlockPos blockPos : blockPosList) {
                    BlockState blockState = chunk.getBlockState(blockPos);
                    Block block = blockState.getBlock();
                    blockCounter.increase(block, y);
                }
            }

            for (int y = minBuildHeight / 4; y < maxBuildHeight / 4; y++)
                for (int x = 0; x < 4; x++)
                    for (int z = 0; z < 4; z++) {
                        Holder<Biome> holder = chunk.getNoiseBiome(x, y, z);
                        biomeCounter.increase(holder, y);
                    }

            count++;
            if (count % SETTINGS.getSaveInterval() == 0) {
                blockCounter.write(worldSeed, dimensionID, chunkPosProvider, minBuildHeight, maxBuildHeight, count);
                biomeCounter.write(
                    worldSeed,
                    dimensionID,
                    chunkPosProvider,
                    minBuildHeight / 4,
                    maxBuildHeight / 4,
                    count
                );
            }
        }

        blockCounter.write(worldSeed, dimensionID, chunkPosProvider, minBuildHeight, maxBuildHeight, count);
        biomeCounter.write(worldSeed, dimensionID, chunkPosProvider, minBuildHeight / 4, maxBuildHeight / 4, count);
    }
}
