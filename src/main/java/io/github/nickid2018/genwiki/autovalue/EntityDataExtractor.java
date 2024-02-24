package io.github.nickid2018.genwiki.autovalue;

import io.github.nickid2018.genwiki.autovalue.wikidata.*;
import io.github.nickid2018.genwiki.inject.InjectedProcess;
import io.github.nickid2018.genwiki.inject.SourceClass;
import lombok.SneakyThrows;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;

public class EntityDataExtractor {

    // Entity
    public static final Class<?> ENTITY_TYPE_CLASS;
    public static final Class<?> LIVING_ENTITY_CLASS;
    public static final Class<?> SERVER_LEVEL_CLASS;
    public static final Class<?> MOB_SPAWN_TYPE_CLASS;
    public static final Class<?> MOB_CATEGORY_CLASS;

    public static final Object MOB_SPAWN_TYPE_COMMAND;

    public static final MethodHandle ENTITY_TYPE_GET_CATEGORY;
    public static final MethodHandle ENTITY_TYPE_SPAWN;
    public static final MethodHandle LIVING_ENTITY_ADD_EFFECT;

    // MobEffect
    public static final Class<?> MOB_EFFECT_CLASS;
    public static final Class<?> MOB_EFFECT_INSTANCE_CLASS;

    public static final MethodHandle MOB_EFFECT_GET_CATEGORY;
    public static final MethodHandle MOB_EFFECT_GET_COLOR;
    public static final MethodHandle MOB_EFFECT_IS_INSTANTENOUS;
    public static final MethodHandle MOB_EFFECT_INSTANCE_CTOR;


