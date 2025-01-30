package io.github.nickid2018.genwiki.remap;

import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.function.Supplier;

@Slf4j
public record AddMethodTransform(Supplier<MethodNode> methodNodeSupplier) implements PostTransform {

    @Override
    public boolean transform(ClassNode code) {
        MethodNode methodNode = methodNodeSupplier.get();
        code.methods.add(methodNode);
        log.info("Added method {}{} to class {}", methodNode.name, methodNode.desc, code.name);
        return true;
    }
}
