package io.github.nickid2018.genwiki.remap;

import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.function.Consumer;

@Slf4j
public record MethodTransform(
    String methodName, String methodDesc, Consumer<MethodNode> transform
) implements PostTransform {

    @Override
    public void transform(ClassNode code) {
        int count = 0;
        for (MethodNode methodNode : code.methods) {
            if (methodNode.name.equals(methodName) && (methodDesc == null || methodNode.desc.equals(methodDesc))) {
                transform.accept(methodNode);
                log.info(
                    "Transformed method {}{} in class {}",
                    methodName,
                    methodDesc == null ? "" : methodDesc,
                    code.name
                );
                count++;
            }
        }
        if (count == 0)
            log.warn(
                "Method {}{} not found in class {}",
                methodName,
                methodDesc == null ? "" : methodDesc,
                code.name
            );
    }
}
