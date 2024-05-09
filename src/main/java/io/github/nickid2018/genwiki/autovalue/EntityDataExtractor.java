package io.github.nickid2018.genwiki.autovalue;

import io.github.nickid2018.genwiki.autovalue.wikidata.*;
import io.github.nickid2018.genwiki.util.LanguageUtils;
import lombok.SneakyThrows;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.alchemy.Potion;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class EntityDataExtractor {
    private static final StringWikiData MOB_CATEGORY = new StringWikiData();

    public static final StringWikiData EFFECT_CATEGORY = new StringWikiData();
    public static final ColorWikiData EFFECT_COLOR = new ColorWikiData();
    public static final BooleanWikiData EFFECT_INSTANTENOUS = new BooleanWikiData();
    public static final StringListWikiData EFFECT_CANNOT_AFFECT = new StringListWikiData();

    public static final PotionEffectWikiData POTION_EFFECT = new PotionEffectWikiData();

    @SneakyThrows
    public static void extractEntityData(MinecraftServer serverObj) {
        Map<String, LivingEntity> livingEntityMap = new HashMap<>();
        for (ResourceKey<EntityType<?>> key : BuiltInRegistries.ENTITY_TYPE.registryKeySet()) {
            String entityID = key.location().getPath();

            EntityType<?> entity = BuiltInRegistries.ENTITY_TYPE.get(key);
            MOB_CATEGORY.put(entityID, entity.getCategory().name());

            Entity entityInstance = entity.spawn(serverObj.overworld(), BlockPos.ZERO, MobSpawnType.COMMAND);
            if (entityInstance instanceof LivingEntity livingEntity)
                livingEntityMap.put(entityID, livingEntity);
        }

        for (ResourceKey<MobEffect> effectKey : BuiltInRegistries.MOB_EFFECT.registryKeySet()) {
            String effectID = effectKey.location().getPath();
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectKey);

            EFFECT_CATEGORY.put(effectID, effect.getCategory().name());
            EFFECT_COLOR.put(effectID, effect.getColor());
            EFFECT_INSTANTENOUS.put(effectID, effect.isInstantenous());

            EFFECT_CANNOT_AFFECT.put(effectID);
            for (Map.Entry<String, LivingEntity> entry : livingEntityMap.entrySet()) {
                String entityID = entry.getKey();
                if (!entry.getValue().addEffect(
                    new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect))
                ))
                    EFFECT_CANNOT_AFFECT.putNew(effectID, entityID);
            }
            EFFECT_CANNOT_AFFECT.get(effectID).sort(Comparator.naturalOrder());
        }

        for (ResourceKey<Potion> potionEffectKey : BuiltInRegistries.POTION.registryKeySet()) {
            String potionEffectID = potionEffectKey.location().getPath();
            Potion potionEffect = BuiltInRegistries.POTION.get(potionEffectKey);

            POTION_EFFECT.put(
                potionEffectID,
                Potion.getName(BuiltInRegistries.POTION.getHolder(potionEffectKey), "")
            );

            for (MobEffectInstance effectInstance : potionEffect.getEffects()) {
                Holder<MobEffect> effect = effectInstance.getEffect();
                int amplifier = effectInstance.getAmplifier();
                POTION_EFFECT.addEffect(
                    potionEffectID,
                    effect.unwrapKey().orElseThrow().location().getPath(),
                    amplifier,
                    effectInstance.getDuration()
                );
                effect.value().createModifiers(
                    amplifier,
                    LanguageUtils.sneakyExceptionBiConsumer((attribute, attributeModifier) -> POTION_EFFECT.addAttributeModifier(
                        potionEffectID,
                        attribute.unwrapKey().orElseThrow().location().getPath(),
                        attributeModifier.amount(),
                        attributeModifier.operation().getSerializedName()
                    ))
                );
            }
        }

        WikiData.write(MOB_CATEGORY, "entity_mob_category.txt");
        WikiData.write(EFFECT_CATEGORY, "mob_effect_category.txt");
        WikiData.write(EFFECT_COLOR, "mob_effect_color.txt");
        WikiData.write(EFFECT_INSTANTENOUS, "mob_effect_instantenous.txt");
        WikiData.write(EFFECT_CANNOT_AFFECT, "mob_effect_cannot_affect.txt");
        WikiData.write(POTION_EFFECT, "potion_effect.txt");
    }
}
