package io.github.nickid2018.genwiki.util;

import lombok.SneakyThrows;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class LanguageUtils {

    public interface BiConsumerWithException<T, U, E extends Throwable> {
        void accept(T t, U u) throws E;
    }

    public interface RunnableWithException<E extends Throwable> {
        void run() throws E;
    }

    public interface FunctionWithException<T, R, E extends Throwable> {
        R apply(T t) throws E;
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

    public static Runnable sneakyExceptionRunnable(RunnableWithException<?> runnable, Consumer<Throwable> catcher) {
        return () -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                catcher.accept(e);
            }
        };
    }

    public static <T, R> Function<T, R> exceptionOrElse(FunctionWithException<T, R, ?> func, BiFunction<T, Throwable, R> catcher) {
        return t -> {
            try {
                return func.apply(t);
            } catch (Throwable e) {
                return catcher.apply(t, e);
            }
        };
    }
}
