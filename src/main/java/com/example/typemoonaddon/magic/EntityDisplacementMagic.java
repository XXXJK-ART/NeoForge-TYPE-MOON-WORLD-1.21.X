package com.example.typemoonaddon.magic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;

public final class EntityDisplacementMagic {
    public static final String MAGIC_ID = "entity_displacement";

    private EntityDisplacementMagic() {
    }

    public static MagicExecutionResult execute(MagicExecutionContext context) {
        ServerPlayer player = context.asServerPlayer();
        if (player == null || !MAGIC_ID.equals(context.magicId())) {
            return player == null ? MagicExecutionResult.NOT_HANDLED : MagicExecutionResult.FAILED;
        }
        if (EntityDisplacementService.swap(player)) {
            return MagicExecutionResult.SUCCESS;
        }
        player.displayClientMessage(Component.translatable(
                "message.typemoonworld.magic.entity_displacement.no_target"), true);
        return MagicExecutionResult.FAILED;
    }
}
