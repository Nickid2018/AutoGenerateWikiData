package io.github.nickid2018.genwiki.autovalue.wikidata;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.TreeMap;

public class EntitySyncWikiData extends JsonWikiData {

    private final Map<String, JsonArray> data = new TreeMap<>();

    public void put(String entity, String[] list) {
        JsonArray array = new JsonArray();
        for (String s : list)
            array.add(s);
        data.put(entity, array);
    }

    @Override
    public JsonElement asJsonData() {
        JsonObject obj = new JsonObject();
        data.forEach(obj::add);
        return obj;
    }
}
