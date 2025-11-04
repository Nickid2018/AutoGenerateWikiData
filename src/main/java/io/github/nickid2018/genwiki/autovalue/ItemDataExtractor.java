package io.github.nickid2018.genwiki.autovalue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.nickid2018.genwiki.autovalue.wikidata.*;
import io.github.nickid2018.genwiki.InjectionEntrypoint;
import io.github.nickid2018.genwiki.util.LanguageUtils;
import lombok.SneakyThrows;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class ItemDataExtractor {
    private static final StringListWikiData CREATIVE_MODE_TABS = new StringListWikiData();
    private static final NumberWikiData BURN_DURATION = new NumberWikiData().setFallback(0);
    private static final DoubleNumberWikiData FOOD_PROPERTIES = new DoubleNumberWikiData()
        .setFallback(0, 0)
        .setFallbackNil(true);

    @SneakyThrows
    public static void extractItemData(MinecraftServer serverObj) {
        Map<Item, String> itemKeyMap = new HashMap<>();
        for (ResourceKey<Item> itemKey : BuiltInRegistries.ITEM.registryKeySet()) {
            String itemID = itemKey.identifier().getPath();
            Item item = BuiltInRegistries.ITEM.getValue(itemKey);
            itemKeyMap.put(item, itemID);

            BURN_DURATION.put(itemID, serverObj.overworld().fuelValues().values.getOrDefault(item, 0));

            ItemStack itemStack = item.getDefaultInstance();
            FoodProperties foodProperties = itemStack.getComponents().get(DataComponents.FOOD);
            if (foodProperties != null) {
                FOOD_PROPERTIES.put(itemID, foodProperties.nutrition(), foodProperties.saturation());
            } else
                FOOD_PROPERTIES.put(itemID, 0, 0);
        }

        CreativeModeTabs.tryRebuildTabContents(InjectionEntrypoint.featureFlagSet, true, serverObj.registryAccess());

        for (ResourceKey<CreativeModeTab> key : BuiltInRegistries.CREATIVE_MODE_TAB.registryKeySet()) {
            String tabName = key.identifier().getPath().toUpperCase();
            if (tabName.equals("SEARCH"))
                continue;
            CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(key);
            Collection<ItemStack> displayItems = tab.getDisplayItems();
            for (ItemStack itemStack : displayItems) {
                CREATIVE_MODE_TABS.putNew(itemKeyMap.get(itemStack.getItem()), tabName);
            }
        }

        for (ResourceKey<Item> itemKey : BuiltInRegistries.ITEM.registryKeySet()) {
            String itemID = itemKey.identifier().getPath();
            if (!CREATIVE_MODE_TABS.hasKey(itemID))
                CREATIVE_MODE_TABS.put(itemID, List.of());
            else
                CREATIVE_MODE_TABS.sort(itemID);
        }

        WikiData.write(CREATIVE_MODE_TABS, "item/creative_mode_tabs.txt");
        WikiData.write(BURN_DURATION, "item/burn_duration.txt");
        WikiData.write(FOOD_PROPERTIES, "item/food_properties.txt");
    }
}
