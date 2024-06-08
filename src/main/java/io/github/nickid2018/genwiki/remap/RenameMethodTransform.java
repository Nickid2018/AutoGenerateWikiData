package io.github.nickid2018.genwiki.remap;

import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.tree.ClassNode;

@Slf4j
public record RenameMethodTransform(String name, String desc, String nameRenamed) implements PostTransform {

    @Override
    public void transform(ClassNode code) {
        code.methods.stream()
                    .filter(method -> method.name.equals(name) && method.desc.equals(desc))
                    .forEach(method -> {
                        method.name = nameRenamed;
                        log.info("Renamed method {}{} to {}", name, desc, nameRenamed);
                    });
    }
}
