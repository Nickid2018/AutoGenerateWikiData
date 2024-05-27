package io.github.nickid2018.genwiki.autovalue.wikidata;

import com.google.gson.JsonElement;

public abstract class JsonWikiData implements WikiData {

    public abstract JsonElement asJsonData();

    @Override
    public String output(int indent) {
        return asJsonData().toString();
    }
}
