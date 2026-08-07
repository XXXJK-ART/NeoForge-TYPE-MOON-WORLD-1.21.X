package com.example.typemoonaddon.magic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;

public final class AbsorptionMagic {
    public static final String MAGIC_ID = "absorption";

    private AbsorptionMagic() {
    }

    public static MagicExecutionResult execute(MagicExecutionContext context) {
        ServerPlayer player = context.asServerPlayer();
        if (player == null) {
            return MagicExecutionResult.NOT_HANDLED;
        }

        boolean active = AbsorptionState.toggle(player);
        player.displayClientMessage(Component.translatable(active
                ? "message.typemoonworld.magic.absorption.enabled"
                : "message.typemoonworld.magic.absorption.disabled"), true);

        // Handled without success prevents the main mod from adding its generic cast cooldown.
        return MagicExecutionResult.FAILED;
    }
}
