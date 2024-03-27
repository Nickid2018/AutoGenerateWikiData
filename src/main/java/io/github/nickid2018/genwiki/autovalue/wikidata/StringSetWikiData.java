package io.github.nickid2018.genwiki.autovalue.wikidata;

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class StringSetWikiData implements WikiData {

    private final Object2ObjectMap<String, TreeSet<String>> data = new Object2ObjectAVLTreeMap<>();

    public boolean hasKey(String id) {
        return data.containsKey(id);
    }

    public void put(String id) {
        data.put(id, new TreeSet<>());
    }

    public void put(String id, TreeSet<String> value) {
        data.put(id, value);
    }

    public void putNew(String id, String value) {
        TreeSet<String> val = data.computeIfAbsent(id, k -> new TreeSet<>());
        val.add(value);
    }

    public TreeSet<String> get(String id) {
        return data.get(id);
    }

    public String output(int indent) {
        StringBuilder builder = new StringBuilder();
        String tab = "\t".repeat(indent);
        for (Object2ObjectMap.Entry<String, TreeSet<String>> entry : data.object2ObjectEntrySet()) {
            String key = entry.getKey();
            builder.append(tab).append("['").append(key).append("'] = {");
            builder.append(String.join(", ", entry.getValue().stream().map(s -> "'" + s + "'").toList()));
            builder.append("},\n");
        }
        return builder.toString();
    }
}
