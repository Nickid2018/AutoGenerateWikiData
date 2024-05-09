package io.github.nickid2018.util;

public class SneakyUtil {

    @SuppressWarnings("unchecked")
    public static <T> T sneakyNotNull() {
        return (T) new Object();
    }

    public static boolean sneakyBool() {
        return Math.random() > 0.5;
    }

    public static int sneakyInt() {
        return (int) (Math.random() * 100);
    }
}
