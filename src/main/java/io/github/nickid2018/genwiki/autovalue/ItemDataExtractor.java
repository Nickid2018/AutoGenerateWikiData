package io.github.nickid2018.genwiki.autovalue;

import io.github.nickid2018.genwiki.autovalue.wikidata.*;
import io.github.nickid2018.genwiki.InjectionEntrypoint;
import io.github.nickid2018.genwiki.util.LanguageUtils;
import lombok.SneakyThrows;
import net.minecraft.core.component.DataComponents;
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
    private static final NumberWikiData MAX_STACK_SIZE = new NumberWikiData().setFallback(64);
    private static final StringWikiData RARITY = new StringWikiData().setFallback("COMMON");
    private static final StringListWikiData CREATIVE_MODE_TABS = new StringListWikiData();
    private static final NumberWikiData BURN_DURATION = new NumberWikiData().setFallback(0);
    private static final NumberWikiData MAX_DAMAGE = new NumberWikiData().setFallback(0);
    private static final DoubleNumberWikiData FOOD_PROPERTIES = new DoubleNumberWikiData()
        .setFallback(0, 0)
        .setFallbackNil(true);
    private static final AttributeModifiersWikiData ATTRIBUTE_MODIFIERS = new AttributeModifiersWikiData();

    @SneakyThrows
    public static void extractItemData(MinecraftServer serverObj) {
        Map<Item, String> itemKeyMap = new HashMap<>();
        for (ResourceKey<Item> itemKey : BuiltInRegistries.ITEM.registryKeySet()) {
            String itemID = itemKey.location().getPath();
            Item item = BuiltInRegistries.ITEM.getValue(itemKey);
            itemKeyMap.put(item, itemID);

            ItemStack itemStack = item.getDefaultInstance();
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                itemStack.forEachModifier(
                    slot,
                    LanguageUtils.sneakyExceptionBiConsumer((attribute, attributeModifier) -> ATTRIBUTE_MODIFIERS.add(
                        itemID,
                        attribute.unwrapKey().orElseThrow().location().getPath(),
                        slot.getSerializedName(),
                        attributeModifier.amount(),
                        attributeModifier.operation().getSerializedName()
                    ))
                );
            }

            MAX_STACK_SIZE.put(itemID, itemStack.getMaxStackSize());
            MAX_DAMAGE.put(itemID, itemStack.getMaxDamage());
            RARITY.put(itemID, itemStack.getRarity().name());
            BURN_DURATION.put(itemID, serverObj.overworld().fuelValues().values.getOrDefault(item, 0));

            FoodProperties foodProperties = itemStack.getComponents().get(DataComponents.FOOD);
            if (foodProperties != null) {
                FOOD_PROPERTIES.put(itemID, foodProperties.nutrition(), foodProperties.saturation());
            } else
                FOOD_PROPERTIES.put(itemID, 0, 0);
        }

        CreativeModeTabs.tryRebuildTabContents(InjectionEntrypoint.featureFlagSet, true, serverObj.registryAccess());

        for (ResourceKey<CreativeModeTab> key : BuiltInRegistries.CREATIVE_MODE_TAB.registryKeySet()) {
            String tabName = key.location().getPath().toUpperCase();
            if (tabName.equals("SEARCH"))
                continue;
            CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(key);
            Collection<ItemStack> displayItems = tab.getDisplayItems();
            for (ItemStack itemStack : displayItems) {
                CREATIVE_MODE_TABS.putNew(itemKeyMap.get(itemStack.getItem()), tabName);
            }
        }

        for (ResourceKey<Item> itemKey : BuiltInRegistries.ITEM.registryKeySet()) {
            String itemID = itemKey.location().getPath();
            if (!CREATIVE_MODE_TABS.hasKey(itemID))
                CREATIVE_MODE_TABS.put(itemID, List.of());
            else
                CREATIVE_MODE_TABS.sort(itemID);
        }

        WikiData.write(MAX_STACK_SIZE, "item_max_stack_size.txt");
        WikiData.write(RARITY, "item_rarity.txt");
        WikiData.write(CREATIVE_MODE_TABS, "item_creative_mode_tabs.txt");
        WikiData.write(BURN_DURATION, "item_burn_duration.txt");
        WikiData.write(MAX_DAMAGE, "item_max_damage.txt");
        WikiData.write(FOOD_PROPERTIES, "item_food_properties.txt");
        WikiData.write(ATTRIBUTE_MODIFIERS, "item_attribute_modifiers.txt");
    }
}
