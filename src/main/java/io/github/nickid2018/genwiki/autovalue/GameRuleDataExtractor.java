package io.github.nickid2018.genwiki.autovalue;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.github.nickid2018.genwiki.autovalue.wikidata.DoubleNumberWikiData;
import io.github.nickid2018.genwiki.autovalue.wikidata.StringWikiData;
import io.github.nickid2018.genwiki.autovalue.wikidata.WikiData;
import lombok.SneakyThrows;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

public class GameRuleDataExtractor {

    public static final StringWikiData GAME_RULE_CATEGORY = new StringWikiData();
    public static final StringWikiData GAME_RULE_DEFAULT_VALUE = new StringWikiData();
    public static final DoubleNumberWikiData GAME_RULE_RANGE = new DoubleNumberWikiData();

    @SneakyThrows
    public static void extractGameRuleData(MinecraftServer serverObj) {
        GameRules gameRules = serverObj.getGameRules();
        gameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            public void visitBoolean(GameRules.Key<GameRules.BooleanValue> key, GameRules.Type<GameRules.BooleanValue> type) {
                String name = key.getId();
                GameRules.Category category = key.getCategory();
                GAME_RULE_CATEGORY.put(name, category.name());
                GameRules.BooleanValue value = type.createRule();
                GAME_RULE_DEFAULT_VALUE.put(name, String.valueOf(value.get()));
            }

            @Override
            public void visitInteger(GameRules.Key<GameRules.IntegerValue> key, GameRules.Type<GameRules.IntegerValue> type) {
                String name = key.getId();
                GameRules.Category category = key.getCategory();
                GAME_RULE_CATEGORY.put(name, category.name());
                GameRules.IntegerValue value = type.createRule();
                GAME_RULE_DEFAULT_VALUE.put(name, String.valueOf(value.get()));
                RequiredArgumentBuilder<CommandSourceStack, ?> arg = type.createArgument("test");
                IntegerArgumentType argType = (IntegerArgumentType) arg.getType();
                GAME_RULE_RANGE.put(name, argType.getMinimum(), argType.getMaximum());
            }
        });
        WikiData.write(GAME_RULE_CATEGORY, "game_rule_category.txt");
        WikiData.write(GAME_RULE_DEFAULT_VALUE, "game_rule_default_value.txt");
        WikiData.write(GAME_RULE_RANGE, "game_rule_range.txt");
    }
}
