package io.github.nickid2018.genwiki.inject;

import lombok.SneakyThrows;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Set;

public class ItemDataExtractor {

    private static final Class<?> ITEM_CLASS;
    private static final Class<?> RARITY_CLASS;

    private static final MethodHandle ITEM_GET_MAX_STACK_SIZE;

    private static final VarHandle ITEM_RARITY;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            ITEM_CLASS = Class.forName("net.minecraft.world.item.Item");
            RARITY_CLASS = Class.forName("net.minecraft.world.item.Rarity");
            ITEM_GET_MAX_STACK_SIZE = lookup.unreflect(ITEM_CLASS.getMethod("getMaxStackSize"));
            MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(ITEM_CLASS, lookup);
            ITEM_RARITY = privateLookup.findVarHandle(ITEM_CLASS, "rarity", RARITY_CLASS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final NumberWikiData MAX_STACK_SIZE = new NumberWikiData().setFallback(64);
    private static final StringWikiData RARITY = new StringWikiData().setFallback("COMMON");

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
            @SourceClass("Rarity")
            Object rarity = ITEM_RARITY.get(item);
            String rarityName = (String) InjectedProcess.ENUM_NAME.invoke(rarity);
            RARITY.put(itemID, rarityName);
        }

        InjectedProcess.write(MAX_STACK_SIZE, "item_max_stack_size.txt");
        InjectedProcess.write(RARITY, "item_rarity.txt");
    }
}
