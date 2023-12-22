package io.github.nickid2018.genwiki.autovalue;

import it.unimi.dsi.fastutil.floats.FloatFloatImmutablePair;
import it.unimi.dsi.fastutil.floats.FloatFloatPair;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

import java.util.ArrayList;
import java.util.List;

public class NumberPairMapWikiData implements WikiData {

    private final Object2ObjectMap<String, List<FloatFloatPair>> data = new Object2ObjectAVLTreeMap<>();

    public boolean hasKey(String id) {
        return data.containsKey(id);
    }

    public void put(String id, List<FloatFloatPair> value) {
        data.put(id, value);
    }

    public void putNew(String id, float a, float b) {
        List<FloatFloatPair> val = data.computeIfAbsent(id, k -> new ArrayList<>());
        FloatFloatPair value = new FloatFloatImmutablePair(a, b);
        if (!val.contains(value))
            val.add(value);
    }

    public String output(int indent) {
        StringBuilder builder = new StringBuilder();
        String tab = "\t".repeat(indent);
        for (Object2ObjectMap.Entry<String, List<FloatFloatPair>> entry : data.object2ObjectEntrySet()) {
            String key = entry.getKey();
            builder.append(tab).append("['").append(key).append("'] = {\n");
            for (FloatFloatPair pair : entry.getValue())
                builder.append(tab).append("\t{'").append(NumberWikiData.formatValue(pair.firstFloat()))
                        .append("', '").append(NumberWikiData.formatValue(pair.secondFloat())).append("'},\n");
            builder.append(tab).append("},\n");
        }
        return builder.toString();
    }
}
