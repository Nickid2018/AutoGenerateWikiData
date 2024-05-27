package io.github.nickid2018.genwiki.autovalue.wikidata;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Set;

public class LiquidComputationWikiData extends JsonWikiData {

    private final JsonObject data = new JsonObject();

    public void put(String key, boolean blocksMotion, Set<String> sturdyFaces) {
        JsonObject obj = new JsonObject();
        obj.addProperty("blocks_motion", blocksMotion);
        JsonArray array = new JsonArray();
        sturdyFaces.forEach(array::add);
        obj.add("face_sturdy", array);
        data.add(key, obj);
    }

    @Override
    public JsonElement asJsonData() {
        return data;
    }
}
