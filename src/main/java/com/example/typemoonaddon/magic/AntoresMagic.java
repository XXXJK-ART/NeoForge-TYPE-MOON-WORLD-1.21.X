package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.antores.AntoresService;
import com.example.typemoonaddon.kimaris.KimarisService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;

public final class AntoresMagic {
    public static final String MAGIC_ID = "antores";
    public static final double MANA_COST = 100.0D;

    private AntoresMagic() {
    }

    public static MagicExecutionResult execute(MagicExecutionContext context) {
        ServerPlayer player = context.asServerPlayer();
        if (player == null) {
            return MagicExecutionResult.NOT_HANDLED;
        }
        if (!MAGIC_ID.equals(context.magicId())
                || !player.isAlive()
                || player.isRemoved()
                || player.isSpectator()
                || KimarisService.isFrozen(player)) {
            return MagicExecutionResult.FAILED;
        }

        double mana = context.vars().player_mana;
        if (!Double.isFinite(mana) || mana < MANA_COST) {
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.magic.antores.insufficient_mana"), true);
            return MagicExecutionResult.FAILED;
        }

        AntoresService.CastOutcome outcome = AntoresService.cast(player);
        if (!outcome.accepted()) {
            return MagicExecutionResult.FAILED;
        }

        player.displayClientMessage(Component.translatable(
                "message.typemoonworld.magic.antores.cast", outcome.damagedTargets()), true);
        return new MagicExecutionResult(true, true, MANA_COST, 0);
    }
}
