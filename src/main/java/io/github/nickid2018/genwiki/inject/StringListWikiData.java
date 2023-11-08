package io.github.nickid2018.genwiki.inject;

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

import java.util.List;

public class StringListWikiData implements WikiData {

    private final Object2ObjectMap<String, List<String>> data = new Object2ObjectAVLTreeMap<>();

    public void put(String id, List<String> value) {
        data.put(id, value);
    }

    public String output(int indent) {
        StringBuilder builder = new StringBuilder();
        String tab = "\t".repeat(indent);
        for (Object2ObjectMap.Entry<String, List<String>> entry : data.object2ObjectEntrySet()) {
            String key = entry.getKey();
            builder.append(tab).append("['").append(key).append("'] = {");
            builder.append(String.join(", ", entry.getValue().stream().map(s -> "'" + s + "'").toList()));
            builder.append("},\n");
        }
        return builder.toString();
    }
}
