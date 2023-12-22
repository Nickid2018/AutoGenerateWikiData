package io.github.nickid2018.genwiki.autovalue;

import it.unimi.dsi.fastutil.floats.FloatFloatImmutablePair;
import it.unimi.dsi.fastutil.floats.FloatFloatPair;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public class DoubleNumberWikiData implements WikiData {

    private boolean enableFallback = false;
    private float fallback1 = 0;
    private float fallback2 = 0;
    private boolean fallbackNil = false;
    private boolean useFirstKeyComparator = true;

    private static final Comparator<FloatFloatPair> FIRST_KEY_COMPARATOR = (o1, o2) -> Float.compare(o2.firstFloat(), o1.firstFloat());
    private static final Comparator<FloatFloatPair> SECOND_KEY_COMPARATOR = (o1, o2) -> Float.compare(o2.secondFloat(), o1.secondFloat());

    private static final Comparator<FloatFloatPair> FIRST_FIRST = FIRST_KEY_COMPARATOR.thenComparing(SECOND_KEY_COMPARATOR);
    private static final Comparator<FloatFloatPair> FIRST_SECOND = SECOND_KEY_COMPARATOR.thenComparing(FIRST_KEY_COMPARATOR);

    private final Object2ObjectAVLTreeMap<FloatFloatPair, Set<String>> groups = new Object2ObjectAVLTreeMap<>(
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

    public DoubleNumberWikiData setFallback(float fallback1, float fallback2) {
        enableFallback = true;
        this.fallback1 = fallback1;
        this.fallback2 = fallback2;
        return this;
    }

    public void put(String id, float value1, float value2) {
        if (enableFallback && value1 == fallback1)
            return;
        groups.computeIfAbsent(new FloatFloatImmutablePair(value1, value2), k -> new TreeSet<>()).add(id);
    }

    private String formatValue(float value) {
        String formatted = String.format("%.3f", value);
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
                builder.append(tab).append("['__fallback'] = nil,\n\n");
            else
                builder.append(tab).append("['__fallback'] = {")
                        .append(formatValue(fallback1)).append(", ").append(formatValue(fallback2)).append("},\n\n");
        for (Object2ObjectMap.Entry<FloatFloatPair, Set<String>> entry : groups.object2ObjectEntrySet()) {
            FloatFloatPair value = entry.getKey();
            String formatted1 = formatValue(value.firstFloat());
            String formatted2 = formatValue(value.secondFloat());
            builder.append(tab).append("-- <").append(formatted1).append(", ").append(formatted2).append(">\n");
            for (String id : entry.getValue())
                builder.append(tab).append("['").append(id).append("'] = {").append(formatted1).append(", ").append(formatted2).append("},\n");
            builder.append("\n");
        }
        return builder.toString();
    }
}
