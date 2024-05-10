package io.github.nickid2018.genwiki.util;

public class ClassUtils {

    public static String toBinaryName(String name) {
        return name.replace('/', '.');
    }

    public static String toInternalName(String name) {
        return name.replace('.', '/');
    }
}
