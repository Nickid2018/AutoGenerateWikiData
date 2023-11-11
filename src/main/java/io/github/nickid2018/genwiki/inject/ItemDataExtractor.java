package io.github.nickid2018.genwiki.inject;

import lombok.SneakyThrows;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.*;

public class ItemDataExtractor {

    private static final Class<?> ITEM_CLASS;
    private static final Class<?> RARITY_CLASS;
    private static final Class<?> CREATIVE_MODE_TABS_CLASS;

    private static final MethodHandle ITEM_GET_MAX_STACK_SIZE;
    private static final MethodHandle ITEM_STACK_GET_ITEM;
    private static final MethodHandle SERVER_OVERWORLD;
    private static final MethodHandle REGISTRY_ACCESS;
    private static final MethodHandle BUILD_TAB_CONTENTS;

    private static final VarHandle ITEM_RARITY;
    private static final VarHandle CREATIVE_MODE_TAB_DISPLAY_ITEMS;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            ITEM_CLASS = Class.forName("net.minecraft.world.item.Item");
            RARITY_CLASS = Class.forName("net.minecraft.world.item.Rarity");
            CREATIVE_MODE_TABS_CLASS = Class.forName("net.minecraft.world.item.CreativeModeTabs");
            Class<?> creativeModeTabClass = Class.forName("net.minecraft.world.item.CreativeModeTab");
            ITEM_GET_MAX_STACK_SIZE = lookup.unreflect(ITEM_CLASS.getMethod("getMaxStackSize"));
            MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(ITEM_CLASS, lookup);
            ITEM_RARITY = privateLookup.findVarHandle(ITEM_CLASS, "rarity", RARITY_CLASS);
            MethodHandles.Lookup privateLookup2 = MethodHandles.privateLookupIn(creativeModeTabClass, lookup);
            CREATIVE_MODE_TAB_DISPLAY_ITEMS = privateLookup2.findVarHandle(creativeModeTabClass, "displayItems", Collection.class);
            Class<?> itemStackClass = Class.forName("net.minecraft.world.item.ItemStack");
            ITEM_STACK_GET_ITEM = lookup.unreflect(itemStackClass.getMethod("getItem"));
            Class<?> minecraftServerClass = Class.forName("net.minecraft.server.MinecraftServer");
            SERVER_OVERWORLD = lookup.unreflect(minecraftServerClass.getMethod("overworld"));
            Class<?> registryAccessClass = Class.forName("net.minecraft.world.level.Level");
            REGISTRY_ACCESS = lookup.unreflect(registryAccessClass.getMethod("registryAccess"));
            BUILD_TAB_CONTENTS = lookup.unreflect(CREATIVE_MODE_TABS_CLASS.getMethod("tryRebuildTabContents",
                    Class.forName("net.minecraft.world.flag.FeatureFlagSet"), boolean.class,
                    Class.forName("net.minecraft.core.HolderLookup$Provider")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final NumberWikiData MAX_STACK_SIZE = new NumberWikiData().setFallback(64);
    private static final StringWikiData RARITY = new StringWikiData().setFallback("COMMON");
    private static final StringListWikiData CREATIVE_MODE_TABS = new StringListWikiData();

    @SneakyThrows
    public static void extractItemData(Object serverObj) {
        @SourceClass("DefaultedRegistry<Item>")
        Object itemRegistry = InjectedProcess.getRegistry("ITEM");
        @SourceClass("Set<ResourceKey<Item>>")
        Set<?> itemKeySet = InjectedProcess.getRegistryKeySet(itemRegistry);
        Map<Object, String> itemKeyMap = new HashMap<>();
        for (Object itemKey : itemKeySet) {
            @SourceClass("ResourceLocation")
            Object itemLocation = InjectedProcess.RESOURCE_KEY_LOCATION.invoke(itemKey);
            String itemID = InjectedProcess.getResourceLocationPath(itemLocation);
            @SourceClass("Item")
            Object item = InjectedProcess.REGISTRY_GET.invoke(itemRegistry, itemKey);
            itemKeyMap.put(item, itemID);
            int maxStackSize = (int) ITEM_GET_MAX_STACK_SIZE.invoke(item);
            MAX_STACK_SIZE.put(itemID, maxStackSize);
            @SourceClass("Rarity")
            Object rarity = ITEM_RARITY.get(item);
            String rarityName = (String) InjectedProcess.ENUM_NAME.invoke(rarity);
            RARITY.put(itemID, rarityName);
        }

        Map<String, Object> creativeModeTabs = new TreeMap<>();
        Field[] fields = CREATIVE_MODE_TABS_CLASS.getDeclaredFields();
        for (Field field : fields) {
            if (field.getType() == InjectedProcess.RESOURCE_KEY_CLASS) {
                field.setAccessible(true);
                creativeModeTabs.put(field.getName(), field.get(null));
            }
        }

        Object serverOverworld = SERVER_OVERWORLD.invoke(serverObj);
        @SourceClass("RegistryAccess")
        Object registryAccess = REGISTRY_ACCESS.invoke(serverOverworld);
        BUILD_TAB_CONTENTS.invoke(InjectedProcess.featureFlagSet, true, registryAccess);

        @SourceClass("Registry<CreativeModeTab>")
        Object creativeTabRegistry = InjectedProcess.getRegistry("CREATIVE_MODE_TAB");
        for (Map.Entry<String, Object> entry : creativeModeTabs.entrySet()) {
            String tabName = entry.getKey();
            if (tabName.equals("SEARCH"))
                continue;
            @SourceClass("CreativeModeTab")
            Object tab = InjectedProcess.REGISTRY_GET.invoke(creativeTabRegistry, entry.getValue());
            Collection<?> displayItems = (Collection<?>) CREATIVE_MODE_TAB_DISPLAY_ITEMS.get(tab);
            for (Object itemStack : displayItems) {
                @SourceClass("Item")
                Object item = ITEM_STACK_GET_ITEM.invoke(itemStack);
                String itemID = itemKeyMap.get(item);
                CREATIVE_MODE_TABS.putNew(itemID, tabName);
            }
        }

        InjectedProcess.write(MAX_STACK_SIZE, "item_max_stack_size.txt");
        InjectedProcess.write(RARITY, "item_rarity.txt");
        InjectedProcess.write(CREATIVE_MODE_TABS, "item_creative_mode_tabs.txt");
    }
}
