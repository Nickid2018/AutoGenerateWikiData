package io.github.nickid2018.genwiki.autovalue;

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
    public static final Class<?> MOB_TYPE_CLASS;
    public static final Class<?> LIVING_ENTITY_CLASS;
    public static final Class<?> SERVER_LEVEL_CLASS;
    public static final Class<?> BLOCK_POS_CLASS;
    public static final Class<?> MOB_SPAWN_TYPE_CLASS;

    public static final Object BLOCK_POS_ZERO;
    public static final Object MOB_SPAWN_TYPE_COMMAND;

    public static final MethodHandle ENTITY_TYPE_GET_CATEGORY;
    public static final MethodHandle LIVING_ENTITY_GET_MOB_TYPE;
    public static final MethodHandle ENTITY_TYPE_SPAWN;

    public static final Map<Object, String> MOB_TYPE_TO_STRING = new HashMap<>();


    static {
        try {
            ENTITY_TYPE_CLASS = Class.forName("net.minecraft.world.entity.EntityType");
            MOB_TYPE_CLASS = Class.forName("net.minecraft.world.entity.MobType");
            LIVING_ENTITY_CLASS = Class.forName("net.minecraft.world.entity.LivingEntity");
            SERVER_LEVEL_CLASS = Class.forName("net.minecraft.server.level.ServerLevel");
            BLOCK_POS_CLASS = Class.forName("net.minecraft.core.BlockPos");
            MOB_SPAWN_TYPE_CLASS = Class.forName("net.minecraft.world.entity.MobSpawnType");

            for (Field field : MOB_TYPE_CLASS.getDeclaredFields())
                if (field.getType() == MOB_TYPE_CLASS)
                    MOB_TYPE_TO_STRING.put(field.get(null), field.getName());

            BLOCK_POS_ZERO = BLOCK_POS_CLASS.getField("ZERO").get(null);
            MOB_SPAWN_TYPE_COMMAND = MOB_SPAWN_TYPE_CLASS.getField("COMMAND").get(null);

            MethodHandles.Lookup lookup = MethodHandles.lookup();
            ENTITY_TYPE_GET_CATEGORY = lookup.unreflect(ENTITY_TYPE_CLASS.getMethod("getCategory"));
            LIVING_ENTITY_GET_MOB_TYPE = lookup.unreflect(LIVING_ENTITY_CLASS.getMethod("getMobType"));
            ENTITY_TYPE_SPAWN = lookup.unreflect(ENTITY_TYPE_CLASS.getMethod("spawn", SERVER_LEVEL_CLASS, BLOCK_POS_CLASS, MOB_SPAWN_TYPE_CLASS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final StringWikiData MOB_CATEGORY = new StringWikiData();
    private static final StringWikiData MOB_TYPE = new StringWikiData();

    @SneakyThrows
    public static void extractEntityData(Object serverObj) {
        @SourceClass("DefaultedRegistry<EntityType<?>>")
        Object entityRegistry = InjectedProcess.getRegistry("ENTITY_TYPE");
        @SourceClass("Set<ResourceKey<EntityType<?>>>")
        Set<?> entityKeySet = InjectedProcess.getRegistryKeySet(entityRegistry);
        @SourceClass("ServerLevel")
        Object serverOverworld = InjectedProcess.SERVER_OVERWORLD.invoke(serverObj);
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
            Object entityInstance = ENTITY_TYPE_SPAWN.invoke(entity, serverOverworld, BLOCK_POS_ZERO, MOB_SPAWN_TYPE_COMMAND);
            if (LIVING_ENTITY_CLASS.isInstance(entityInstance))
                MOB_TYPE.put(entityID, MOB_TYPE_TO_STRING.get(LIVING_ENTITY_GET_MOB_TYPE.invoke(entityInstance)));
            else
                MOB_TYPE.put(entityID, "NON_LIVING_ENTITY");
        }

        WikiData.write(MOB_CATEGORY, "entity_mob_category.txt");
        WikiData.write(MOB_TYPE, "entity_mob_type.txt");
    }
}
