package io.github.nickid2018.genwiki.autovalue.wikidata;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class OcclusionWikiData extends JsonWikiData {

    private final Map<String, JsonObject> data = new TreeMap<>();

    public void put(String key, boolean canOcclude, Map<String, List<double[]>> faces) {
        JsonObject obj = new JsonObject();
        obj.addProperty("can_occlude", canOcclude);
        for (Map.Entry<String, List<double[]>> entry : faces.entrySet()) {
            JsonArray array = new JsonArray();
            for (double[] doubles : entry.getValue()) {
                JsonArray face = new JsonArray();
                for (double d : doubles)
                    face.add(d);
                array.add(face);
            }
            obj.add(entry.getKey(), array);
        }
        data.put(key, obj);
    }

    @Override
    public JsonElement asJsonData() {
        JsonObject obj = new JsonObject();
        data.forEach(obj::add);
        return obj;
    }
}
