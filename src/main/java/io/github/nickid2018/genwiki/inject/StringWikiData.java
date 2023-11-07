package io.github.nickid2018.genwiki.inject;

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

import java.util.Set;
import java.util.TreeSet;

public class StringWikiData implements WikiData {

    private final Object2ObjectMap<String, Set<String>> groups = new Object2ObjectAVLTreeMap<>();

    public void put(String id, String value) {
        groups.computeIfAbsent(value, k -> new TreeSet<>()).add(id);
    }

    public String output(int indent) {
        StringBuilder builder = new StringBuilder();
        String tab = "\t".repeat(indent);
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
