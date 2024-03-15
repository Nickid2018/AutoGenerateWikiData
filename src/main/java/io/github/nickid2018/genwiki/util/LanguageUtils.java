package io.github.nickid2018.genwiki.util;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class LanguageUtils {

    public interface FunctionWithException<T, R, E extends Throwable> {
        R apply(T t) throws E;
    }

    public interface BiConsumerWithException<T, U, E extends Throwable> {
        void accept(T t, U u) throws E;
    }

    public static <T, R> Function<T, R> sneakyExceptionFunction(FunctionWithException<T, R, ?> function) {
        return t -> {
            try {
                return function.apply(t);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static <T, U> BiConsumer<T, U> sneakyExceptionBiConsumer(BiConsumerWithException<T, U, ?> consumer) {
        return (t, u) -> {
            try {
                consumer.accept(t, u);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <S, T> Function<S, T> castFunction() {
        return t -> (T) t;
    }
}
