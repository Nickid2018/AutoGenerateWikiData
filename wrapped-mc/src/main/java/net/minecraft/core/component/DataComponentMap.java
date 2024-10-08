package net.minecraft.core.component;

import java.util.Set;

public interface DataComponentMap
    extends Iterable<TypedDataComponent<?>>{

    <T> T get(DataComponentType<? extends T> var1);
}
