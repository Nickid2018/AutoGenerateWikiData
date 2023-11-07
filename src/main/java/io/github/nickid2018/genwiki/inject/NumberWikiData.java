package io.github.nickid2018.genwiki.inject;

import it.unimi.dsi.fastutil.floats.*;

import java.util.Set;
import java.util.TreeSet;

public class NumberWikiData implements WikiData {

    private final Float2ObjectMap<Set<String>> groups = new Float2ObjectAVLTreeMap<>();

    public void put(String id, float value) {
        groups.computeIfAbsent(value, k -> new TreeSet<>()).add(id);
    }

    public String output(int indent) {
        StringBuilder builder = new StringBuilder();
        String tab = "\t".repeat(indent);
        for (Float2ObjectMap.Entry<Set<String>> entry : groups.float2ObjectEntrySet()) {
            float value = entry.getFloatKey();
            String formatted = String.format("%.3f", value);
            if (formatted.contains("."))
                while (formatted.endsWith("0"))
                    formatted = formatted.substring(0, formatted.length() - 1);
            if (formatted.endsWith("."))
                formatted = formatted.substring(0, formatted.length() - 1);
            builder.append(tab).append("-- ").append(formatted).append("\n");
            for (String id : entry.getValue())
                builder.append(tab).append("['").append(id).append("'] = ").append(formatted).append(",\n");
            builder.append("\n");
        }
        return builder.toString();
    }
}
