package net.minecraft.world.level.gamerules;

public interface GameRuleTypeVisitor {
    default void visitBoolean(GameRule<Boolean> gameRule) {
    }

    default void visitInteger(GameRule<Integer> gameRule) {
    }
}
