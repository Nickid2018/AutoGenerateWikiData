package net.minecraft.core.component;

public interface DataComponentMap
    extends Iterable<TypedDataComponent<?>>{

    <T> T get(DataComponentType<? extends T> var1);
}
