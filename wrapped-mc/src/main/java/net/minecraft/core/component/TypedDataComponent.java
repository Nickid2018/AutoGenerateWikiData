package net.minecraft.core.component;

public record TypedDataComponent<T>(DataComponentType<T> type, T value) {
}
