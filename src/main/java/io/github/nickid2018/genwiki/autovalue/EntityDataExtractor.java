package io.github.nickid2018.genwiki.autovalue;

import io.github.nickid2018.genwiki.autovalue.wikidata.StringWikiData;
import io.github.nickid2018.genwiki.autovalue.wikidata.WikiData;
import io.github.nickid2018.genwiki.inject.InjectedProcess;
import io.github.nickid2018.genwiki.inject.SourceClass;
import lombok.SneakyThrows;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EntityDataExtractor {

    public static final Class<?> ENTITY_TYPE_CLASS;
    public static final Class<?> LIVING_ENTITY_CLASS;
    public static final Class<?> SERVER_LEVEL_CLASS;
    public static final Class<?> MOB_SPAWN_TYPE_CLASS;
    public static final Class<?> MOB_CATEGORY_CLASS;

    public static final Object MOB_SPAWN_TYPE_COMMAND;

    public static final MethodHandle ENTITY_TYPE_GET_CATEGORY;
    public static final MethodHandle ENTITY_TYPE_SPAWN;

    public static final Map<Object, String> MOB_TYPE_TO_STRING = new HashMap<>();


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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final StringWikiData MOB_CATEGORY = new StringWikiData();

    @SneakyThrows
    public static void extractEntityData(Object serverObj) {
        @SourceClass("DefaultedRegistry<EntityType<?>>")
        Object entityRegistry = InjectedProcess.getRegistry("ENTITY_TYPE");
        @SourceClass("Set<ResourceKey<EntityType<?>>>")
        Set<?> entityKeySet = InjectedProcess.getRegistryKeySet(entityRegistry);
        for (@SourceClass("ResourceKey<EntityType<?>>") Object key : entityKeySet) {
            @SourceClass("ResourceLocation")
            Object location = InjectedProcess.RESOURCE_KEY_LOCATION.invoke(key);
            String entityID = InjectedProcess.getResourceLocationPath(location);
            @SourceClass("EntityType<?>")
            Object entity = InjectedProcess.REGISTRY_GET.invoke(entityRegistry, key);
            @SourceClass("MobCategory")
            Object category = ENTITY_TYPE_GET_CATEGORY.invoke(entity);
            MOB_CATEGORY.put(entityID, (String) InjectedProcess.ENUM_NAME.invoke(category));
        }

        WikiData.write(MOB_CATEGORY, "entity_mob_category.txt");
    }
}
