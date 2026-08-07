package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.kimaris.KimarisService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class KimarisMagic {
    public static final String MAGIC_ID = "kimaris";
    public static final double MANA_COST = 200.0D;

    private KimarisMagic() {
    }

    public static MagicExecutionResult execute(MagicExecutionContext context) {
        ServerPlayer player = context.asServerPlayer();
        if (player == null) {
            return MagicExecutionResult.NOT_HANDLED;
        }
        if (!MAGIC_ID.equals(context.magicId()) || !player.isAlive() || player.isRemoved()
                || player.isSpectator() || KimarisService.isFrozen(player)) {
            return MagicExecutionResult.FAILED;
        }

        TypeMoonWorldModVariables.PlayerVariables vars = context.vars();
        double currentMana = finiteNonNegative(vars.player_mana);
        if (currentMana < MANA_COST) {
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.magic.kimaris.insufficient_mana"), true);
            return MagicExecutionResult.FAILED;
        }

        int affectedTargets = KimarisService.cast(player);
        if (affectedTargets <= 0) {
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.magic.kimaris.no_targets"), true);
            return MagicExecutionResult.FAILED;
        }

        player.displayClientMessage(Component.translatable(
                "message.typemoonworld.magic.kimaris.cast", affectedTargets), true);
        return new MagicExecutionResult(true, true, MANA_COST, 0);
    }

    private static double finiteNonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }
}
