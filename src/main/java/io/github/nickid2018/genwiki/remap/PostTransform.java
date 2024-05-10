package io.github.nickid2018.genwiki.remap;

import org.objectweb.asm.tree.ClassNode;

public interface PostTransform {

    void transform(ClassNode code);
}
