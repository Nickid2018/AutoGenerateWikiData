package io.github.nickid2018.genwiki.inject;

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Set;
import java.util.TreeSet;

public class StringWikiData implements WikiData {

    private final Object2ObjectMap<String, Set<String>> groups = new Object2ObjectAVLTreeMap<>();

    @Setter
    @Accessors(chain = true)
    private String fallback;

    public void put(String id, String value) {
        if (value.equals(fallback))
            return;
        groups.computeIfAbsent(value, k -> new TreeSet<>()).add(id);
    }

    public String output(int indent) {
        StringBuilder builder = new StringBuilder();
        String tab = "\t".repeat(indent);
        if (fallback != null)
            builder.append(tab).append("['__fallback'] = '").append(fallback).append("',\n\n");
        for (Object2ObjectMap.Entry<String, Set<String>> entry : groups.object2ObjectEntrySet()) {
            String value = entry.getKey();
            builder.append(tab).append("-- ").append(value).append("\n");
            for (String id : entry.getValue())
                builder.append(tab).append("['").append(id).append("'] = '").append(value).append("',\n");
            builder.append("\n");
        }
        return builder.toString();
    }
}
