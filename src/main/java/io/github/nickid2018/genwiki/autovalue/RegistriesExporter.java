package io.github.nickid2018.genwiki.autovalue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import io.github.nickid2018.genwiki.InjectionEntrypoint;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.SneakyThrows;
import net.minecraft.core.IdMapper;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class RegistriesExporter {

    @SneakyThrows
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void exportRegistries() {
        Class<?> registryClass = Registry.class;
        Field[] fields = BuiltInRegistries.class.getFields();
        for (Field field : fields) {
            if (registryClass.isAssignableFrom(field.getType()) && Modifier.isStatic(field.getModifiers())) {
                Registry registry = (Registry) field.get(null);
                Set<ResourceKey> keys = registry.registryKeySet();
                int maxID = 0;
                Object2IntMap<String> idMap = new Object2IntArrayMap<>();
                for (ResourceKey key : keys) {
                    int id = registry.getId(registry.getValue(key));
                    idMap.put(key.identifier().getPath(), id);
                    maxID = Math.max(maxID, id);
                }
                JsonArray array = new JsonArray();
                JsonPrimitive empty = new JsonPrimitive("<Empty Slot>");
                for (int i = 0; i <= maxID; i++)
                    array.add(empty);
                idMap.forEach((key, id) -> array.set(id, new JsonPrimitive(key)));

                String fieldName = field.getName();
                File file = new File(InjectionEntrypoint.OUTPUT_FOLDER, "registries/" + fieldName.toLowerCase(Locale.ROOT) + ".json");
                if (!file.getParentFile().isDirectory())
                    file.getParentFile().mkdirs();
                JsonWriter writer = new JsonWriter(new FileWriter(file));
                writer.setIndent("    ");
                Streams.write(array, writer);
                writer.close();
            }
        }

        IdMapper<BlockState> blockStateIdMapper = Block.BLOCK_STATE_REGISTRY;
        JsonArray blockStateArray = new JsonArray();
        Iterator<BlockState> iterator = blockStateIdMapper.iterator();
        while (iterator.hasNext()) {
            BlockState state = iterator.next();
            blockStateArray.add(new JsonPrimitive(state.toString().substring(16).replace("}", "")));
        }
        File blockStateFile = new File(InjectionEntrypoint.OUTPUT_FOLDER, "registries/block_states.json");
        if (!blockStateFile.getParentFile().isDirectory())
            blockStateFile.getParentFile().mkdirs();
        JsonWriter writer = new JsonWriter(new FileWriter(blockStateFile));
        writer.setIndent("    ");
        Streams.write(blockStateArray, writer);
        writer.close();
    }

    @SneakyThrows
    public static void exportServerRegistries(MinecraftServer serverObj) {
        RegistryAccess.Frozen access = serverObj.registryAccess();
        Map<String, JsonObject> registryMap = new TreeMap<>();
        access.registries().forEach(registryEntry -> {
            String name = registryEntry.key().identifier().getPath();
            Registry<?> registry = registryEntry.value();
            Map<String, JsonArray> tagMap = new TreeMap<>();
            registry.getTags().forEach(tag -> {
                String tagName = tag.key().location().getPath();
                List<String> list = new ArrayList<>();
                tag.stream()
                        .map(holder -> holder.unwrapKey().map(key -> key.identifier().getPath()))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .forEach(list::add);
                list.sort(Comparator.naturalOrder());
                JsonArray array = new JsonArray();
                list.forEach(array::add);
                tagMap.put(tagName, array);
            });
            JsonObject tags = new JsonObject();
            tagMap.forEach(tags::add);
            registryMap.put(name, tags);
        });
        JsonObject registries = new JsonObject();
        registryMap.forEach(registries::add);
        File blockStateFile = new File(InjectionEntrypoint.OUTPUT_FOLDER, "tags.json");
        if (!blockStateFile.getParentFile().isDirectory())
            blockStateFile.getParentFile().mkdirs();
        JsonWriter writer = new JsonWriter(new FileWriter(blockStateFile));
        writer.setIndent("    ");
        Streams.write(registries, writer);
        writer.close();
    }
}
