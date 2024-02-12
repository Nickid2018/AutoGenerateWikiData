package io.github.nickid2018.genwiki.autovalue.wikidata;

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

public class PropertyWikiData implements WikiData {
    private final Object2ObjectMap<String, List<String>> data = new Object2ObjectAVLTreeMap<>();
    private final Object2ObjectMap<String, String> names = new Object2ObjectAVLTreeMap<>();

    public void put(String id, String name) {
        names.put(id, name);
    }

    public void putNew(String id, String value) {
        List<String> val = data.computeIfAbsent(id, k -> new ArrayList<>());
        if (!val.contains(value))
            val.add(value);
    }

    public String output(int indent) {
        StringBuilder builder = new StringBuilder();
        String tab = "\t".repeat(indent);
        for (Object2ObjectMap.Entry<String, List<String>> entry : data.object2ObjectEntrySet()) {
            String key = entry.getKey();
            builder.append(tab).append("['").append(key).append("'] = {");
            builder.append("'").append(names.get(key)).append("', {");
            builder.append(String.join(", ", entry.getValue().stream().map(s -> "'" + s + "'").toList()));
            builder.append("}},\n");
        }
        return builder.toString();
    }
}
