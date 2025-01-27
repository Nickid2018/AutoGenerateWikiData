package io.github.nickid2018.genwiki.autovalue;

import com.google.common.collect.ImmutableList;
import io.github.nickid2018.genwiki.autovalue.wikidata.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
public class BlockDataExtractor {
    private static final NumberWikiData EXPLOSION_RESISTANCE = new NumberWikiData();
    private static final NumberWikiData DESTROY_TIME = new NumberWikiData();
    private static final BooleanWikiData IGNITE_BY_LAVA = new BooleanWikiData();
    private static final StringWikiData PUSH_REACTION = new StringWikiData();
    private static final ExceptData PUSH_REACTION_EXCEPT = new ExceptData();
    private static final BooleanWikiData REPLACEABLE = new BooleanWikiData();
    private static final StringListWikiData BREAKING_TOOLS = new StringListWikiData();

    private static final BooleanWikiData REDSTONE_CONDUCTOR = new BooleanWikiData();
    private static final ExceptData REDSTONE_CONDUCTOR_EXCEPT = new ExceptData();
    private static final BooleanWikiData SUFFOCATING = new BooleanWikiData();
    private static final ExceptData SUFFOCATING_EXCEPT = new ExceptData();
    private static final NumberWikiData BURN_ODDS = new NumberWikiData().setFallback(0);
    private static final NumberWikiData IGNITE_ODDS = new NumberWikiData().setFallback(0);
    private static final BooleanWikiData LEGACY_SOLID = new BooleanWikiData();
    private static final ExceptData LEGACY_SOLID_EXCEPT = new ExceptData();
    private static final StringWikiData MAP_COLOR = new StringWikiData();
    private static final ExceptData MAP_COLOR_EXCEPT = new ExceptData();
    private static final StringWikiData INSTRUMENT = new StringWikiData();
    private static final SubStringMapWikiData SUPPORT_TYPE = new SubStringMapWikiData();
    private static final ExceptData SUPPORT_TYPE_EXCEPT = new ExceptData();
    private static final PropertyWikiData BLOCK_PROPERTY_VALUES = new PropertyWikiData();
    private static final PairMapWikiData<String, String> BLOCK_PROPERTIES = new PairMapWikiData<>();
    private static final OcclusionWikiData OCCLUSION_SHAPE_VALUES = new OcclusionWikiData();
    private static final LiquidComputationWikiData LIQUID_COMPUTATION_VALUES = new LiquidComputationWikiData();

