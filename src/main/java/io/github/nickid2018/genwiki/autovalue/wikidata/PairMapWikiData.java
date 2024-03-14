package io.github.nickid2018.genwiki.autovalue.wikidata;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;

import java.util.ArrayList;
import java.util.List;

public class PairMapWikiData<T1, T2> implements WikiData {

    private final Object2ObjectMap<String, List<Pair<T1, T2>>> data = new Object2ObjectAVLTreeMap<>();

    public boolean hasKey(String id) {
        return data.containsKey(id);
    }

    public void put(String id) {
        data.put(id, new ArrayList<>());
    }

    public void put(String id, List<Pair<T1, T2>> value) {
        data.put(id, value);
    }

    public void putNew(String id, T1 a, T2 b) {
        List<Pair<T1, T2>> val = data.computeIfAbsent(id, k -> new ArrayList<>());
        Pair<T1, T2> value = new ObjectObjectImmutablePair<>(a, b);
        if (!val.contains(value))
            val.add(value);
    }

    public String output(int indent) {
        StringBuilder builder = new StringBuilder();
        String tab = "\t".repeat(indent);
        for (Object2ObjectMap.Entry<String, List<Pair<T1, T2>>> entry : data.object2ObjectEntrySet()) {
            String key = entry.getKey();
            builder.append(tab).append("['").append(key).append("'] = {");
            if (!entry.getValue().isEmpty()) {
                builder.append("\n");
                for (Pair<T1, T2> pair : entry.getValue())
                    builder.append(tab).append("\t{'").append(pair.first())
                            .append("', '").append(pair.second()).append("'},\n");
                builder.append(tab);
            }
            builder.append("},\n");
        }
        return builder.toString();
    }
}
