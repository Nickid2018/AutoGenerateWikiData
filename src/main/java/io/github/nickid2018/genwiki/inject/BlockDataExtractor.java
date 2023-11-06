package io.github.nickid2018.genwiki.inject;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Set;

@Slf4j
public class BlockDataExtractor {

    public static final Class<?> BLOCK_CLASS;
    public static final Class<?> PROPERTIES_CLASS;
    public static final Class<?> PUSH_REACTION_CLASS;
    public static final Class<?> BLOCK_STATE_CLASS;
    public static final Class<?> STATE_PREDICATE_CLASS;

    @SourceClass("StateDefinition<Block, BlockState>")
    public static final MethodHandle GET_STATE_DEFINITION;
    @SourceClass("BlockBehaviour.Properties")
    public static final MethodHandle PROPERTIES;
    public static final MethodHandle STATE_PREDICATE_TEST;

    public static final VarHandle PROPERTIES_EXPLOSION_RESISTANCE;
    public static final VarHandle PROPERTIES_DESTROY_TIME;
    public static final VarHandle PROPERTIES_IGNITE_BY_LAVA;
    public static final VarHandle PROPERTIES_PUSH_REACTION;
    public static final VarHandle PROPERTIES_REPLACEABLE;
    public static final VarHandle PROPERTIES_IS_REDSTONE_CONDUCTOR;


    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            BLOCK_CLASS = Class.forName("net.minecraft.world.level.block.Block");
            PROPERTIES_CLASS = Class.forName("net.minecraft.world.level.block.state.BlockBehaviour$Properties");
            PUSH_REACTION_CLASS = Class.forName("net.minecraft.world.level.material.PushReaction");
            BLOCK_STATE_CLASS = Class.forName("net.minecraft.world.level.block.state.BlockState");
            STATE_PREDICATE_CLASS = Class.forName("net.minecraft.world.level.block.state.BlockBehaviour$StatePredicate");

            GET_STATE_DEFINITION = lookup.unreflect(BLOCK_CLASS.getMethod("getStateDefinition"));
            PROPERTIES = lookup.unreflect(BLOCK_CLASS.getMethod("properties"));
            STATE_PREDICATE_TEST = lookup.unreflect(STATE_PREDICATE_CLASS.getDeclaredMethods()[0]);

            MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(PROPERTIES_CLASS, lookup);
            PROPERTIES_EXPLOSION_RESISTANCE = privateLookup.findVarHandle(PROPERTIES_CLASS, "explosionResistance", float.class);
            PROPERTIES_DESTROY_TIME = privateLookup.findVarHandle(PROPERTIES_CLASS, "destroyTime", float.class);
            PROPERTIES_IGNITE_BY_LAVA = privateLookup.findVarHandle(PROPERTIES_CLASS, "ignitedByLava", boolean.class);
            PROPERTIES_PUSH_REACTION = privateLookup.findVarHandle(PROPERTIES_CLASS, "pushReaction", PUSH_REACTION_CLASS);
            PROPERTIES_REPLACEABLE = privateLookup.findVarHandle(PROPERTIES_CLASS, "replaceable", boolean.class);
            PROPERTIES_IS_REDSTONE_CONDUCTOR = privateLookup.findVarHandle(PROPERTIES_CLASS, "isRedstoneConductor", STATE_PREDICATE_CLASS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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
            @SourceClass("BlockBehaviour.Properties")
            Object properties = PROPERTIES.invoke(block);

            float explosionResistance = (float) PROPERTIES_EXPLOSION_RESISTANCE.get(properties);
            float destroyTime = (float) PROPERTIES_DESTROY_TIME.get(properties);
            boolean igniteByLava = (boolean) PROPERTIES_IGNITE_BY_LAVA.get(properties);
            Object pushReactionObject = PROPERTIES_PUSH_REACTION.get(properties);
            String pushReaction = pushReactionToString(pushReactionObject);
            boolean replaceable = (boolean) PROPERTIES_REPLACEABLE.get(properties);

            Object isRedstoneConductor = PROPERTIES_IS_REDSTONE_CONDUCTOR.get(properties);

            log.info("ID: {}, ER: {}, DT: {}, IL: {}, PR: {}, R: {}",
                    blockID, explosionResistance, destroyTime, igniteByLava, pushReaction, replaceable);
        }
    }

    private static final String[] PUSH_REACTION_NAMES = new String[] {
            "NORMAL", "DESTROY", "BLOCK", "IGNORE", "PUSH_ONLY"
    };

    @SneakyThrows
    public static String pushReactionToString(Object pushReactionObject) {
        int ordinal = (int) InjectedProcess.ENUM_ORDINAL.invoke(pushReactionObject);
        return PUSH_REACTION_NAMES[ordinal];
    }
}
