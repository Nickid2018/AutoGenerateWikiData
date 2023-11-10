package io.github.nickid2018.genwiki.inject;

import com.google.common.collect.ImmutableList;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.*;

@Slf4j
public class BlockDataExtractor {

    public static final Class<?> BLOCK_CLASS;
    public static final Class<?> PROPERTIES_CLASS;
    public static final Class<?> PUSH_REACTION_CLASS;
    public static final Class<?> BLOCK_STATE_CLASS;
    public static final Class<?> STATE_PREDICATE_CLASS;
    public static final Class<?> ENTITY_BLOCK_CLASS;
    public static final Class<?> STATE_DEFINITION_CLASS;

    @SourceClass("StateDefinition<Block, BlockState>")
    public static final MethodHandle GET_STATE_DEFINITION;
    @SourceClass("BlockBehaviour.Properties")
    public static final MethodHandle PROPERTIES;
    public static final MethodHandle STATE_PREDICATE_TEST;
    @SourceClass("BlockState")
    public static final MethodHandle BLOCK_DEFAULT_BLOCK_STATE;
    public static final MethodHandle STATE_DEFINITION_GET_POSSIBLE_STATES;
    public static final MethodHandle BLOCKSTATE_IS;
    public static final MethodHandle GET_BURN_ODDS;
    public static final MethodHandle GET_IGNITE_ODDS;

    public static final VarHandle PROPERTIES_EXPLOSION_RESISTANCE;
    public static final VarHandle PROPERTIES_DESTROY_TIME;
    public static final VarHandle PROPERTIES_IGNITE_BY_LAVA;
    public static final VarHandle PROPERTIES_PUSH_REACTION;
    public static final VarHandle PROPERTIES_REPLACEABLE;
    public static final VarHandle PROPERTIES_IS_REDSTONE_CONDUCTOR;
    public static final VarHandle PROPERTIES_IS_SUFFOCATING;
    public static final VarHandle PROPERTIES_REQUIRES_CORRECT_TOOL_FOR_DROPS;

    public static final Object TAG_NEEDS_DIAMOND_TOOL;
    public static final Object TAG_NEEDS_IRON_TOOL;
    public static final Object TAG_NEEDS_STONE_TOOL;
    public static final Object TAG_MINEABLE_WITH_AXE;
    public static final Object TAG_MINEABLE_WITH_HOE;
    public static final Object TAG_MINEABLE_WITH_PICKAXE;
    public static final Object TAG_MINEABLE_WITH_SHOVEL;
    public static final Object TAG_SWORD_EFFICIENT;
    public static final Object TAG_LEAVES;
    public static final Object TAG_WOOL;

    public static final Object FIRE_BLOCK;


    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            BLOCK_CLASS = Class.forName("net.minecraft.world.level.block.Block");
            PROPERTIES_CLASS = Class.forName("net.minecraft.world.level.block.state.BlockBehaviour$Properties");
            PUSH_REACTION_CLASS = Class.forName("net.minecraft.world.level.material.PushReaction");
            BLOCK_STATE_CLASS = Class.forName("net.minecraft.world.level.block.state.BlockState");
            STATE_PREDICATE_CLASS = Class.forName("net.minecraft.world.level.block.state.BlockBehaviour$StatePredicate");
            ENTITY_BLOCK_CLASS = Class.forName("net.minecraft.world.level.block.EntityBlock");
            STATE_DEFINITION_CLASS = Class.forName("net.minecraft.world.level.block.state.StateDefinition");

            GET_STATE_DEFINITION = lookup.unreflect(BLOCK_CLASS.getMethod("getStateDefinition"));
            PROPERTIES = lookup.unreflect(BLOCK_CLASS.getMethod("properties"));
            STATE_PREDICATE_TEST = lookup.unreflect(STATE_PREDICATE_CLASS.getDeclaredMethods()[0]);
            BLOCK_DEFAULT_BLOCK_STATE = lookup.unreflect(BLOCK_CLASS.getMethod("defaultBlockState"));
            STATE_DEFINITION_GET_POSSIBLE_STATES = lookup.unreflect(STATE_DEFINITION_CLASS.getMethod("getPossibleStates"));
            BLOCKSTATE_IS = lookup.unreflect(BLOCK_STATE_CLASS.getMethod("is", InjectedProcess.TAG_KEY_CLASS));

            MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(PROPERTIES_CLASS, lookup);
            PROPERTIES_EXPLOSION_RESISTANCE = privateLookup.findVarHandle(PROPERTIES_CLASS, "explosionResistance", float.class);
            PROPERTIES_DESTROY_TIME = privateLookup.findVarHandle(PROPERTIES_CLASS, "destroyTime", float.class);
            PROPERTIES_IGNITE_BY_LAVA = privateLookup.findVarHandle(PROPERTIES_CLASS, "ignitedByLava", boolean.class);
            PROPERTIES_PUSH_REACTION = privateLookup.findVarHandle(PROPERTIES_CLASS, "pushReaction", PUSH_REACTION_CLASS);
            PROPERTIES_REPLACEABLE = privateLookup.findVarHandle(PROPERTIES_CLASS, "replaceable", boolean.class);
            PROPERTIES_IS_REDSTONE_CONDUCTOR = privateLookup.findVarHandle(PROPERTIES_CLASS, "isRedstoneConductor", STATE_PREDICATE_CLASS);
            PROPERTIES_IS_SUFFOCATING = privateLookup.findVarHandle(PROPERTIES_CLASS, "isSuffocating", STATE_PREDICATE_CLASS);
            PROPERTIES_REQUIRES_CORRECT_TOOL_FOR_DROPS = privateLookup.findVarHandle(PROPERTIES_CLASS, "requiresCorrectToolForDrops", boolean.class);

            Field[] fields = Class.forName("net.minecraft.tags.BlockTags").getDeclaredFields();
            Map<String, Field> tagFields = new HashMap<>();
            for (Field field : fields)
                tagFields.put(field.getName(), field);
            TAG_NEEDS_DIAMOND_TOOL = tagFields.get("NEEDS_DIAMOND_TOOL").get(null);
            TAG_NEEDS_IRON_TOOL = tagFields.get("NEEDS_IRON_TOOL").get(null);
            TAG_NEEDS_STONE_TOOL = tagFields.get("NEEDS_STONE_TOOL").get(null);
            TAG_MINEABLE_WITH_AXE = tagFields.get("MINEABLE_WITH_AXE").get(null);
            TAG_MINEABLE_WITH_HOE = tagFields.get("MINEABLE_WITH_HOE").get(null);
            TAG_MINEABLE_WITH_PICKAXE = tagFields.get("MINEABLE_WITH_PICKAXE").get(null);
            TAG_MINEABLE_WITH_SHOVEL = tagFields.get("MINEABLE_WITH_SHOVEL").get(null);
            TAG_SWORD_EFFICIENT = tagFields.get("SWORD_EFFICIENT").get(null);
            TAG_LEAVES = tagFields.get("LEAVES").get(null);
            TAG_WOOL = tagFields.get("WOOL").get(null);

