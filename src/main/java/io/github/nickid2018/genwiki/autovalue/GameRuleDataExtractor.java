package io.github.nickid2018.genwiki.autovalue;

import io.github.nickid2018.genwiki.autovalue.wikidata.StringWikiData;
import io.github.nickid2018.genwiki.autovalue.wikidata.WikiData;
import lombok.SneakyThrows;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

public class GameRuleDataExtractor {

    public static final StringWikiData GAME_RULE_CATEGORY = new StringWikiData();
    public static final StringWikiData GAME_RULE_DEFAULT_VALUE = new StringWikiData();
    public static final StringWikiData GAME_RULE_TYPE = new StringWikiData();

    @SneakyThrows
    public static void extractGameRuleData(MinecraftServer serverObj) {
        GameRules gameRules = serverObj.getWorldData().getGameRules();
        gameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            public void visitBoolean(GameRules.Key<GameRules.BooleanValue> key, GameRules.Type<GameRules.BooleanValue> type) {
                String name = key.getId();
                GameRules.Category category = key.getCategory();
                GAME_RULE_CATEGORY.put(name, category.name());
                GAME_RULE_TYPE.put(name, "bool");
                GameRules.BooleanValue value = type.createRule();
                GAME_RULE_DEFAULT_VALUE.put(name, String.valueOf(value.get()));
            }

            @Override
            public void visitInteger(GameRules.Key<GameRules.IntegerValue> key, GameRules.Type<GameRules.IntegerValue> type) {
                String name = key.getId();
                GameRules.Category category = key.getCategory();
                GAME_RULE_CATEGORY.put(name, category.name());
                GAME_RULE_TYPE.put(name, "int");
                GameRules.IntegerValue value = type.createRule();
                GAME_RULE_DEFAULT_VALUE.put(name, String.valueOf(value.get()));
            }
        });
        WikiData.write(GAME_RULE_TYPE, "game_rule_type.txt");
        WikiData.write(GAME_RULE_CATEGORY, "game_rule_category.txt");
        WikiData.write(GAME_RULE_DEFAULT_VALUE, "game_rule_default_value.txt");
    }
}
