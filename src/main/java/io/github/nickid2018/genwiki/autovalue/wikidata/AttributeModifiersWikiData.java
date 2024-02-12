package io.github.nickid2018.genwiki.autovalue.wikidata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AttributeModifiersWikiData implements WikiData {

    private final Map<String, Map<String, List<AttributeModifier>>> data = new TreeMap<>();

    public void add(String item, String attribute, String slot, double amount, String operation) {
        data.computeIfAbsent(item, k -> new TreeMap<>())
                .computeIfAbsent(slot, k -> new ArrayList<>())
                .add(new AttributeModifier(attribute, amount, operation));
    }

    public static String formatValue(double value) {
        String formatted = String.format("%.3f", value);
        if (formatted.contains("."))
            while (formatted.endsWith("0"))
                formatted = formatted.substring(0, formatted.length() - 1);
        if (formatted.endsWith("."))
            formatted = formatted.substring(0, formatted.length() - 1);
        return formatted;
    }

    @Override
    public String output(int indent) {
        StringBuilder builder = new StringBuilder();
        String tab = "\t".repeat(indent);
        for (Map.Entry<String, Map<String, List<AttributeModifier>>> itemEntry : data.entrySet()) {
            String item = itemEntry.getKey();
            builder.append(tab).append("['").append(item).append("'] = {\n");
            for (Map.Entry<String, List<AttributeModifier>> slotTypeEntry : itemEntry.getValue().entrySet()) {
                String category = slotTypeEntry.getKey();
                builder.append(tab).append("\t['").append(category).append("'] = {\n");
                for (AttributeModifier entry : slotTypeEntry.getValue()) {
                    builder.append(tab).append("\t\t{\n");
                    builder.append(tab).append("\t\t\t['attribute'] = '").append(entry.attribute).append("',\n");
                    builder.append(tab).append("\t\t\t['amount'] = ").append(formatValue(entry.amount)).append(",\n");
                    builder.append(tab).append("\t\t\t['operation'] = '").append(entry.operation).append("',\n");
                    builder.append(tab).append("\t\t},\n");
                }
                builder.append(tab).append("\t},\n");
            }
            builder.append(tab).append("},\n");
        }
        return builder.toString();
    }

    private record AttributeModifier(String attribute, double amount, String operation) {
    }
}
