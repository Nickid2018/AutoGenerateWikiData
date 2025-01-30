package io.github.nickid2018.genwiki.remap;

import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.tree.ClassNode;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public record RenameMethodTransform(String name, String desc, String nameRenamed) implements PostTransform {

    @Override
    public boolean transform(ClassNode code) {
        AtomicInteger count = new AtomicInteger();
        code.methods
            .stream()
            .filter(method -> method.name.equals(name) && method.desc.equals(desc))
            .forEach(method -> {
                count.getAndIncrement();
                method.name = nameRenamed;
                log.info("Renamed method {}{} to {}", name, desc, nameRenamed);
            });
        return count.get() > 0;
    }
}
