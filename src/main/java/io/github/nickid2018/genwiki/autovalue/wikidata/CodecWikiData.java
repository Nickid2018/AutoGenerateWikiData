package io.github.nickid2018.genwiki.autovalue.wikidata;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.TreeMap;

public class CodecWikiData extends JsonWikiData {

    private final Map<String, JsonElement> json = new TreeMap<>();

    public void add(String name, JsonElement data) {
        json.put(name, data);
    }

    @Override
    public JsonElement asJsonData() {
        JsonObject obj = new JsonObject();
        json.forEach(obj::add);
        return obj;
    }
}
