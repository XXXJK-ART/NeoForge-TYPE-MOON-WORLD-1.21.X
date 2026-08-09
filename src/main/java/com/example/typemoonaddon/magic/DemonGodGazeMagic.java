package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.demon_god_gaze.DemonGodGazeService;
import com.example.typemoonaddon.demon_god_gaze.DemonGodGazeService.CastOutcome;
import com.example.typemoonaddon.demon_god_gaze.DemonGodGazeService.CastStatus;
import com.example.typemoonaddon.kimaris.KimarisService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;

public final class DemonGodGazeMagic {
    public static final String MAGIC_ID = "demon_god_gaze";
    public static final double MANA_COST = 50.0D;

    private DemonGodGazeMagic() {
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
                    "message.typemoonworld.demon_god_gaze.insufficient_mana"), true);
            return MagicExecutionResult.FAILED;
        }

        CastOutcome outcome = DemonGodGazeService.cast(player);
        if (outcome.status() == CastStatus.NO_TARGET) {
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.demon_god_gaze.no_target"), true);
            return MagicExecutionResult.FAILED;
        }
        if (outcome.status() != CastStatus.SUCCESS) {
            return MagicExecutionResult.FAILED;
        }

        return new MagicExecutionResult(true, true, MANA_COST, 0);
    }
}
