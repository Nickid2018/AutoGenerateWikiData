package io.github.nickid2018.genwiki.remap;

import io.github.nickid2018.genwiki.util.ClassUtils;
import lombok.SneakyThrows;

import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public record SingleFile(String file, String path) implements InjectEntries {

    @Override
    @SneakyThrows
    public Map<String, byte[]> getInjectEntries() {
        JarFile thisJar = new JarFile(
            IncludeJarPackages.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath()
        );
        Optional<JarEntry> fileFind = thisJar.stream().filter(e -> e.getName().equals(file)).findFirst();
        if (fileFind.isEmpty())
            return Map.of();
        byte[] bytes = thisJar.getInputStream(fileFind.get()).readAllBytes();
        return Map.of(path, bytes);
    }
}