    @SneakyThrows
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void extractBlockData(MinecraftServer serverObj) {
        DefaultedRegistry<Block> blockRegistry = BuiltInRegistries.BLOCK;
        Set<ResourceKey<Block>> blockKeySet = blockRegistry.registryKeySet();
        ServerLevel serverOverworld = serverObj.overworld();

        Map<Object, String> revPropertyMap = new HashMap<>();
        for (Field propertyField : BlockStateProperties.class.getDeclaredFields()) {
            Object property = propertyField.get(null);
            String propertyID = propertyField.getName();
            if (property instanceof Property p) {
                String propertyName = p.getName();
                BLOCK_PROPERTY_VALUES.put(propertyID, propertyName);
                p.getPossibleValues().stream()
                 .map(n -> p.getName((Comparable<?>) n))
                 .forEach(value -> BLOCK_PROPERTY_VALUES.putNew(propertyID, (String) value));
                revPropertyMap.put(property, propertyID);
            }
        }

        Int2ObjectMap<String> mapColorMap = new Int2ObjectAVLTreeMap<>();
        Field[] mapColorFields = MapColor.class.getDeclaredFields();
        for (Field field : mapColorFields) {
            field.setAccessible(true);
            if (!field.accessFlags().contains(AccessFlag.STATIC))
                continue;
            Object obj = field.get(null);
            if (obj instanceof MapColor mapColor)
                mapColorMap.put(mapColor.id, field.getName());
        }

        for (ResourceKey<Block> key : blockKeySet) {
            String blockID = key.location().getPath();
            Block block = blockRegistry.getValue(key);
            ImmutableList<BlockState> states = block.getStateDefinition().getPossibleStates();
            BlockBehaviour.Properties properties = block.properties();

            EXPLOSION_RESISTANCE.put(blockID, properties.explosionResistance);
            DESTROY_TIME.put(blockID, properties.destroyTime);
            IGNITE_BY_LAVA.put(blockID, properties.ignitedByLava);
            if (!blockID.equals("piston") && !blockID.equals("sticky_piston"))
                PUSH_REACTION.put(blockID, computePushReaction(block, properties, properties.destroyTime, blockID));
            REPLACEABLE.put(blockID, properties.replaceable);
            if (properties.instrument != null)
                INSTRUMENT.put(blockID, properties.instrument.name());

            for (BlockState state : states) {
                Map<String, List<double[]>> occlusionMap = new HashMap<>();
                Set<String> faceSturdySet = new TreeSet<>();
                String stateName = state.toString();
                if (stateName.contains("[")) {
                    String[] propertiesArray = stateName
                        .substring(stateName.indexOf('[') + 1, stateName.length() - 1)
                        .split(",");
                    List<String> collected = Stream
                        .of(propertiesArray)
                        .filter(s -> !s.startsWith("waterlogged="))
                        .sorted()
                        .toList();
                    stateName = "[" + String.join(",", collected) + "]";
                } else
                    stateName = "";

                for (Direction direction : Direction.values()) {
                    String directionName = direction.name().toLowerCase();
                    if (state.isFaceSturdy(serverOverworld, BlockPos.ZERO, direction, SupportType.FULL))
                        faceSturdySet.add(directionName);
                    VoxelShape faceShape = state.getFaceOcclusionShape(direction);
                    if (faceShape.isEmpty())
                        continue;
                    List<double[]> aabbArray = faceShape.toAabbs().stream().map(aabb -> switch (direction) {
                        case DOWN:
                        case UP:
                            yield new double[]{aabb.minX, aabb.minZ, aabb.maxX, aabb.maxZ};
                        case NORTH:
                        case SOUTH:
                            yield new double[]{aabb.minX, aabb.minY, aabb.maxX, aabb.maxY};
                        case WEST:
                        case EAST:
                            yield new double[]{aabb.minZ, aabb.minY, aabb.maxZ, aabb.maxY};
                    }).toList();
                    occlusionMap.put(directionName, aabbArray);
                }

                OCCLUSION_SHAPE_VALUES.put(blockID + stateName, state.canOcclude(), occlusionMap);
                LIQUID_COMPUTATION_VALUES.put(blockID + stateName, state.blocksMotion(), faceSturdySet);
            }

            makeStatePredicateData(
                blockID,
                properties.isRedstoneConductor,
                states,
                REDSTONE_CONDUCTOR,
                REDSTONE_CONDUCTOR_EXCEPT
            );
            makeStatePredicateData(blockID, properties.isSuffocating, states, SUFFOCATING, SUFFOCATING_EXCEPT);

            calcSolid(blockID, states);
            resolveMapColor(blockID, states, mapColorMap);
            resolveSupportType(serverOverworld, blockID, states);

            BlockState defaultBlockState = block.defaultBlockState();
            List<String> breakingTools = resolveBreakingTools(blockID, properties, defaultBlockState);
            BREAKING_TOOLS.put(blockID, breakingTools);

            BURN_ODDS.put(blockID, ((FireBlock) Blocks.FIRE).getBurnOdds(defaultBlockState));
            IGNITE_ODDS.put(blockID, ((FireBlock) Blocks.FIRE).getIgniteOdds(defaultBlockState));

            Map<Property<?>, Comparable<?>> defaultStateMap = defaultBlockState.getValues();
            BLOCK_PROPERTIES.put(blockID);
            for (Map.Entry<Property<?>, Comparable<?>> entry : defaultStateMap.entrySet()) {
                Property property = entry.getKey();
                BLOCK_PROPERTIES.putNew(blockID, revPropertyMap.get(property), property.getName(entry.getValue()));
            }
        }

        PUSH_REACTION_EXCEPT.putUnknown("piston");
        PUSH_REACTION_EXCEPT.putUnknown("sticky_piston");

        WikiData.write(EXPLOSION_RESISTANCE, "block_explosion_resistance.txt");
        WikiData.write(DESTROY_TIME, "block_destroy_time.txt");
        WikiData.write(IGNITE_BY_LAVA, "block_ignite_by_lava.txt");
        WikiData.write(PUSH_REACTION, PUSH_REACTION_EXCEPT, "block_push_reaction.txt");
        WikiData.write(REPLACEABLE, "block_replaceable.txt");
        WikiData.write(REDSTONE_CONDUCTOR, REDSTONE_CONDUCTOR_EXCEPT, "block_redstone_conductor.txt");
        WikiData.write(SUFFOCATING, SUFFOCATING_EXCEPT, "block_suffocating.txt");
        WikiData.write(BREAKING_TOOLS, "block_breaking_tools.txt");
        WikiData.write(BURN_ODDS, "block_burn_odds.txt");
        WikiData.write(IGNITE_ODDS, "block_ignite_odds.txt");
        WikiData.write(LEGACY_SOLID, LEGACY_SOLID_EXCEPT, "block_legacy_solid.txt");
        WikiData.write(MAP_COLOR, MAP_COLOR_EXCEPT, "block_map_color.txt");
        WikiData.write(INSTRUMENT, "block_instrument.txt");
        WikiData.write(SUPPORT_TYPE, SUPPORT_TYPE_EXCEPT, "block_support_type.txt");
        WikiData.write(BLOCK_PROPERTY_VALUES, "block_property_values.txt");
        WikiData.write(BLOCK_PROPERTIES, "block_properties.txt");
        WikiData.write(OCCLUSION_SHAPE_VALUES, "block_occlusion_shape.json");
        WikiData.write(LIQUID_COMPUTATION_VALUES, "block_liquid_computation.json");
    }

