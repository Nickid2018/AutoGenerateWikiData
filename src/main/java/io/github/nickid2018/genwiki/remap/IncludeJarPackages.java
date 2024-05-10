package io.github.nickid2018.genwiki.remap;

import io.github.nickid2018.genwiki.util.ClassUtils;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public record IncludeJarPackages(String packagePrefix, Class<?> searchJar) implements InjectEntries {

    public IncludeJarPackages(String packagePrefix) {
        this(packagePrefix, IncludeJarPackages.class);
    }

    @Override
    @SneakyThrows
    public Map<String, byte[]> getInjectEntries() {
        String packagePrefix = ClassUtils.toInternalName(this.packagePrefix) + "/";
        JarFile thisJar = new JarFile(
            IncludeJarPackages.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath()
        );
        Map<String, byte[]> entries = new HashMap<>();
        for (JarEntry entry : thisJar.stream().filter(e -> e.getName().startsWith(packagePrefix)).toList()) {
            byte[] bytes = thisJar.getInputStream(entry).readAllBytes();
            entries.put(entry.getName(), bytes);
        }
        return entries;
    }
}
