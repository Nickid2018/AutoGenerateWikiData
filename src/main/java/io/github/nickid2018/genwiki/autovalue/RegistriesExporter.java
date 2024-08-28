package io.github.nickid2018.genwiki.autovalue;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import io.github.nickid2018.genwiki.InjectionEntrypoint;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.SneakyThrows;
import net.minecraft.core.IdMapper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

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
                    idMap.put(key.location().getPath(), id);
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
                FileWriter writer = new FileWriter(file);
                writer.write(array.toString());
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
        FileWriter writer = new FileWriter(blockStateFile);
        writer.write(blockStateArray.toString());
        writer.close();
    }
}
