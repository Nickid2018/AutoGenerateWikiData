package io.github.nickid2018.genwiki.autovalue;

import io.github.nickid2018.genwiki.autovalue.wikidata.StringWikiData;
import io.github.nickid2018.genwiki.autovalue.wikidata.WikiData;
import lombok.SneakyThrows;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.gamerules.GameRules;

public class GameRuleDataExtractor {

    public static final StringWikiData GAME_RULE_CATEGORY = new StringWikiData();
    public static final StringWikiData GAME_RULE_DEFAULT_VALUE = new StringWikiData();
    public static final StringWikiData GAME_RULE_TYPE = new StringWikiData();

    @SneakyThrows
    public static void extractGameRuleData(MinecraftServer serverObj) {
        GameRules gameRules = serverObj.getWorldData().getGameRules();
        gameRules.visitGameRuleTypes(new GameRuleTypeVisitor() {
            @Override
            public void visitBoolean(GameRule<Boolean> gameRule) {
                String name = gameRule.toString();
                GAME_RULE_CATEGORY.put(name, gameRule.category().id().getPath().toUpperCase());
                GAME_RULE_TYPE.put(name, "bool");
                GAME_RULE_DEFAULT_VALUE.put(name, String.valueOf(gameRule.defaultValue()));
            }

            @Override
            public void visitInteger(GameRule<Integer> gameRule) {
                String name = gameRule.toString();
                GAME_RULE_CATEGORY.put(name, gameRule.category().id().getPath().toUpperCase());
                GAME_RULE_TYPE.put(name, "int");
                GAME_RULE_DEFAULT_VALUE.put(name, String.valueOf(gameRule.defaultValue()));
            }
        });
        WikiData.write(GAME_RULE_TYPE, "gamerule/type.txt");
        WikiData.write(GAME_RULE_CATEGORY, "gamerule/category.txt");
        WikiData.write(GAME_RULE_DEFAULT_VALUE, "gamerule/default_value.txt");
    }
}
