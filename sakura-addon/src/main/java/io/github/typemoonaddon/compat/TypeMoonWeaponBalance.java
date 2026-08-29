package io.github.typemoonaddon.compat;

import io.github.typemoonaddon.TypeMoonAddon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;

public final class TypeMoonWeaponBalance {
    private static final ResourceLocation TEMPLE_STONE_SWORD_AXE =
        ResourceLocation.fromNamespaceAndPath("typemoonworld", "temple_stone_sword_axe");
    private static final ResourceLocation HERACLES_ATTACK_SPEED =
        TypeMoonAddon.id("heracles_temple_stone_sword_axe_speed");
    private static final AttributeModifier HERACLES_ATTACK_SPEED_MODIFIER = new AttributeModifier(
        HERACLES_ATTACK_SPEED,
        4.5D,
        AttributeModifier.Operation.ADD_VALUE
    );

    public static void tick(ServerPlayer player) {
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed == null) {
            return;
        }
        var form = TypeMoonWorldApi.servantForm(player);
        ResourceLocation servantId = form.servantId();
        boolean shouldBoost = form.transformed()
            && servantId != null
            && "heracles".equals(servantId.getPath())
            && TEMPLE_STONE_SWORD_AXE.equals(BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()));
        if (shouldBoost && !attackSpeed.hasModifier(HERACLES_ATTACK_SPEED)) {
            attackSpeed.addTransientModifier(HERACLES_ATTACK_SPEED_MODIFIER);
        } else if (!shouldBoost && attackSpeed.hasModifier(HERACLES_ATTACK_SPEED)) {
            attackSpeed.removeModifier(HERACLES_ATTACK_SPEED);
        }
    }

    private TypeMoonWeaponBalance() {
    }
}
