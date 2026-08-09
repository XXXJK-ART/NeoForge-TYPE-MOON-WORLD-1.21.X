package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.kimaris.KimarisService;
import com.example.typemoonaddon.nega_summon.NegaSummonService;
import com.example.typemoonaddon.nega_summon.NegaSummonService.CastOutcome;
import com.example.typemoonaddon.nega_summon.NegaSummonService.CastResult;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;

public final class NegaSummonMagic {
    public static final String MAGIC_ID = "nega_summon";
    public static final double MANA_COST = 0.0D;
    public static final int COOLDOWN_TICKS = 0;

    private NegaSummonMagic() {
    }

    public static MagicExecutionResult execute(MagicExecutionContext context) {
        ServerPlayer player = context.asServerPlayer();
        if (player == null) {
            return MagicExecutionResult.NOT_HANDLED;
        }
        if (!MAGIC_ID.equals(context.magicId())) {
            return MagicExecutionResult.NOT_HANDLED;
        }
        if (!player.isAlive()
                || player.isRemoved()
                || player.isSpectator()
                || KimarisService.isFrozen(player)) {
            return MagicExecutionResult.FAILED;
        }

        CastOutcome outcome = NegaSummonService.cast(player);
        if (outcome.result() == CastResult.NO_TARGET) {
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.nega_summon.no_target"), true);
            return MagicExecutionResult.FAILED;
        }
        if (outcome.result() != CastResult.SUCCESS) {
            return MagicExecutionResult.FAILED;
        }

        player.displayClientMessage(Component.translatable(
                "message.typemoonworld.nega_summon.success",
                outcome.affectedTargets()), true);
        return new MagicExecutionResult(true, true, MANA_COST, COOLDOWN_TICKS);
    }
}
