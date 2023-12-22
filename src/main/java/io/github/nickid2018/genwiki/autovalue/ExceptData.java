package io.github.nickid2018.genwiki.autovalue;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class ExceptData {

    private final Map<String, Map<String, Set<String>>> exceptData = new TreeMap<>();
    private final Set<String> unknown = new TreeSet<>();

    public void put(String id, String key, String value) {
        exceptData.computeIfAbsent(id, k -> new TreeMap<>()).computeIfAbsent(value, k -> new TreeSet<>()).add(key);
    }

    public void putUnknown(String id) {
        unknown.add(id);
    }

    public String output() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Map<String, Set<String>>> entry : exceptData.entrySet()) {
            String id = entry.getKey();
            builder.append(id).append(": \n");
            for (Map.Entry<String, Set<String>> entry1 : entry.getValue().entrySet()) {
                String key = entry1.getKey();
                builder.append("\t").append(key).append(": \n");
                for (String value : entry1.getValue())
                    builder.append("\t\t").append(value).append("\n");
                builder.append("\n");
            }
        }
        builder.append("*Unknown Data (Need check the code manually): \n");
        for (String id : unknown)
            builder.append("\t").append(id).append("\n");
        return builder.toString();
    }
}