    private static final Set<String> OVERRIDE_BLOCK_PUSH_REACTION = Set.of(
        "obsidian",
        "crying_obsidian",
        "respawn_anchor",
        "reinforced_deepslate"
    );

    public static String computePushReaction(Block block, BlockBehaviour.Properties properties, float destroyTime, String blockID) {
        if (OVERRIDE_BLOCK_PUSH_REACTION.contains(blockID))
            return "BLOCK";
        if (destroyTime == -1.0f)
            return "BLOCK";
        String pushReaction = properties.pushReaction.name();
        if (block instanceof EntityBlock && pushReaction.equals("NORMAL"))
            return "BLOCK";
        return pushReaction;
    }

    @SneakyThrows
    public static void makeStatePredicateData(
        String blockID,
        BlockBehaviour.StatePredicate statePredicate, ImmutableList<BlockState> states,
        BooleanWikiData wikiData, ExceptData exceptData
    ) {
        Set<String> trueSet = new LinkedHashSet<>();
        Set<String> falseSet = new LinkedHashSet<>();
        for (BlockState state : states) {
            try {
                boolean test = statePredicate.test(state, null, null);
                String stateString = state.toString();
                if (stateString.contains("["))
                    stateString = stateString.substring(stateString.indexOf('['));
                if (test)
                    trueSet.add(stateString);
                else
                    falseSet.add(stateString);
            } catch (NullPointerException e) {
                exceptData.putUnknown(blockID);
                return;
            }
        }
        if (trueSet.isEmpty())
            wikiData.put(blockID, false);
        else if (falseSet.isEmpty())
            wikiData.put(blockID, true);
        else {
            for (String state : trueSet)
                exceptData.put(blockID, state, "true");
            for (String state : falseSet)
                exceptData.put(blockID, state, "false");
        }
    }

    @SneakyThrows
    public static void calcSolid(String blockID, ImmutableList<BlockState> states) {
        Set<String> trueSet = new LinkedHashSet<>();
        Set<String> falseSet = new LinkedHashSet<>();
        for (BlockState state : states) {
            String stateString = state.toString();
            if (stateString.contains("["))
                stateString = stateString.substring(stateString.indexOf('['));
            if (state.legacySolid)
                trueSet.add(stateString);
            else
                falseSet.add(stateString);
        }
        if (trueSet.isEmpty())
            LEGACY_SOLID.put(blockID, false);
        else if (falseSet.isEmpty())
            LEGACY_SOLID.put(blockID, true);
        else {
            for (String state : trueSet)
                LEGACY_SOLID_EXCEPT.put(blockID, state, "true");
            for (String state : falseSet)
                LEGACY_SOLID_EXCEPT.put(blockID, state, "false");
        }
    }

