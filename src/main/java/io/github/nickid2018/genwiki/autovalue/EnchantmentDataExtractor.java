package io.github.nickid2018.genwiki.autovalue;

import io.github.nickid2018.genwiki.autovalue.wikidata.PairMapWikiData;
import io.github.nickid2018.genwiki.autovalue.wikidata.WikiData;
import lombok.SneakyThrows;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EnchantmentDataExtractor {
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
            for (int i = 1; i <= enchantment.getMaxLevel(); i++)
                ENCHANTMENT_COST.putNew(name, enchantment.getMinCost(i), enchantment.getMaxCost(i));
        }

        WikiData.write(ENCHANTMENT_COST, "enchantment/cost.txt");
    }
}
