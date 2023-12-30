package io.github.nickid2018.genwiki.autovalue;

import io.github.nickid2018.genwiki.autovalue.wikidata.*;
import io.github.nickid2018.genwiki.inject.InjectedProcess;
import io.github.nickid2018.genwiki.inject.SourceClass;
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
    private static final Class<?> FOOD_PROPERTIES_CLASS;

    private static final MethodHandle ITEM_GET_MAX_STACK_SIZE;
    private static final MethodHandle ITEM_STACK_GET_ITEM;
    private static final MethodHandle ITEM_GET_MAX_DAMAGE;
    private static final MethodHandle ITEM_GET_FOOD_PROPERTIES;
    private static final MethodHandle FOOD_PROPERTIES_NUTRITION;
    private static final MethodHandle FOOD_PROPERTIES_SATURATION_MODIFIER;
    private static final MethodHandle BUILD_TAB_CONTENTS;
    private static final MethodHandle GET_FUEL;

    private static final VarHandle ITEM_RARITY;
    private static final VarHandle CREATIVE_MODE_TAB_DISPLAY_ITEMS;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            ITEM_CLASS = Class.forName("net.minecraft.world.item.Item");
            RARITY_CLASS = Class.forName("net.minecraft.world.item.Rarity");
            CREATIVE_MODE_TABS_CLASS = Class.forName("net.minecraft.world.item.CreativeModeTabs");
            FOOD_PROPERTIES_CLASS = Class.forName("net.minecraft.world.food.FoodProperties");
            Class<?> creativeModeTabClass = Class.forName("net.minecraft.world.item.CreativeModeTab");
            ITEM_GET_MAX_STACK_SIZE = lookup.unreflect(ITEM_CLASS.getMethod("getMaxStackSize"));
            ITEM_GET_MAX_DAMAGE = lookup.unreflect(ITEM_CLASS.getMethod("getMaxDamage"));
            ITEM_GET_FOOD_PROPERTIES = lookup.unreflect(ITEM_CLASS.getMethod("getFoodProperties"));
            FOOD_PROPERTIES_NUTRITION = lookup.unreflect(FOOD_PROPERTIES_CLASS.getMethod("getNutrition"));
            FOOD_PROPERTIES_SATURATION_MODIFIER = lookup.unreflect(FOOD_PROPERTIES_CLASS.getMethod("getSaturationModifier"));
            MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(ITEM_CLASS, lookup);
            ITEM_RARITY = privateLookup.findVarHandle(ITEM_CLASS, "rarity", RARITY_CLASS);
            MethodHandles.Lookup privateLookup2 = MethodHandles.privateLookupIn(creativeModeTabClass, lookup);
            CREATIVE_MODE_TAB_DISPLAY_ITEMS = privateLookup2.findVarHandle(creativeModeTabClass, "displayItems", Collection.class);
            Class<?> itemStackClass = Class.forName("net.minecraft.world.item.ItemStack");
            ITEM_STACK_GET_ITEM = lookup.unreflect(itemStackClass.getMethod("getItem"));
            BUILD_TAB_CONTENTS = lookup.unreflect(CREATIVE_MODE_TABS_CLASS.getMethod("tryRebuildTabContents",
                    Class.forName("net.minecraft.world.flag.FeatureFlagSet"), boolean.class,
                    Class.forName("net.minecraft.core.HolderLookup$Provider")));
            GET_FUEL = lookup.unreflect(
                    Class.forName("net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity").getMethod("getFuel"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final NumberWikiData MAX_STACK_SIZE = new NumberWikiData().setFallback(64);
    private static final StringWikiData RARITY = new StringWikiData().setFallback("COMMON");
    private static final StringListWikiData CREATIVE_MODE_TABS = new StringListWikiData();
    private static final NumberWikiData BURN_DURATION = new NumberWikiData().setFallback(0);
    private static final NumberWikiData MAX_DAMAGE = new NumberWikiData().setFallback(0);
    private static final DoubleNumberWikiData FOOD_PROPERTIES = new DoubleNumberWikiData().setFallback(0, 0).setFallbackNil(true);

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static void extractItemData(Object serverObj) {
        @SourceClass("Map<Item, Integer>")
        Map<?, Integer> fuelMap = (Map<?, Integer>) GET_FUEL.invoke();
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
            int maxDamage = (int) ITEM_GET_MAX_DAMAGE.invoke(item);
            MAX_DAMAGE.put(itemID, maxDamage);
            @SourceClass("Rarity")
            Object rarity = ITEM_RARITY.get(item);
            String rarityName = (String) InjectedProcess.ENUM_NAME.invoke(rarity);
            RARITY.put(itemID, rarityName);
            BURN_DURATION.put(itemID, fuelMap.getOrDefault(item, 0));
            @SourceClass("FoodProperties")
            Object foodProperties = ITEM_GET_FOOD_PROPERTIES.invoke(item);
            if (foodProperties != null) {
                int nutrition = (int) FOOD_PROPERTIES_NUTRITION.invoke(foodProperties);
                float saturationModifier = (float) FOOD_PROPERTIES_SATURATION_MODIFIER.invoke(foodProperties);
                FOOD_PROPERTIES.put(itemID, nutrition, saturationModifier);
            } else
                FOOD_PROPERTIES.put(itemID, 0, 0);
        }

        Map<String, Object> creativeModeTabs = new TreeMap<>();
        Field[] fields = CREATIVE_MODE_TABS_CLASS.getDeclaredFields();
        for (Field field : fields) {
            if (field.getType() == InjectedProcess.RESOURCE_KEY_CLASS) {
                field.setAccessible(true);
                creativeModeTabs.put(field.getName(), field.get(null));
            }
        }

        Object serverOverworld = InjectedProcess.SERVER_OVERWORLD.invoke(serverObj);
        @SourceClass("RegistryAccess")
        Object registryAccess = InjectedProcess.REGISTRY_ACCESS.invoke(serverOverworld);
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

        for (Object itemKey : itemKeySet) {
            @SourceClass("ResourceLocation")
            Object itemLocation = InjectedProcess.RESOURCE_KEY_LOCATION.invoke(itemKey);
            String itemID = InjectedProcess.getResourceLocationPath(itemLocation);
            if (!CREATIVE_MODE_TABS.hasKey(itemID))
                CREATIVE_MODE_TABS.put(itemID, List.of());
        }

        WikiData.write(MAX_STACK_SIZE, "item_max_stack_size.txt");
        WikiData.write(RARITY, "item_rarity.txt");
        WikiData.write(CREATIVE_MODE_TABS, "item_creative_mode_tabs.txt");
        WikiData.write(BURN_DURATION, "item_burn_duration.txt");
        WikiData.write(MAX_DAMAGE, "item_max_damage.txt");
        WikiData.write(FOOD_PROPERTIES, "item_food_properties.txt");
    }
}
