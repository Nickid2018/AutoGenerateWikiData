package io.github.nickid2018.genwiki.util;

import lombok.SneakyThrows;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class LanguageUtils {

    public interface BiConsumerWithException<T, U, E extends Throwable> {
        void accept(T t, U u) throws E;
    }

    public static <T, U> BiConsumer<T, U> sneakyExceptionBiConsumer(BiConsumerWithException<T, U, ?> consumer) {
        return new BiConsumer<T, U>() {
            @Override
            @SneakyThrows
            public void accept(T t, U u) {
                consumer.accept(t, u);
            }
        };
    }
}