    static {
        try {
            ENTITY_TYPE_CLASS = Class.forName("net.minecraft.world.entity.EntityType");
            LIVING_ENTITY_CLASS = Class.forName("net.minecraft.world.entity.LivingEntity");
            SERVER_LEVEL_CLASS = Class.forName("net.minecraft.server.level.ServerLevel");
            MOB_SPAWN_TYPE_CLASS = Class.forName("net.minecraft.world.entity.MobSpawnType");
            MOB_CATEGORY_CLASS = Class.forName("net.minecraft.world.entity.MobCategory");

            MOB_SPAWN_TYPE_COMMAND = MOB_SPAWN_TYPE_CLASS.getField("COMMAND").get(null);

            MethodHandles.Lookup lookup = MethodHandles.lookup();
            ENTITY_TYPE_GET_CATEGORY = lookup.unreflect(ENTITY_TYPE_CLASS.getMethod("getCategory"));
            ENTITY_TYPE_SPAWN = lookup.unreflect(ENTITY_TYPE_CLASS.getMethod(
                    "spawn", SERVER_LEVEL_CLASS, InjectedProcess.BLOCK_POS_CLASS, MOB_SPAWN_TYPE_CLASS));

            MOB_EFFECT_CLASS = Class.forName("net.minecraft.world.effect.MobEffect");
            MOB_EFFECT_INSTANCE_CLASS = Class.forName("net.minecraft.world.effect.MobEffectInstance");

            MOB_EFFECT_GET_CATEGORY = lookup.unreflect(MOB_EFFECT_CLASS.getDeclaredMethod("getCategory"));
            MOB_EFFECT_GET_COLOR = lookup.unreflect(MOB_EFFECT_CLASS.getDeclaredMethod("getColor"));
            MOB_EFFECT_IS_INSTANTENOUS = lookup.unreflect(MOB_EFFECT_CLASS.getDeclaredMethod("isInstantenous"));
            LIVING_ENTITY_ADD_EFFECT = lookup.unreflect(LIVING_ENTITY_CLASS.getMethod("addEffect", MOB_EFFECT_INSTANCE_CLASS));
            MOB_EFFECT_INSTANCE_CTOR = lookup.findConstructor(MOB_EFFECT_INSTANCE_CLASS, MethodType.methodType(void.class, InjectedProcess.HOLDER_CLASS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final StringWikiData MOB_CATEGORY = new StringWikiData();

    public static final StringWikiData EFFECT_CATEGORY = new StringWikiData();
    public static final ColorWikiData EFFECT_COLOR = new ColorWikiData();
    public static final BooleanWikiData EFFECT_INSTANTENOUS = new BooleanWikiData();
    public static final StringListWikiData EFFECT_CANNOT_AFFECT = new StringListWikiData();

    @SneakyThrows
    public static void extractEntityData(Object serverObj) {
        @SourceClass("DefaultedRegistry<EntityType<?>>")
        Object entityRegistry = InjectedProcess.getRegistry("ENTITY_TYPE");
        @SourceClass("Set<ResourceKey<EntityType<?>>>")
        Set<?> entityKeySet = InjectedProcess.getRegistryKeySet(entityRegistry);
        @SourceClass("ServerLevel")
        Object serverOverworld = InjectedProcess.SERVER_OVERWORLD.invoke(serverObj);
        Map<String, Object> livingEntityMap = new HashMap<>();
        for (@SourceClass("ResourceKey<EntityType<?>>") Object key : entityKeySet) {
            @SourceClass("ResourceLocation")
            Object location = InjectedProcess.RESOURCE_KEY_LOCATION.invoke(key);
            String entityID = InjectedProcess.getResourceLocationPath(location);
            @SourceClass("EntityType<?>")
            Object entity = InjectedProcess.REGISTRY_GET.invoke(entityRegistry, key);
            @SourceClass("MobCategory")
            Object category = ENTITY_TYPE_GET_CATEGORY.invoke(entity);
            MOB_CATEGORY.put(entityID, (String) InjectedProcess.ENUM_NAME.invoke(category));

            @SourceClass("Entity")
            Object entityInstance = ENTITY_TYPE_SPAWN.invoke(
                    entity, serverOverworld, InjectedProcess.BLOCK_POS_ZERO, MOB_SPAWN_TYPE_COMMAND);
            if (LIVING_ENTITY_CLASS.isInstance(entityInstance))
                livingEntityMap.put(entityID, entityInstance);
        }

        @SourceClass("Registry<MobEffect>")
        Object effectRegistry = InjectedProcess.getRegistry("MOB_EFFECT");
        @SourceClass("Set<ResourceKey<MobEffect>>")
        Set<?> effectKeySet = InjectedProcess.getRegistryKeySet(effectRegistry);
        for (Object effectKey : effectKeySet) {
            @SourceClass("ResourceLocation")
            Object effectLocation = InjectedProcess.RESOURCE_KEY_LOCATION.invoke(effectKey);
            String effectID = InjectedProcess.getResourceLocationPath(effectLocation);
            @SourceClass("MobEffect")
            Object effect = InjectedProcess.REGISTRY_GET.invoke(effectRegistry, effectKey);

            String category = (String) InjectedProcess.ENUM_NAME.invoke(MOB_EFFECT_GET_CATEGORY.invoke(effect));
            int color = (int) MOB_EFFECT_GET_COLOR.invoke(effect);
            boolean instantenous = (boolean) MOB_EFFECT_IS_INSTANTENOUS.invoke(effect);
            EFFECT_CATEGORY.put(effectID, category);
            EFFECT_COLOR.put(effectID, color);
            EFFECT_INSTANTENOUS.put(effectID, instantenous);

            EFFECT_CANNOT_AFFECT.put(effectID);
            for (Map.Entry<String, Object> entry : livingEntityMap.entrySet()) {
                @SourceClass("LivingEntity")
                Object livingEntity = entry.getValue();
                String entityID = entry.getKey();

                Object effectInstance = MOB_EFFECT_INSTANCE_CTOR.invoke(
                        ((Optional<?>) InjectedProcess.GET_HOLDER.invoke(effectRegistry, effectKey)).orElseThrow());
                boolean canBeAffected = (boolean) LIVING_ENTITY_ADD_EFFECT.invoke(livingEntity, effectInstance);

                if (!canBeAffected)
                    EFFECT_CANNOT_AFFECT.putNew(effectID, entityID);
            }
            EFFECT_CANNOT_AFFECT.get(effectID).sort(Comparator.naturalOrder());
        }

        WikiData.write(MOB_CATEGORY, "entity_mob_category.txt");
        WikiData.write(EFFECT_CATEGORY, "mob_effect_category.txt");
        WikiData.write(EFFECT_COLOR, "mob_effect_color.txt");
        WikiData.write(EFFECT_INSTANTENOUS, "mob_effect_instantenous.txt");
        WikiData.write(EFFECT_CANNOT_AFFECT, "mob_effect_cannot_affect.txt");
    }
}
