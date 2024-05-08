package io.github.nickid2018.genwiki.autovalue;

import io.github.nickid2018.genwiki.autovalue.wikidata.NumberWikiData;
import io.github.nickid2018.genwiki.autovalue.wikidata.PairMapWikiData;
import io.github.nickid2018.genwiki.autovalue.wikidata.StringListWikiData;
import io.github.nickid2018.genwiki.autovalue.wikidata.WikiData;
import io.github.nickid2018.genwiki.inject.InjectedProcess;
import io.github.nickid2018.genwiki.inject.SourceClass;
import lombok.SneakyThrows;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EnchantmentDataExtractor {

    public static final Class<?> ENCHANTMENT_CLASS;
    public static final Class<?> ITEM_STACK_CLASS;
    public static final Class<?> ITEM_CLASS;
    public static final Class<?> TAG_KEY_CLASS;

    public static final MethodHandle ENCHANTMENT_GET_WEIGHT;
    public static final MethodHandle ENCHANTMENT_GET_MAX_LEVEL;
    public static final MethodHandle ENCHANTMENT_ARE_COMPATIBLE;
    public static final MethodHandle ENCHANTMENT_GET_MIN_COST;
    public static final MethodHandle ENCHANTMENT_GET_MAX_COST;
    public static final MethodHandle ENCHANTMENT_IS_SUPPORTED_ITEM;
    public static final MethodHandle ENCHANTMENT_IS_PRIMARY_ITEM;
    public static final MethodHandle ITEM_GET_DEFAULT_INSTANCE;
    public static final MethodHandle ITEM_STACK_IS;

    static {
        try {
            ENCHANTMENT_CLASS = Class.forName("net.minecraft.world.item.enchantment.Enchantment");
            ITEM_STACK_CLASS = Class.forName("net.minecraft.world.item.ItemStack");
            ITEM_CLASS = Class.forName("net.minecraft.world.item.Item");
            TAG_KEY_CLASS = Class.forName("net.minecraft.tags.TagKey");
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            ENCHANTMENT_GET_WEIGHT = lookup.unreflect(ENCHANTMENT_CLASS.getMethod("getWeight"));
            ENCHANTMENT_GET_MAX_LEVEL = lookup.unreflect(ENCHANTMENT_CLASS.getMethod("getMaxLevel"));
            ENCHANTMENT_ARE_COMPATIBLE = lookup.unreflect(ENCHANTMENT_CLASS.getMethod(
                "areCompatible",
                InjectedProcess.HOLDER_CLASS,
                InjectedProcess.HOLDER_CLASS
            ));

            ENCHANTMENT_GET_MIN_COST = lookup.unreflect(ENCHANTMENT_CLASS.getMethod("getMinCost", int.class));
            ENCHANTMENT_GET_MAX_COST = lookup.unreflect(ENCHANTMENT_CLASS.getMethod("getMaxCost", int.class));
            ENCHANTMENT_IS_SUPPORTED_ITEM = lookup.unreflect(ENCHANTMENT_CLASS.getMethod(
                "isSupportedItem",
                ITEM_STACK_CLASS
            ));
            ENCHANTMENT_IS_PRIMARY_ITEM = lookup.unreflect(ENCHANTMENT_CLASS.getMethod(
                "isPrimaryItem",
                ITEM_STACK_CLASS
            ));

            ITEM_GET_DEFAULT_INSTANCE = lookup.unreflect(ITEM_CLASS.getMethod("getDefaultInstance"));
            ITEM_STACK_IS = lookup.unreflect(ITEM_STACK_CLASS.getMethod("is", TAG_KEY_CLASS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static final NumberWikiData ENCHANTMENT_WEIGHT = new NumberWikiData();
    public static final NumberWikiData ENCHANTMENT_MAX_LEVEL = new NumberWikiData();
    public static final StringListWikiData ENCHANTMENT_INCOMPATIBLE = new StringListWikiData();
    public static final PairMapWikiData<Integer, Integer> ENCHANTMENT_COST = new PairMapWikiData<>();
    public static final StringListWikiData ENCHANTMENT_SUPPORT_ITEMS = new StringListWikiData();
    public static final StringListWikiData ENCHANTMENT_PRIMARY_ITEMS = new StringListWikiData();

    @SneakyThrows
    private static String getRegistryName(Object enchantment) {
        return InjectedProcess.getResourceLocationPath(InjectedProcess.RESOURCE_KEY_LOCATION.invoke(enchantment));
    }

    @SneakyThrows
    private static Object getEnchantmentObject(Object registry, Object enchantment) {
        return InjectedProcess.REGISTRY_GET.invoke(registry, enchantment);
    }

    @SneakyThrows
    public static void extractEnchantmentData(Object server) {
        @SourceClass("Registry<Enchantment>")
        Object enchantmentRegistry = InjectedProcess.getServerSyncRegistry(server, "ENCHANTMENT");
        @SourceClass("Set<ResourceKey<Enchantment>>")
        Set<?> enchantmentKeySet = InjectedProcess.getRegistryKeySet(enchantmentRegistry);
        Map<String, ?> enchantmentMap = enchantmentKeySet.stream().collect(Collectors.toMap(
            EnchantmentDataExtractor::getRegistryName,
            enchantment -> getEnchantmentObject(enchantmentRegistry, enchantment)
        ));

        for (Map.Entry<String, ?> entry : enchantmentMap.entrySet()) {
            String name = entry.getKey();
            Object enchantment = entry.getValue();
            int rarity = (int) ENCHANTMENT_GET_WEIGHT.invoke(enchantment);
            ENCHANTMENT_WEIGHT.put(name, rarity);
            int maxLevel = (int) ENCHANTMENT_GET_MAX_LEVEL.invoke(enchantment);
            ENCHANTMENT_MAX_LEVEL.put(name, maxLevel);

            List<String> incompatibles = enchantmentMap
                .entrySet().stream()
                .filter(e -> e.getValue() != enchantment)
                .filter(e -> {
                    Object other = e.getValue();
                    try {
                        return !(boolean) ENCHANTMENT_ARE_COMPATIBLE.invoke(
                            InjectedProcess.wrapAsHolder(enchantmentRegistry, enchantment),
                            InjectedProcess.wrapAsHolder(enchantmentRegistry, other)
                        );
                    } catch (Throwable throwable) {
                        throw new RuntimeException(throwable);
                    }
                })
                .map(Map.Entry::getKey).sorted()
                .collect(Collectors.toList());
            ENCHANTMENT_INCOMPATIBLE.put(name, incompatibles);

            for (int i = 1; i <= maxLevel; i++) {
                int minCost = (int) ENCHANTMENT_GET_MIN_COST.invoke(enchantment, i);
                int maxCost = (int) ENCHANTMENT_GET_MAX_COST.invoke(enchantment, i);
                ENCHANTMENT_COST.putNew(name, minCost, maxCost);
            }
        }

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
            @SourceClass("ItemStack")
            Object itemStack = ITEM_GET_DEFAULT_INSTANCE.invoke(item);
            for (Map.Entry<String, ?> enchantmentEntry : enchantmentMap.entrySet()) {
                Object enchantment = enchantmentEntry.getValue();
                String name = enchantmentEntry.getKey();
                boolean support = (boolean) ENCHANTMENT_IS_SUPPORTED_ITEM.invoke(enchantment, itemStack);
                if (support) {
                    ENCHANTMENT_SUPPORT_ITEMS.putNew(name, itemID);
                    boolean isPrimary = (boolean) ENCHANTMENT_IS_PRIMARY_ITEM.invoke(enchantment, itemStack);
                    if (isPrimary)
                        ENCHANTMENT_PRIMARY_ITEMS.putNew(name, itemID);
                }
            }
        }

        for (String name : enchantmentMap.keySet()) {
            if (!ENCHANTMENT_SUPPORT_ITEMS.hasKey(name))
                ENCHANTMENT_SUPPORT_ITEMS.put(name, new ArrayList<>());
            if (!ENCHANTMENT_PRIMARY_ITEMS.hasKey(name))
                ENCHANTMENT_PRIMARY_ITEMS.put(name, new ArrayList<>());
            ENCHANTMENT_SUPPORT_ITEMS.sort(name);
            ENCHANTMENT_PRIMARY_ITEMS.sort(name);
        }

        WikiData.write(ENCHANTMENT_WEIGHT, "enchantment_weight.txt");
        WikiData.write(ENCHANTMENT_MAX_LEVEL, "enchantment_max_level.txt");
        WikiData.write(ENCHANTMENT_INCOMPATIBLE, "enchantment_incompatible.txt");
        WikiData.write(ENCHANTMENT_COST, "enchantment_cost.txt");
        WikiData.write(ENCHANTMENT_SUPPORT_ITEMS, "enchantment_support_items.txt");
        WikiData.write(ENCHANTMENT_PRIMARY_ITEMS, "enchantment_primary_items.txt");
    }
}
