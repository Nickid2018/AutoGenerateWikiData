package io.github.nickid2018.genwiki.autovalue.wikidata;

import it.unimi.dsi.fastutil.doubles.Double2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectMap;

import java.util.Set;
import java.util.TreeSet;

public class NumberWikiData implements WikiData {

    private final Double2ObjectMap<Set<String>> groups = new Double2ObjectAVLTreeMap<>();

    private boolean enableFallback = false;
    private float fallback = 0;

    public NumberWikiData setFallback(float fallback) {
        enableFallback = true;
        this.fallback = fallback;
        return this;
    }

    public void put(String id, double value) {
        if (enableFallback && value == fallback)
            return;
        groups.computeIfAbsent(value, k -> new TreeSet<>()).add(id);
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

    public String output(int indent) {
        StringBuilder builder = new StringBuilder();
        String tab = "\t".repeat(indent);
        if (enableFallback)
            builder.append(tab).append("['__fallback'] = ").append(formatValue(fallback)).append(",\n\n");
        for (Double2ObjectMap.Entry<Set<String>> entry : groups.double2ObjectEntrySet()) {
            double value = entry.getDoubleKey();
            String formatted = formatValue(value);
            builder.append(tab).append("-- ").append(formatted).append("\n");
            for (String id : entry.getValue())
                builder.append(tab).append("['").append(id).append("'] = ").append(formatted).append(",\n");
            builder.append("\n");
        }
        return builder.toString();
    }
}
