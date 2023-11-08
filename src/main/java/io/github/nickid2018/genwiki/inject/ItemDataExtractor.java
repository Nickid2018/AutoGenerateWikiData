package io.github.nickid2018.genwiki.inject;

import lombok.SneakyThrows;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Set;

public class ItemDataExtractor {

    private static final Class<?> ITEM_CLASS;

    private static final MethodHandle ITEM_GET_MAX_STACK_SIZE;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            ITEM_CLASS = Class.forName("net.minecraft.world.item.Item");
            ITEM_GET_MAX_STACK_SIZE = lookup.unreflect(ITEM_CLASS.getMethod("getMaxStackSize"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final NumberWikiData MAX_STACK_SIZE = new NumberWikiData();

    @SneakyThrows
    public static void extractItemData() {
        @SourceClass("DefaultedRegistry<Item>")
        Object itemRegistry = InjectedProcess.getRegistry("ITEM");
        @SourceClass("Set<ResourceKey<Item>>")
        Set<?> itemKeySet = InjectedProcess.getRegistryKeySet(itemRegistry);
        for (Object itemKey : itemKeySet) {
            @SourceClass("ResourceLocation")
            Object itemLocation = InjectedProcess.RESOURCE_KEY_LOCATION.invoke(itemKey);
            String itemID = InjectedProcess.getResourceLocationPath(itemLocation);
            @SourceClass("Item")
            Object item = InjectedProcess.REGISTRY_GET.invoke(itemRegistry, itemKey);
            int maxStackSize = (int) ITEM_GET_MAX_STACK_SIZE.invoke(item);
            MAX_STACK_SIZE.put(itemID, maxStackSize);
        }

        InjectedProcess.write(MAX_STACK_SIZE, "item_max_stack_size.txt");
    }
}
