package net.minecraft.world.level;

import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

public class GameRules {

    public void visitGameRuleTypes(GameRuleTypeVisitor gameRuleTypeVisitor) {
        throw new RuntimeException();
    }

    public interface GameRuleTypeVisitor {
        default <T extends Value<T>> void visit(Key<T> key, Type<T> type) {
        }

        default void visitBoolean(Key<BooleanValue> key, Type<BooleanValue> type) {
        }

        default void visitInteger(Key<IntegerValue> key, Type<IntegerValue> type) {
        }
    }

    public enum Category {

    }

    public static final class Key<T extends Value<T>> {

        public String getId() {
            throw new RuntimeException();
        }

        public Category getCategory() {
            throw new RuntimeException();
        }
    }

    public static class Type<T extends Value<T>> {

        public T createRule() {
            throw new RuntimeException();
        }

        public RequiredArgumentBuilder<CommandSourceStack, ?> createArgument(String string) {
            throw new RuntimeException();
        }
    }

    public static abstract class Value<T extends Value<T>> {

    }

    public static class BooleanValue extends Value<BooleanValue> {

        public boolean get() {
            throw new RuntimeException();
        }
    }

    public static class IntegerValue extends Value<IntegerValue> {

        public int get() {
            throw new RuntimeException();
        }
    }
}
