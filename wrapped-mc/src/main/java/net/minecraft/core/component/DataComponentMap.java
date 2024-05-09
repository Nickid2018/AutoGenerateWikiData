package net.minecraft.core.component;

public interface DataComponentMap {

    <T> T get(DataComponentType<? extends T> var1);
}
