package io.github.nickid2018.genwiki.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

public class JsonUtils {
    @SuppressWarnings("unchecked")
    public static <T extends JsonElement> Optional<T> getData(JsonObject root, String name, Class<T> type) {
        JsonElement element = root.get(name);
        if (element == null)
            return Optional.empty();
        if (type.isInstance(element))
            return Optional.of((T) element);
        else
            return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public static <T extends JsonElement> Optional<T> getDataInPath(JsonObject root, String path, Class<T> type) {
        String[] paths = path.split("\\.");
        JsonElement last = root;
        for (int i = 0; i < paths.length - 1; i++) {
            String key = paths[i];
            if (last instanceof JsonArray array) {
                int num = Integer.parseInt(key);
                if (num >= array.size())
                    return Optional.empty();
                last = array.get(num);
            } else if (last instanceof JsonObject object) {
                if (!object.has(key))
                    return Optional.empty();
                last = object.get(key);
            } else
                return Optional.empty();
        }
        String name = paths[paths.length - 1];
        JsonElement get = null;
        if (last instanceof JsonObject now) {
            if (!now.has(name))
                return Optional.empty();
            get = now.get(name);
        } else if (last instanceof JsonArray now){
            int num = Integer.parseInt(name);
            if (num >= now.size())
                return Optional.empty();
            get = now.get(num);
        }
        return type.isInstance(get) ? Optional.of((T) get) : Optional.empty();
    }

    public static Optional<String> getString(JsonObject root, String path) {
        return getData(root, path, JsonPrimitive.class).map(JsonPrimitive::getAsString);
    }

    public static String getStringOrNull(JsonObject root, String path) {
        return getString(root, path).orElse(null);
    }

    public static String getStringOrElse(JsonObject root, String path, String other) {
        return getString(root, path).orElse(other);
    }

    public static Optional<String> getStringInPath(JsonObject root, String path) {
        return getDataInPath(root, path, JsonPrimitive.class).map(JsonPrimitive::getAsString);
    }

    public static String getStringInPathOrNull(JsonObject root, String path) {
        return getStringInPath(root, path).orElse(null);
    }

    public static String getStringInPathOrElse(JsonObject root, String path, String other) {
        return getStringInPath(root, path).orElse(other);
    }

    public static OptionalInt getInt(JsonObject root, String path) {
        return getData(root, path, JsonPrimitive.class)
                .map(JsonPrimitive::getAsInt).map(OptionalInt::of).orElse(OptionalInt.empty());
    }

    public static int getIntOrZero(JsonObject root, String path) {
        return getInt(root, path).orElse(0);
    }

    public static int getIntOrElse(JsonObject root, String path, int other) {
        return getInt(root, path).orElse(other);
    }

    public static OptionalInt getIntInPath(JsonObject root, String path) {
        return getDataInPath(root, path, JsonPrimitive.class)
                .map(JsonPrimitive::getAsInt).map(OptionalInt::of).orElse(OptionalInt.empty());
    }

    public static int getIntInPathOrZero(JsonObject root, String path) {
        return getIntInPath(root, path).orElse(0);
    }

    public static int getIntInPathOrElse(JsonObject root, String path, int other) {
        return getIntInPath(root, path).orElse(other);
    }

    public static OptionalLong getLong(JsonObject root, String path) {
        return getData(root, path, JsonPrimitive.class)
                .map(JsonPrimitive::getAsLong).map(OptionalLong::of).orElse(OptionalLong.empty());
    }

    public static long getLongOrZero(JsonObject root, String path) {
        return getLong(root, path).orElse(0);
    }

    public static long getLongOrElse(JsonObject root, String path, long other) {
        return getLong(root, path).orElse(other);
    }

    public static OptionalLong getLongInPath(JsonObject root, String path) {
        return getDataInPath(root, path, JsonPrimitive.class)
                .map(JsonPrimitive::getAsLong).map(OptionalLong::of).orElse(OptionalLong.empty());
    }

    public static long getLongInPathOrZero(JsonObject root, String path) {
        return getLongInPath(root, path).orElse(0);
    }

    public static long getLongInPathOrElse(JsonObject root, String path, long other) {
        return getLongInPath(root, path).orElse(other);
    }
}
