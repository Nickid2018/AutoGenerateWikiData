package io.github.nickid2018.genwiki.inject;

import com.google.common.collect.ImmutableList;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    public static final VarHandle PROPERTIES_EXPLOSION_RESISTANCE;
    public static final VarHandle PROPERTIES_DESTROY_TIME;
    public static final VarHandle PROPERTIES_IGNITE_BY_LAVA;
    public static final VarHandle PROPERTIES_PUSH_REACTION;
    public static final VarHandle PROPERTIES_REPLACEABLE;
    public static final VarHandle PROPERTIES_IS_REDSTONE_CONDUCTOR;
    public static final VarHandle PROPERTIES_IS_SUFFOCATING;


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

            MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(PROPERTIES_CLASS, lookup);
            PROPERTIES_EXPLOSION_RESISTANCE = privateLookup.findVarHandle(PROPERTIES_CLASS, "explosionResistance", float.class);
            PROPERTIES_DESTROY_TIME = privateLookup.findVarHandle(PROPERTIES_CLASS, "destroyTime", float.class);
            PROPERTIES_IGNITE_BY_LAVA = privateLookup.findVarHandle(PROPERTIES_CLASS, "ignitedByLava", boolean.class);
            PROPERTIES_PUSH_REACTION = privateLookup.findVarHandle(PROPERTIES_CLASS, "pushReaction", PUSH_REACTION_CLASS);
            PROPERTIES_REPLACEABLE = privateLookup.findVarHandle(PROPERTIES_CLASS, "replaceable", boolean.class);
            PROPERTIES_IS_REDSTONE_CONDUCTOR = privateLookup.findVarHandle(PROPERTIES_CLASS, "isRedstoneConductor", STATE_PREDICATE_CLASS);
            PROPERTIES_IS_SUFFOCATING = privateLookup.findVarHandle(PROPERTIES_CLASS, "isSuffocating", STATE_PREDICATE_CLASS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final NumberWikiData EXPLOSION_RESISTANCE = new NumberWikiData();
    private static final NumberWikiData DESTROY_TIME = new NumberWikiData();
    private static final BooleanWikiData IGNITE_BY_LAVA = new BooleanWikiData();
    private static final StringWikiData PUSH_REACTION = new StringWikiData();
    private static final BooleanWikiData REPLACEABLE = new BooleanWikiData();

    private static final BooleanWikiData REDSTONE_CONDUCTOR = new BooleanWikiData();
    private static final ExceptData REDSTONE_CONDUCTOR_EXCEPT = new ExceptData();
    private static final BooleanWikiData SUFFOCATING = new BooleanWikiData();
    private static final ExceptData SUFFOCATING_EXCEPT = new ExceptData();

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
            PUSH_REACTION.put(blockID, computePushReaction(block, properties, destroyTime, blockID));
            boolean replaceable = (boolean) PROPERTIES_REPLACEABLE.get(properties);
            REPLACEABLE.put(blockID, replaceable);

            @SourceClass("BlockBehaviour$StatePredicate")
            Object isRedstoneConductor = PROPERTIES_IS_REDSTONE_CONDUCTOR.get(properties);
            makeStatePredicateData(blockID, isRedstoneConductor, states, REDSTONE_CONDUCTOR, REDSTONE_CONDUCTOR_EXCEPT);
            @SourceClass("BlockBehaviour$StatePredicate")
            Object isSuffocating = PROPERTIES_IS_SUFFOCATING.get(properties);
            makeStatePredicateData(blockID, isSuffocating, states, SUFFOCATING, SUFFOCATING_EXCEPT);
        }

        InjectedProcess.write(EXPLOSION_RESISTANCE, "block_explosion_resistance.txt");
        InjectedProcess.write(DESTROY_TIME, "block_destroy_time.txt");
        InjectedProcess.write(IGNITE_BY_LAVA, "block_ignite_by_lava.txt");
        InjectedProcess.write(PUSH_REACTION, "block_push_reaction.txt");
        InjectedProcess.write(REPLACEABLE, "block_replaceable.txt");
        InjectedProcess.write(REDSTONE_CONDUCTOR, REDSTONE_CONDUCTOR_EXCEPT, "block_redstone_conductor.txt");
        InjectedProcess.write(SUFFOCATING, SUFFOCATING_EXCEPT, "block_suffocating.txt");
    }

    private static final String[] PUSH_REACTION_NAMES = new String[] {
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
}
