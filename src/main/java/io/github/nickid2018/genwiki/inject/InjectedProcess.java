package io.github.nickid2018.genwiki.inject;

import lombok.Generated;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
public class InjectedProcess {

    private static final Map<String, Object> REGISTRY = new HashMap<>();

    public static final Class<?> TAG_KEY_CLASS;

    public static final MethodHandle ENUM_ORDINAL;

    @SourceClass("Set<ResourceKey<T>>")
    public static final MethodHandle REGISTRY_KEY_SET;
    @SourceClass("T")
    public static final MethodHandle REGISTRY_GET;
    @SourceClass("ResourceLocation")
    public static final MethodHandle RESOURCE_KEY_LOCATION;
    @SourceClass("String")
    public static final MethodHandle RESOURCE_LOCATION_PATH;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            TAG_KEY_CLASS = Class.forName("net.minecraft.tags.TagKey");
            ENUM_ORDINAL = lookup.unreflect(Enum.class.getMethod("ordinal"));
            Class<?> registryClass = Class.forName("net.minecraft.core.Registry");
            Class<?> resourceKeyClass = Class.forName("net.minecraft.resources.ResourceKey");
            Class<?> resourceLocationClass = Class.forName("net.minecraft.resources.ResourceLocation");
            REGISTRY_KEY_SET = lookup.unreflect(registryClass.getMethod("registryKeySet"));
            REGISTRY_GET = lookup.unreflect(registryClass.getMethod("get", resourceKeyClass));
            RESOURCE_KEY_LOCATION = lookup.unreflect(resourceKeyClass.getMethod("location"));
            RESOURCE_LOCATION_PATH = lookup.unreflect(resourceLocationClass.getMethod("getPath"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    @SourceClass("Set<ResourceKey<T>>")
    public static Set<?> getRegistryKeySet(Object registry) {
        return (Set<?>) REGISTRY_KEY_SET.invoke(registry);
    }

    @SneakyThrows
    public static String getResourceLocationPath(Object resourceKey) {
        return (String) RESOURCE_LOCATION_PATH.invoke(resourceKey);
    }

    @SneakyThrows
    @SuppressWarnings("unused")
    public static void onInjection(Object server) {
        log.info("Trapped server instance: {}", server);
        Class<?> registryClass = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
        Field[] fields = registryClass.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Object obj = field.get(null);
            if (obj != null) {
                log.info("Get registry: {}", field.getName());
                REGISTRY.put(field.getName(), obj);
            }
        }

        if (InjectionConstant.OUTPUT_FOLDER.isDirectory())
            FileUtils.deleteDirectory(InjectionConstant.OUTPUT_FOLDER);
        InjectionConstant.OUTPUT_FOLDER.mkdirs();

        BlockDataExtractor.extractBlockData();
        ItemDataExtractor.extractItemData();

        throw new RuntimeException("Program exited, wiki data has been written.");
    }

    public static void write(WikiData data, String file) throws IOException {
        File outputFile = new File(InjectionConstant.OUTPUT_FOLDER, file);
        FileUtils.write(outputFile, data.output(1), "UTF-8");
    }

    public static void write(WikiData data, ExceptData exceptData, String file) throws IOException {
        File outputFile = new File(InjectionConstant.OUTPUT_FOLDER, file);
        FileUtils.write(outputFile, data.output(1) + "\n=== Except Data ===\n" + exceptData.output(), "UTF-8");
    }

    public static Object getRegistry(String name) {
        return REGISTRY.get(name);
    }
}
