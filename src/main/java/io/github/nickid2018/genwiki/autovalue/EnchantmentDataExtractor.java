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
    public static final StringListWikiData ENCHANTMENT_INCOMPATIBLE = new StringListWikiData();
    public static final PairMapWikiData<Integer, Integer> ENCHANTMENT_COST = new PairMapWikiData<>();

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

        WikiData.write(ENCHANTMENT_INCOMPATIBLE, "enchantment/incompatible.txt");
        WikiData.write(ENCHANTMENT_COST, "enchantment/cost.txt");
    }
}