            Field[] fields2 = Class.forName("net.minecraft.world.level.block.Blocks").getDeclaredFields();
            Map<String, Field> blockFields = new HashMap<>();
            for (Field field : fields2)
                blockFields.put(field.getName(), field);
            FIRE_BLOCK = blockFields.get("FIRE").get(null);
            Class<?> CLASS_FIRE = Class.forName("net.minecraft.world.level.block.FireBlock");
            MethodHandles.Lookup privateLookup2 = MethodHandles.privateLookupIn(CLASS_FIRE, lookup);
            GET_BURN_ODDS = privateLookup2.findVirtual(CLASS_FIRE, "getBurnOdds", MethodType.methodType(int.class, BLOCK_STATE_CLASS));
            GET_IGNITE_ODDS = privateLookup2.findVirtual(CLASS_FIRE, "getIgniteOdds", MethodType.methodType(int.class, BLOCK_STATE_CLASS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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

    @SneakyThrows
    public static void extractBlockData() {
        @SourceClass("DefaultedRegistry<Block>")
        Object blockRegistry = InjectedProcess.getRegistry("BLOCK");
        @SourceClass("Set<ResourceKey<Block>>")
        Set<?> blockKeySet = InjectedProcess.getRegistryKeySet(blockRegistry);
        for (@SourceClass("ResourceKey<Block>") Object key : blockKeySet) {
            @SourceClass("ResourceLocation")
            Object location = InjectedProcess.RESOURCE_KEY_LOCATION.invoke(key);
            String blockID = InjectedProcess.getResourceLocationPath(location);
            @SourceClass("Block")
            Object block = InjectedProcess.REGISTRY_GET.invoke(blockRegistry, key);
            @SourceClass("StateDefinition<Block, BlockState>")
            Object stateDefinition = GET_STATE_DEFINITION.invoke(block);
            @SourceClass("ImmutableList<BlockState>")
            ImmutableList<?> states = (ImmutableList<?>) STATE_DEFINITION_GET_POSSIBLE_STATES.invoke(stateDefinition);
            @SourceClass("BlockBehaviour.Properties")
            Object properties = PROPERTIES.invoke(block);

            float explosionResistance = (float) PROPERTIES_EXPLOSION_RESISTANCE.get(properties);
            EXPLOSION_RESISTANCE.put(blockID, explosionResistance);
            float destroyTime = (float) PROPERTIES_DESTROY_TIME.get(properties);
            DESTROY_TIME.put(blockID, destroyTime);
            boolean igniteByLava = (boolean) PROPERTIES_IGNITE_BY_LAVA.get(properties);
            IGNITE_BY_LAVA.put(blockID, igniteByLava);
            if (!blockID.equals("piston") && !blockID.equals("sticky_piston"))
                PUSH_REACTION.put(blockID, computePushReaction(block, properties, destroyTime, blockID));
            boolean replaceable = (boolean) PROPERTIES_REPLACEABLE.get(properties);
            REPLACEABLE.put(blockID, replaceable);

            @SourceClass("BlockBehaviour$StatePredicate")
            Object isRedstoneConductor = PROPERTIES_IS_REDSTONE_CONDUCTOR.get(properties);
            makeStatePredicateData(blockID, isRedstoneConductor, states, REDSTONE_CONDUCTOR, REDSTONE_CONDUCTOR_EXCEPT);
            @SourceClass("BlockBehaviour$StatePredicate")
            Object isSuffocating = PROPERTIES_IS_SUFFOCATING.get(properties);
            makeStatePredicateData(blockID, isSuffocating, states, SUFFOCATING, SUFFOCATING_EXCEPT);

            @SourceClass("BlockState")
            Object defaultBlockState = BLOCK_DEFAULT_BLOCK_STATE.invoke(block);
            List<String> breakingTools = resolveBreakingTools(blockID, properties, defaultBlockState);
            BREAKING_TOOLS.put(blockID, breakingTools);

            int burnOdds = (int) GET_BURN_ODDS.invoke(FIRE_BLOCK, defaultBlockState);
            BURN_ODDS.put(blockID, burnOdds);
            int igniteOdds = (int) GET_IGNITE_ODDS.invoke(FIRE_BLOCK, defaultBlockState);
            IGNITE_ODDS.put(blockID, igniteOdds);
        }

        PUSH_REACTION_EXCEPT.putUnknown("piston");
        PUSH_REACTION_EXCEPT.putUnknown("sticky_piston");

        InjectedProcess.write(EXPLOSION_RESISTANCE, "block_explosion_resistance.txt");
        InjectedProcess.write(DESTROY_TIME, "block_destroy_time.txt");
        InjectedProcess.write(IGNITE_BY_LAVA, "block_ignite_by_lava.txt");
        InjectedProcess.write(PUSH_REACTION, PUSH_REACTION_EXCEPT, "block_push_reaction.txt");
        InjectedProcess.write(REPLACEABLE, "block_replaceable.txt");
        InjectedProcess.write(REDSTONE_CONDUCTOR, REDSTONE_CONDUCTOR_EXCEPT, "block_redstone_conductor.txt");
        InjectedProcess.write(SUFFOCATING, SUFFOCATING_EXCEPT, "block_suffocating.txt");
        InjectedProcess.write(BREAKING_TOOLS, "block_breaking_tools.txt");
        InjectedProcess.write(BURN_ODDS, "block_burn_odds.txt");
        InjectedProcess.write(IGNITE_ODDS, "block_ignite_odds.txt");
    }

    private static final String[] PUSH_REACTION_NAMES = new String[]{
            "NORMAL", "DESTROY", "BLOCK", "IGNORE", "PUSH_ONLY"
    };

    private static final Set<String> OVERRIDE_BLOCK_PUSH_REACTION = Set.of(
            "obsidian",
            "crying_obsidian",
            "respawn_anchor",
            "reinforced_deepslate"
    );

    public static String computePushReaction(Object block, Object properties, float destroyTime, String blockID) {
        if (OVERRIDE_BLOCK_PUSH_REACTION.contains(blockID))
            return "BLOCK";
        if (destroyTime == -1.0f)
            return "BLOCK";
        Object pushReactionObject = PROPERTIES_PUSH_REACTION.get(properties);
        String pushReaction = pushReactionToString(pushReactionObject);
        if (ENTITY_BLOCK_CLASS.isInstance(block) && pushReaction.equals("NORMAL"))
            return "BLOCK";
        return pushReaction;
    }

    @SneakyThrows
    public static String pushReactionToString(Object pushReactionObject) {
        int ordinal = (int) InjectedProcess.ENUM_ORDINAL.invoke(pushReactionObject);
        return PUSH_REACTION_NAMES[ordinal];
    }

    @SneakyThrows
    public static void makeStatePredicateData(String blockID, Object statePredicate, ImmutableList<?> states,
                                              BooleanWikiData wikiData, ExceptData exceptData) {
        Set<String> trueSet = new LinkedHashSet<>();
        Set<String> falseSet = new LinkedHashSet<>();
        for (Object state : states) {
            try {
                boolean test = (boolean) STATE_PREDICATE_TEST.invoke(statePredicate, state, null, null);
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
    private static boolean hasTag(Object blockState, Object tag) {
        return (boolean) BLOCKSTATE_IS.invoke(blockState, tag);
    }

    private static final Set<String> SHEARS_MINEABLES = Set.of(
            "cobweb",
            "vine",
            "glow_lichen"
    );

    @SneakyThrows
    private static List<String> resolveBreakingTools(String blockID, Object properties, Object defaultBlockState) {
        String tierPrefix;
        boolean needsCorrectTool = (boolean) PROPERTIES_REQUIRES_CORRECT_TOOL_FOR_DROPS.get(properties);
        if (needsCorrectTool) {
            if (hasTag(defaultBlockState, TAG_NEEDS_DIAMOND_TOOL))
                tierPrefix = "diamond";
            else if (hasTag(defaultBlockState, TAG_NEEDS_IRON_TOOL))
                tierPrefix = "iron";
            else if (hasTag(defaultBlockState, TAG_NEEDS_STONE_TOOL))
                tierPrefix = "stone";
            else
                tierPrefix = "wooden";
        } else
            tierPrefix = null;

        List<String> tools = new ArrayList<>();

        if (hasTag(defaultBlockState, TAG_MINEABLE_WITH_AXE))
            tools.add("axe");
        if (hasTag(defaultBlockState, TAG_MINEABLE_WITH_HOE))
            tools.add("hoe");
        if (hasTag(defaultBlockState, TAG_MINEABLE_WITH_PICKAXE))
            tools.add("pickaxe");
        if (hasTag(defaultBlockState, TAG_MINEABLE_WITH_SHOVEL))
            tools.add("shovel");
        if (hasTag(defaultBlockState, TAG_SWORD_EFFICIENT))
            tools.add("sword");

        if (tierPrefix != null)
            tools.replaceAll(s -> tierPrefix + " " + s);

        if (hasTag(defaultBlockState, TAG_LEAVES) || hasTag(defaultBlockState, TAG_WOOL) || SHEARS_MINEABLES.contains(blockID))
            tools.add(needsCorrectTool ? "shears required" : "shears");

        tools.sort(Comparator.naturalOrder());

        return tools;
    }
}
