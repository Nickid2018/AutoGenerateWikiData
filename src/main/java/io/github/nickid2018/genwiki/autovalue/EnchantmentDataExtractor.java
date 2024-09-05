package io.github.nickid2018.genwiki.autovalue;

import io.github.nickid2018.genwiki.autovalue.wikidata.NumberWikiData;
import io.github.nickid2018.genwiki.autovalue.wikidata.PairMapWikiData;
import io.github.nickid2018.genwiki.autovalue.wikidata.StringListWikiData;
import io.github.nickid2018.genwiki.autovalue.wikidata.WikiData;
import lombok.SneakyThrows;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EnchantmentDataExtractor {
    public static final NumberWikiData ENCHANTMENT_WEIGHT = new NumberWikiData();
    public static final NumberWikiData ENCHANTMENT_MAX_LEVEL = new NumberWikiData();
    public static final StringListWikiData ENCHANTMENT_INCOMPATIBLE = new StringListWikiData();
    public static final PairMapWikiData<Integer, Integer> ENCHANTMENT_COST = new PairMapWikiData<>();
    public static final StringListWikiData ENCHANTMENT_SUPPORT_ITEMS = new StringListWikiData();
    public static final StringListWikiData ENCHANTMENT_PRIMARY_ITEMS = new StringListWikiData();

    @SneakyThrows
    public static void extractEnchantmentData(MinecraftServer server) {
        Registry<Enchantment> enchantmentRegistry = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Set<ResourceKey<Enchantment>> enchantmentKeySet = enchantmentRegistry.registryKeySet();
        Map<String, Enchantment> enchantmentMap = enchantmentKeySet.stream().collect(Collectors.toMap(
            enchantmentResourceKey -> enchantmentResourceKey.location().getPath(),
            enchantmentRegistry::getValue
        ));

        for (Map.Entry<String, Enchantment> entry : enchantmentMap.entrySet()) {
            String name = entry.getKey();
            Enchantment enchantment = entry.getValue();
            ENCHANTMENT_WEIGHT.put(name, enchantment.getWeight());
            ENCHANTMENT_MAX_LEVEL.put(name, enchantment.getMaxLevel());

            List<String> incompatibles = enchantmentMap
                .entrySet().stream()
                .filter(e -> e.getValue() != enchantment)
                .filter(e -> !Enchantment.areCompatible(
                    enchantmentRegistry.wrapAsHolder(enchantment),
                    enchantmentRegistry.wrapAsHolder(e.getValue())
                ))
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());
            ENCHANTMENT_INCOMPATIBLE.put(name, incompatibles);

            for (int i = 1; i <= enchantment.getMaxLevel(); i++)
                ENCHANTMENT_COST.putNew(name, enchantment.getMinCost(i), enchantment.getMaxCost(i));
        }

        for (ResourceKey<Item> itemKey : BuiltInRegistries.ITEM.registryKeySet()) {
            String itemID = itemKey.location().getPath();
            ItemStack itemStack = BuiltInRegistries.ITEM.getValue(itemKey).getDefaultInstance();
            for (Map.Entry<String, Enchantment> enchantmentEntry : enchantmentMap.entrySet()) {
                Enchantment enchantment = enchantmentEntry.getValue();
                String name = enchantmentEntry.getKey();
                if (enchantment.isSupportedItem(itemStack)) {
                    ENCHANTMENT_SUPPORT_ITEMS.putNew(name, itemID);
                    if (enchantment.isPrimaryItem(itemStack))
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
            ENCHANTMENT_INCOMPATIBLE.sort(name);
        }

        WikiData.write(ENCHANTMENT_WEIGHT, "enchantment_weight.txt");
        WikiData.write(ENCHANTMENT_MAX_LEVEL, "enchantment_max_level.txt");
        WikiData.write(ENCHANTMENT_INCOMPATIBLE, "enchantment_incompatible.txt");
        WikiData.write(ENCHANTMENT_COST, "enchantment_cost.txt");
        WikiData.write(ENCHANTMENT_SUPPORT_ITEMS, "enchantment_support_items.txt");
        WikiData.write(ENCHANTMENT_PRIMARY_ITEMS, "enchantment_primary_items.txt");
    }
}
