package io.github.nickid2018.genwiki.util;

import io.github.nickid2018.genwiki.remap.IncludeJarPackages;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;

import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassUtils {

    public static String toBinaryName(String name) {
        return name.replace('/', '.');
    }

    public static String toInternalName(String name) {
        return name.replace('.', '/');
    }

    @SneakyThrows
    public static String readJarContent(String path) {
        try (JarFile thisJar = new JarFile(ClassUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath())) {
            JarEntry fileFind = thisJar.stream().filter(e -> e.getName().equals(path)).findFirst().orElseThrow();
            return IOUtils.toString(thisJar.getInputStream(fileFind), StandardCharsets.UTF_8);
        }
    }
}
