package io.github.nickid2018.genwiki.autovalue.wikidata;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class LiquidComputationWikiData extends JsonWikiData {

    private final Map<String, JsonObject> data = new TreeMap<>();

    public void put(String key, boolean blocksMotion, Set<String> sturdyFaces) {
        JsonObject obj = new JsonObject();
        obj.addProperty("blocks_motion", blocksMotion);
        JsonArray array = new JsonArray();
        sturdyFaces.forEach(array::add);
        obj.add("face_sturdy", array);
        data.put(key, obj);
    }

    @Override
    public JsonElement asJsonData() {
        JsonObject obj = new JsonObject();
        data.forEach(obj::add);
        return obj;
    }
}
