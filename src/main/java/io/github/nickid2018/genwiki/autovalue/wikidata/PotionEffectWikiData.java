package io.github.nickid2018.genwiki.autovalue.wikidata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PotionEffectWikiData implements WikiData {
	private final Map<String, PotionEffect> data = new TreeMap<>();

	public void put(String id, String name) {
		data.put(id, new PotionEffect(name, new ArrayList<>(), new ArrayList<>()));
	}

	public void addEffect(String potionEffect, String id, int amplifier, int duration) {
		data.get(potionEffect).effects.add(new EffectInstance(id, amplifier, duration));
	}

	public void addAttributeModifier(String potionEffect, String attribute, double amount, String operation) {
		data.get(potionEffect).attributeModifiers.add(new AttributeModifiersWikiData.AttributeModifier(attribute, amount, operation));
	}

	@Override
	public String output(int indent) {
		StringBuilder builder = new StringBuilder();
		String tab = "\t".repeat(indent);
		for (Map.Entry<String, PotionEffect> potionEntry : data.entrySet()) {
			String id = potionEntry.getKey();
			PotionEffect potion = potionEntry.getValue();
			builder.append(tab).append("['").append(id).append("'] = {\n");
			builder.append(tab).append("\t['name'] = '").append(potion.name).append("',\n");
			if (!potion.effects.isEmpty()) {
				builder.append(tab).append("\t['effects'] = {\n");
				for (EffectInstance effect : potion.effects) {
					builder.append(tab).append("\t\t{\n");
					builder.append(tab).append("\t\t\t['id'] = '").append(effect.id).append("',\n");
					builder.append(tab).append("\t\t\t['amplifier'] = ").append(NumberWikiData.formatValue(effect.amplifier)).append(",\n");
					builder.append(tab).append("\t\t\t['duration'] = ").append(NumberWikiData.formatValue(effect.duration)).append(",\n");
					builder.append(tab).append("\t\t},\n");
				}
				builder.append(tab).append("\t},\n");
			}
			if (!potion.attributeModifiers.isEmpty()) {
				builder.append(tab).append("\t['attributes'] = {\n");
				builder.append(AttributeModifiersWikiData.printAttributeModifiers(indent + 2, potion.attributeModifiers));
				builder.append(tab).append("\t},\n");
			}
			builder.append(tab).append("},\n");
		}
		return builder.toString();
	}


	private record EffectInstance(String id, int amplifier, int duration) {}
	private record PotionEffect(String name, List<EffectInstance> effects, List<AttributeModifiersWikiData.AttributeModifier> attributeModifiers) {}
}
