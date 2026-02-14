package io.github.nickid2018.genwiki.autovalue.wikidata;

public record AttributeModifierData(String attribute, double amount, String operation) {
    public static String formatValue(double value) {
        String formatted = String.format("%.3f", value);
        if (formatted.contains("."))
            while (formatted.endsWith("0"))
                formatted = formatted.substring(0, formatted.length() - 1);
        if (formatted.endsWith("."))
            formatted = formatted.substring(0, formatted.length() - 1);
        return formatted;
    }

    public static String printAttributeModifiers(int indent, Iterable<AttributeModifierData> attributeModifiers) {
        StringBuilder builder = new StringBuilder();
        String tab = "\t".repeat(indent);
        for (AttributeModifierData entry : attributeModifiers) {
            builder.append(tab).append("{\n");
            builder.append(tab).append("\t['attribute'] = '").append(entry.attribute).append("',\n");
            builder.append(tab).append("\t['amount'] = ").append(formatValue(entry.amount)).append(",\n");
            builder.append(tab).append("\t['operation'] = '").append(entry.operation).append("',\n");
            builder.append(tab).append("},\n");
        }
        return builder.toString();
    }
}
