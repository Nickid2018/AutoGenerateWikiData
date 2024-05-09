package net.minecraft.world.entity.ai.attributes;

import net.minecraft.util.StringRepresentable;

import java.util.UUID;

public record AttributeModifier(UUID id, String name, double amount, Operation operation) {

    public enum Operation implements StringRepresentable {
        ;

        @Override
        public String getSerializedName() {
            return "";
        }
    }
}
