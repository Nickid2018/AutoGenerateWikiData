package net.minecraft.resources;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.util.stream.Stream;

public class RegistryOps<T> implements DynamicOps<T> {
    @Override
    public T empty() {
        return null;
    }

    @Override
    public <U> U convertTo(DynamicOps<U> outOps, T input) {
        return null;
    }

    @Override
    public DataResult<Number> getNumberValue(T input) {
        return null;
    }

    @Override
    public T createNumeric(Number i) {
        return null;
    }

    @Override
    public DataResult<String> getStringValue(T input) {
        return null;
    }

    @Override
    public T createString(String value) {
        return null;
    }

    @Override
    public DataResult<T> mergeToList(T list, T value) {
        return null;
    }

    @Override
    public DataResult<T> mergeToMap(T map, T key, T value) {
        return null;
    }

    @Override
    public DataResult<Stream<Pair<T, T>>> getMapValues(T input) {
        return null;
    }

    @Override
    public T createMap(Stream<Pair<T, T>> map) {
        return null;
    }

    @Override
    public DataResult<Stream<T>> getStream(T input) {
        return null;
    }

    @Override
    public T createList(Stream<T> input) {
        return null;
    }

    @Override
    public T remove(T input, String key) {
        return null;
    }
}
