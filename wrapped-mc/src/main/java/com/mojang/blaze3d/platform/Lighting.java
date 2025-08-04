package com.mojang.blaze3d.platform;

public class Lighting {

    public enum Entry {
        LEVEL,
        ITEMS_FLAT,
        ITEMS_3D,
        ENTITY_IN_UI,
        PLAYER_SKIN;
    }

    public void setupFor(Entry entry) {
        throw new RuntimeException();
    }
}
