package io.github.nickid2018.genwiki.autovalue;

import com.google.common.reflect.ClassPath;
import io.github.nickid2018.genwiki.autovalue.wikidata.*;
import io.github.nickid2018.genwiki.util.LanguageUtils;
import lombok.SneakyThrows;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.alchemy.Potion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class EntityDataExtractor {
    private static final StringWikiData MOB_CATEGORY = new StringWikiData();

    public static final StringWikiData EFFECT_CATEGORY = new StringWikiData();
    public static final ColorWikiData EFFECT_COLOR = new ColorWikiData();
    public static final BooleanWikiData EFFECT_INSTANTANEOUS = new BooleanWikiData();
    public static final StringListWikiData EFFECT_CANNOT_AFFECT = new StringListWikiData();

    public static final StringWikiData ATTRIBUTE_SENTIMENT = new StringWikiData();
    public static final DoubleNumberWikiData ATTRIBUTE_RANGE = new DoubleNumberWikiData();
    public static final NumberWikiData ATTRIBUTE_DEFAULT_VALUE = new NumberWikiData();

    public static final PotionEffectWikiData POTION_EFFECT = new PotionEffectWikiData();

    public static final EntitySyncWikiData ENTITY_SYNC = new EntitySyncWikiData();
    private static final Logger log = LoggerFactory.getLogger(EntityDataExtractor.class);

    @SneakyThrows
    public static void extractEntityData(MinecraftServer serverObj) {
        Map<String, LivingEntity> livingEntityMap = new HashMap<>();

        Map<Object, String> entitySyncDataNames = new HashMap<>();
        ClassPath
            .from(Entity.class.getClassLoader())
            .getTopLevelClassesRecursive("net.minecraft.world.entity")
            .stream()
            .map(ClassPath.ClassInfo::load)
            .flatMap(clazz -> {
                ArrayList<Class<?>> classes = new ArrayList<>();
                classes.add(clazz);
                classes.addAll(Arrays.asList(clazz.getClasses()));
                return classes.stream();
            })
            .filter(Entity.class::isAssignableFrom)
            .flatMap(clazz -> Arrays.stream(clazz.getDeclaredFields()))
            .filter(field -> EntityDataAccessor.class.isAssignableFrom(field.getType()))
            .peek(field -> field.setAccessible(true))
            .forEach(field -> {
                try {
                    EntityDataAccessor<?> accessor = (EntityDataAccessor<?>) field.get(null);
                    entitySyncDataNames.put(accessor, field.getName());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            });
        log.info("Found {} entity sync data fields", entitySyncDataNames.size());

        for (ResourceKey<EntityType<?>> key : BuiltInRegistries.ENTITY_TYPE.registryKeySet()) {
            String entityID = key.location().getPath();

            EntityType<?> entity = BuiltInRegistries.ENTITY_TYPE.get(key);
            MOB_CATEGORY.put(entityID, entity.getCategory().name());

            Entity entityInstance = entity.spawn(serverObj.overworld(), BlockPos.ZERO, MobSpawnType.COMMAND);
            if (entityInstance instanceof LivingEntity livingEntity)
                livingEntityMap.put(entityID, livingEntity);

            if (entityInstance != null)
                ENTITY_SYNC.put(
                    entityID,
                    Arrays
                        .stream(entityInstance.getEntityData().itemsById)
                        .map(item -> item.accessor)
                        .map(entitySyncDataNames::get)
                        .toArray(String[]::new)
                );
        }

        for (ResourceKey<MobEffect> effectKey : BuiltInRegistries.MOB_EFFECT.registryKeySet()) {
            String effectID = effectKey.location().getPath();
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectKey);

            EFFECT_CATEGORY.put(effectID, effect.getCategory().name());
            EFFECT_COLOR.put(effectID, effect.getColor());
            EFFECT_INSTANTANEOUS.put(effectID, effect.isInstantenous());

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

        for (ResourceKey<Attribute> attributeKey : BuiltInRegistries.ATTRIBUTE.registryKeySet()) {
            String attributeID = attributeKey.location().getPath();
            Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(attributeKey);
            ATTRIBUTE_SENTIMENT.put(attributeID, attribute.sentiment.name());
            ATTRIBUTE_DEFAULT_VALUE.put(attributeID, attribute.defaultValue);
            if (attribute instanceof RangedAttribute rangedAttribute) {
                double minValue = rangedAttribute.getMinValue();
                double maxValue = rangedAttribute.getMaxValue();
                ATTRIBUTE_RANGE.put(attributeID, minValue, maxValue);
            }
        }

        WikiData.write(MOB_CATEGORY, "entity_mob_category.txt");
        WikiData.write(EFFECT_CATEGORY, "mob_effect_category.txt");
        WikiData.write(EFFECT_COLOR, "mob_effect_color.txt");
        WikiData.write(EFFECT_INSTANTANEOUS, "mob_effect_instantaneous.txt");
        WikiData.write(EFFECT_CANNOT_AFFECT, "mob_effect_cannot_affect.txt");
        WikiData.write(POTION_EFFECT, "potion_effect.txt");
        WikiData.write(ATTRIBUTE_SENTIMENT, "attribute_sentiment.txt");
        WikiData.write(ATTRIBUTE_RANGE, "attribute_range.txt");
        WikiData.write(ATTRIBUTE_DEFAULT_VALUE, "attribute_default_value.txt");
        WikiData.write(ENTITY_SYNC, "entity_sync.json");
    }
}
