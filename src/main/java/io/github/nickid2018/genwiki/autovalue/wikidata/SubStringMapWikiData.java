package io.github.nickid2018.genwiki.autovalue.wikidata;

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

import java.util.Map;
import java.util.TreeMap;

public class SubStringMapWikiData implements WikiData {

    private final Object2ObjectMap<String, Map<String, String>> data = new Object2ObjectAVLTreeMap<>();

    public boolean hasKey(String id) {
        return data.containsKey(id);
    }

    public void put(String id, Map<String, String> value) {
        data.put(id, value);
    }

    public void putNew(String id, String key, String value) {
        Map<String, String> val = data.computeIfAbsent(id, k -> new TreeMap<>());
        val.put(key, value);
    }

    public String output(int indent) {
        StringBuilder builder = new StringBuilder();
        String tab = "\t".repeat(indent);
        for (Object2ObjectMap.Entry<String, Map<String, String>> entry : data.object2ObjectEntrySet()) {
            String key = entry.getKey();
            builder.append(tab).append("['").append(key).append("'] = {\n");
            Map<String, String> value = entry.getValue();
            for (Map.Entry<String, String> e : value.entrySet()) {
                builder.append(tab).append("\t['").append(e.getKey()).append("'] = '").append(e.getValue()).append("',\n");
            }
            builder.append(tab).append("},\n");
        }
        return builder.toString();
    }
}
