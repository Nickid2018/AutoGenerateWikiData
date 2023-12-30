package io.github.nickid2018.genwiki.autovalue.wikidata;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class SpawnWikiData implements WikiData {

    private final Map<String, Map<String, Set<Entry>>> data = new TreeMap<>();

    public void add(String biome, String category, String entity, int weight, int min, int max) {
        data.computeIfAbsent(biome, k -> new TreeMap<>())
                .computeIfAbsent(category, k -> new TreeSet<>())
                .add(new Entry(entity, weight, min, max));
    }

    public void addSpawnCost(String biome, String entity, double budget, double charge) {
        data.computeIfAbsent(biome, k -> new TreeMap<>()).values().forEach(set -> set.stream()
                .filter(entry -> entry.name.equals(entity))
                .forEach(entry -> {
                    entry.budget = budget;
                    entry.charge = charge;
                }));
    }

    public static String formatValue(double value) {
        String formatted = String.format("%.3f", value);
        if (formatted.contains("."))
            while (formatted.endsWith("0"))
                formatted = formatted.substring(0, formatted.length() - 1);
        if (formatted.endsWith("."))
            formatted = formatted.substring(0, formatted.length() - 1);
        return formatted;
    }

    @Override
    public String output(int indent) {
        StringBuilder builder = new StringBuilder();
        String tab = "\t".repeat(indent);
        for (Map.Entry<String, Map<String, Set<Entry>>> biomeEntry : data.entrySet()) {
            String biome = biomeEntry.getKey();
            builder.append(tab).append("['").append(biome).append("'] = {\n");
            for (Map.Entry<String, Set<Entry>> categoryEntry : biomeEntry.getValue().entrySet()) {
                String category = categoryEntry.getKey();
                builder.append(tab).append("\t['").append(category).append("'] = {\n");
                for (Entry entry : categoryEntry.getValue()) {
                    builder.append(tab).append("\t\t['").append(entry.name).append("'] = {\n");
                    builder.append(tab).append("\t\t\t['weight'] = ").append(entry.weight).append(",\n");
                    builder.append(tab).append("\t\t\t['min_size'] = ").append(entry.min).append(",\n");
                    builder.append(tab).append("\t\t\t['max_size'] = ").append(entry.max).append(",\n");
                    if (entry.budget != 0) {
                        builder.append(tab).append("\t\t\t['budget'] = ").append(formatValue(entry.budget)).append(",\n");
                        builder.append(tab).append("\t\t\t['charge'] = ").append(formatValue(entry.charge)).append(",\n");
                    }
                    builder.append(tab).append("\t\t},\n");
                }
                builder.append(tab).append("\t},\n");
            }
            builder.append(tab).append("},\n");
        }
        return builder.toString();
    }

    private static class Entry implements Comparable<Entry> {
        public final String name;
        public final int weight;
        public final int min;
        public final int max;
        public double budget;
        public double charge;

        private Entry(String name, int weight, int min, int max) {
            this.name = name;
            this.weight = weight;
            this.min = min;
            this.max = max;
        }

        @Override
        public int compareTo(Entry o) {
            return name.compareTo(o.name);
        }
    }
}
