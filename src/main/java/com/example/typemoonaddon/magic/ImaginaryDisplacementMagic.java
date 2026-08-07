package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;

public final class ImaginaryDisplacementMagic {
    public static final String MAGIC_ID = "imaginary_displacement";

    private ImaginaryDisplacementMagic() {
    }

    public static MagicExecutionResult execute(MagicExecutionContext context) {
        ServerPlayer player = context.asServerPlayer();
        if (player == null || !MAGIC_ID.equals(context.magicId()) || !player.isAlive()
                || player.isRemoved() || player.isSpectator()) {
            return player == null ? MagicExecutionResult.NOT_HANDLED : MagicExecutionResult.FAILED;
        }

        ImaginaryDisplacementData data = player.getData(ImaginaryDisplacementAttachments.PLAYER_STATE);
        if (data.isCoolingDown()) {
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.magic.imaginary_displacement.cooldown",
                    secondsRemaining(data.cooldownRemainingTicks())), true);
            return MagicExecutionResult.FAILED;
        }

        data.activate();
        player.displayClientMessage(Component.translatable(
                "message.typemoonworld.magic.imaginary_displacement.started"), true);

        // The addon owns the 300-tick cooldown; avoid the main mod's generic cast cooldown.
        return MagicExecutionResult.FAILED;
    }

    public static int secondsRemaining(int ticks) {
        return Math.max(1, (Math.max(0, ticks) + 19) / 20);
    }
}
