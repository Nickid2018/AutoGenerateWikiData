package io.github.nickid2018.genwiki.autovalue.wikidata;


import java.util.Map;
import java.util.TreeMap;

public class ColorWikiData implements WikiData {

    private final Map<String, String> data = new TreeMap<>();

    public void put(String key, int value) {
        data.put(key, String.format("#%06X", value));
    }

    @Override
    public String output(int indent) {
        StringBuilder builder = new StringBuilder();
        String tab = "\t".repeat(indent);
        for (Map.Entry<String, String> entry : data.entrySet())
            builder.append(tab).append("['").append(entry.getKey()).append("'] = '").append(entry.getValue()).append("',\n");
        return builder.toString();
    }
}
