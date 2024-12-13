package io.github.nickid2018.genwiki.util;

import com.google.gson.JsonObject;

public class ConfigUtils {

    public static String envGetOrDefault(String key, String def) {
        String value = System.getenv(key);
        return value == null ? def : value;
    }

    public static int envGetOrDefault(String key, int def) {
        String value = System.getenv(key);
        return value == null ? def : Integer.parseInt(value);
    }

    public static String propGetOrDefault(String key, String def) {
        String value = System.getProperty(key);
        return value == null ? def : value;
    }

    public static int jsonGetOrDefault(JsonObject object, String key, int def) {
        return object.has(key) ? object.get(key).getAsInt() : def;
    }

    public static String jsonGetOrDefault(JsonObject object, String key, String def) {
        return object.has(key) ? object.get(key).getAsString() : def;
    }
}
