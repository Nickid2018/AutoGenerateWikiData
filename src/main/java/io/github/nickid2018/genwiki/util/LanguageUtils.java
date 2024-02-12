package io.github.nickid2018.genwiki.util;

import java.util.function.Function;

public class LanguageUtils {

    public interface FunctionWithException<T, R, E extends Throwable> {
        R apply(T t) throws E;
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

    @SuppressWarnings("unchecked")
    public static <S, T> Function<S, T> castFunction() {
        return t -> (T) t;
    }
}
