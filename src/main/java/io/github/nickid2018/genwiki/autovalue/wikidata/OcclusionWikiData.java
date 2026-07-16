package io.github.nickid2018.genwiki.autovalue.wikidata;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class OcclusionWikiData extends JsonWikiData {

    private final Map<String, JsonObject> data = new TreeMap<>();

    public void put(
        String key,
        boolean canOcclude, int lightEmission,
        int lightDampening, boolean useShapeForLightOcclusion,
        boolean collisionFull, float shadeBrightness, boolean isVewBlocking,
        Map<String, List<double[]>> faces
    ) {
        JsonObject obj = new JsonObject();
        obj.addProperty("can_occlude", canOcclude);
        if (lightEmission > 0) obj.addProperty("emission", lightEmission);
        if (lightDampening > 0) obj.addProperty("dampening", lightDampening);
        if (useShapeForLightOcclusion) obj.addProperty("shape_light_occlusion", true);
        if (collisionFull) obj.addProperty("collision_full", true);
        if ((collisionFull && shadeBrightness != 0.2) || (!collisionFull && shadeBrightness != 1))
            obj.addProperty("shade_brightness", shadeBrightness);
        if (isVewBlocking) obj.addProperty("view_blocking", true);
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
