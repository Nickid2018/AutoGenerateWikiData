package io.github.nickid2018.genwiki.autovalue.wikidata;

import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import it.unimi.dsi.fastutil.doubles.DoubleDoublePair;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public class DoubleNumberWikiData implements WikiData {

    private boolean enableFallback = false;
    private double fallback1 = 0;
    private double fallback2 = 0;
    private boolean fallbackNil = false;
    private boolean useFirstKeyComparator = true;

    private static final Comparator<DoubleDoublePair> FIRST_KEY_COMPARATOR = (o1, o2) -> Double.compare(
        o2.firstDouble(),
        o1.firstDouble()
    );
    private static final Comparator<DoubleDoublePair> SECOND_KEY_COMPARATOR = (o1, o2) -> Double.compare(
        o2.secondDouble(),
        o1.secondDouble()
    );

    private static final Comparator<DoubleDoublePair> FIRST_FIRST = FIRST_KEY_COMPARATOR.thenComparing(
        SECOND_KEY_COMPARATOR);
    private static final Comparator<DoubleDoublePair> FIRST_SECOND = SECOND_KEY_COMPARATOR.thenComparing(
        FIRST_KEY_COMPARATOR);

    private final Object2ObjectAVLTreeMap<DoubleDoublePair, Set<String>> groups = new Object2ObjectAVLTreeMap<>(
        (o1, o2) -> useFirstKeyComparator ? FIRST_FIRST.compare(o1, o2) : FIRST_SECOND.compare(o1, o2)
    );

    public DoubleNumberWikiData setUseFirstKeyComparator(boolean useFirstKeyComparator) {
        this.useFirstKeyComparator = useFirstKeyComparator;
        return this;
    }

    public DoubleNumberWikiData setFallbackNil(boolean fallbackNil) {
        this.fallbackNil = fallbackNil;
        return this;
    }

    public DoubleNumberWikiData setFallback(double fallback1, double fallback2) {
        enableFallback = true;
        this.fallback1 = fallback1;
        this.fallback2 = fallback2;
        return this;
    }

    public void put(String id, double value1, double value2) {
        if (enableFallback && value1 == fallback1)
            return;
        groups.computeIfAbsent(new DoubleDoubleImmutablePair(value1, value2), k -> new TreeSet<>()).add(id);
    }

    private String formatValue(double value) {
        String formatted = String.format("%.5f", value);
        if (formatted.contains("."))
            while (formatted.endsWith("0"))
                formatted = formatted.substring(0, formatted.length() - 1);
        if (formatted.endsWith("."))
            formatted = formatted.substring(0, formatted.length() - 1);
        return formatted;
    }

    public String output(int indent) {
        StringBuilder builder = new StringBuilder();
        String tab = "\t".repeat(indent);
        if (enableFallback)
            if (fallbackNil)
                builder.append(tab).append("['__fallback'] = {},\n\n");
            else
                builder.append(tab).append("['__fallback'] = {")
                       .append(formatValue(fallback1)).append(", ").append(formatValue(fallback2)).append("},\n\n");
        for (Object2ObjectMap.Entry<DoubleDoublePair, Set<String>> entry : groups.object2ObjectEntrySet()) {
            DoubleDoublePair value = entry.getKey();
            String formatted1 = formatValue(value.firstDouble());
            String formatted2 = formatValue(value.secondDouble());
            builder.append(tab).append("-- <").append(formatted1).append(", ").append(formatted2).append(">\n");
            for (String id : entry.getValue())
                builder.append(tab).append("['").append(id).append("'] = {").append(formatted1).append(", ").append(
                    formatted2).append("},\n");
            builder.append("\n");
        }
        return builder.toString();
    }
}
