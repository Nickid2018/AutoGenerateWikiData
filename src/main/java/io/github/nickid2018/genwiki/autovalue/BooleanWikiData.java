package io.github.nickid2018.genwiki.autovalue;

import java.util.Set;
import java.util.TreeSet;

public class BooleanWikiData implements WikiData {

    private final Set<String> trueSet = new TreeSet<>();
    private final Set<String> falseSet = new TreeSet<>();

    public void put(String id, boolean value) {
        if (value)
            trueSet.add(id);
        else
            falseSet.add(id);
    }

    @Override
    public String output(int indent) {
        StringBuilder builder = new StringBuilder();
        String tab = "\t".repeat(indent);
        builder.append(tab).append("-- true\n");
        for (String id : trueSet)
            builder.append(tab).append("['").append(id).append("'] = true,\n");
        builder.append("\n");
        builder.append(tab).append("-- false\n");
        for (String id : falseSet)
            builder.append(tab).append("['").append(id).append("'] = false,\n");
        return builder.toString();
    }
}
