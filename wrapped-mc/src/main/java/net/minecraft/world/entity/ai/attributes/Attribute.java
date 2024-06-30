package net.minecraft.world.entity.ai.attributes;

import io.github.nickid2018.util.SneakyUtil;

public class Attribute {

    public final double defaultValue = SneakyUtil.sneakyInt();
    public Sentiment sentiment = Sentiment.POSITIVE;

    public enum Sentiment {
        POSITIVE,
        NEUTRAL,
        NEGATIVE
    }
}
