package io.github.nickid2018.genwiki.remap;

import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

@Slf4j
public record ExtendAccessTransform(
    io.github.nickid2018.genwiki.remap.ExtendAccessTransform.Mode mode
) implements PostTransform {

    public static final ExtendAccessTransform FIELD = new ExtendAccessTransform(Mode.FIELD);
    public static final ExtendAccessTransform METHOD = new ExtendAccessTransform(Mode.METHOD);
    public static final ExtendAccessTransform ALL = new ExtendAccessTransform(Mode.ALL);

    public enum Mode {
        FIELD, METHOD, ALL
    }

    @Override
    public void transform(ClassNode code) {
        if (mode == Mode.FIELD || mode == Mode.ALL) {
            code.fields.forEach(field -> {
                field.access &= ~Opcodes.ACC_PROTECTED & ~Opcodes.ACC_PRIVATE;
                field.access |= Opcodes.ACC_PUBLIC;
            });
        }
        if (mode == Mode.METHOD || mode == Mode.ALL) {
            code.methods.forEach(method -> {
                method.access &= ~Opcodes.ACC_PROTECTED & ~Opcodes.ACC_PRIVATE;
                method.access |= Opcodes.ACC_PUBLIC;
            });
        }
        log.info("Extend access transform applied to class {} with mode {}", code.name, mode);
    }
}
