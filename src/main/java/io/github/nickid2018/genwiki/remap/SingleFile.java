package io.github.nickid2018.genwiki.remap;

import io.github.nickid2018.genwiki.util.ClassUtils;
import lombok.SneakyThrows;

import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public record SingleFile(String file) implements InjectEntries {

    @Override
    @SneakyThrows
    public Map<String, byte[]> getInjectEntries() {
        String filePath = ClassUtils.toInternalName(file) + ".class";
        JarFile thisJar = new JarFile(
            IncludeJarPackages.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath()
        );
        Optional<JarEntry> file = thisJar.stream().filter(e -> e.getName().equals(filePath)).findFirst();
        if (file.isEmpty())
            return Map.of();
        byte[] bytes = thisJar.getInputStream(file.get()).readAllBytes();
        return Map.of(filePath, bytes);
    }
}
