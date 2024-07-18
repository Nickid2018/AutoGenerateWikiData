package io.github.nickid2018.genwiki.autovalue.wikidata;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class EntitySyncWikiData extends JsonWikiData {

    private final JsonObject data = new JsonObject();

    public void put(String entity, String[] list) {
        JsonArray array = new JsonArray();
        for (String s : list)
            array.add(s);
        data.add(entity, array);
    }

    @Override
    public JsonElement asJsonData() {
        return data;
    }
}