    @SneakyThrows
    private static void resolveMapColor(String blockID, ImmutableList<BlockState> states, Int2ObjectMap<String> mapColorMap) {
        Map<String, List<String>> map = new HashMap<>();
        for (BlockState state : states) {
            String stateString = state.toString();
            if (stateString.contains("["))
                stateString = stateString.substring(stateString.indexOf('['));
            map.computeIfAbsent(mapColorMap.get(state.mapColor.id), k -> new ArrayList<>()).add(stateString);
        }
        if (map.size() == 1)
            MAP_COLOR.put(blockID, map.keySet().iterator().next());
        else {
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                String name = entry.getKey();
                for (String state : entry.getValue())
                    MAP_COLOR_EXCEPT.put(blockID, state, name);
            }
        }
    }

    private static final Set<String> SHEARS_MINEABLES = Set.of(
        "cobweb",
        "vine",
        "glow_lichen"
    );

    @SneakyThrows
    private static List<String> resolveBreakingTools(String blockID, BlockBehaviour.Properties properties, BlockState defaultBlockState) {
        String tierPrefix;
        if (properties.requiresCorrectToolForDrops) {
            if (defaultBlockState.is(BlockTags.NEEDS_DIAMOND_TOOL))
                tierPrefix = "diamond";
            else if (defaultBlockState.is(BlockTags.NEEDS_IRON_TOOL))
                tierPrefix = "iron";
            else if (defaultBlockState.is(BlockTags.NEEDS_STONE_TOOL))
                tierPrefix = "stone";
            else
                tierPrefix = "wooden";
        } else
            tierPrefix = null;

        List<String> tools = new ArrayList<>();

        if (defaultBlockState.is(BlockTags.MINEABLE_WITH_AXE))
            tools.add("axe");
        if (defaultBlockState.is(BlockTags.MINEABLE_WITH_HOE))
            tools.add("hoe");
        if (defaultBlockState.is(BlockTags.MINEABLE_WITH_PICKAXE))
            tools.add("pickaxe");
        if (defaultBlockState.is(BlockTags.MINEABLE_WITH_SHOVEL))
            tools.add("shovel");
        if (defaultBlockState.is(BlockTags.SWORD_EFFICIENT) || defaultBlockState.is(BlockTags.SWORD_INSTANTLY_MINES))
            tools.add("sword");

        if (tierPrefix != null)
            tools.replaceAll(s -> tierPrefix + " " + s);

        if (defaultBlockState.is(BlockTags.LEAVES) ||
            defaultBlockState.is(BlockTags.WOOL) ||
            SHEARS_MINEABLES.contains(blockID))
            tools.add(properties.requiresCorrectToolForDrops ? "shears required" : "shears");

        if (properties.requiresCorrectToolForDrops && tools.isEmpty())
            tools.add("null required");

        tools.sort(Comparator.naturalOrder());

        return tools;
    }

    @SneakyThrows
    public static void resolveSupportType(ServerLevel level, String blockID, ImmutableList<BlockState> states) {
        Map<String, Map<String, List<String>>> map = new HashMap<>();
        boolean needExcept = false;
        for (Direction direction : Direction.values()) {
            Map<String, List<String>> subMap = new HashMap<>();
            map.put(direction.name(), subMap);
            for (BlockState state : states) {
                String stateString = state.toString();
                if (stateString.contains("["))
                    stateString = stateString.substring(stateString.indexOf('['));
                boolean testFULL = state.isFaceSturdy(level, BlockPos.ZERO, direction, SupportType.FULL);
                if (testFULL)
                    subMap.computeIfAbsent("FULL", k -> new ArrayList<>()).add(stateString);
                else {
                    boolean testCENTER = state.isFaceSturdy(level, BlockPos.ZERO, direction, SupportType.CENTER);
                    boolean testRIGID = state.isFaceSturdy(level, BlockPos.ZERO, direction, SupportType.RIGID);
                    if (testCENTER && testRIGID)
                        subMap.computeIfAbsent("CENTER_AND_RIGID", k -> new ArrayList<>()).add(stateString);
                    else if (testCENTER)
                        subMap.computeIfAbsent("CENTER", k -> new ArrayList<>()).add(stateString);
                    else if (testRIGID)
                        subMap.computeIfAbsent("RIGID", k -> new ArrayList<>()).add(stateString);
                    else
                        subMap.computeIfAbsent("NONE", k -> new ArrayList<>()).add(stateString);
                }
            }
            if (subMap.size() > 1)
                needExcept = true;
        }
        if (needExcept) {
            Map<String, List<String>> exceptMap = new HashMap<>();
            for (Map.Entry<String, Map<String, List<String>>> entry : map.entrySet()) {
                String direction = entry.getKey();
                Map<String, List<String>> subMap = entry.getValue();
                for (Map.Entry<String, List<String>> subEntry : subMap.entrySet()) {
                    String type = subEntry.getKey();
                    List<String> statesList = subEntry.getValue();
                    for (String state : statesList)
                        exceptMap.computeIfAbsent(state, k -> new ArrayList<>()).add(direction + "=" + type);
                }
            }
            for (Map.Entry<String, List<String>> entry : exceptMap.entrySet()) {
                String state = entry.getKey();
                List<String> directionData = entry.getValue();
                SUPPORT_TYPE_EXCEPT.put(blockID, state, String.join(", ", directionData));
            }
        } else {
            for (Map.Entry<String, Map<String, List<String>>> entry : map.entrySet()) {
                String direction = entry.getKey();
                Map<String, List<String>> subMap = entry.getValue();
                String type = subMap.keySet().iterator().next();
                SUPPORT_TYPE.putNew(blockID, direction, type);
            }
        }
    }
}
