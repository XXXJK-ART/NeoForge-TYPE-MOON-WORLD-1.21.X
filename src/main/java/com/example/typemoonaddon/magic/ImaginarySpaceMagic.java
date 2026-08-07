package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.imaginary_space.ImaginarySpaceService;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceService.CastOutcome;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceService.CastStatus;
import com.example.typemoonaddon.kimaris.KimarisService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;

public final class ImaginarySpaceMagic {
    public static final String MAGIC_ID = "imaginary_space";

    private ImaginarySpaceMagic() {
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

        double manaCost = ImaginarySpaceService.manaCost(player);
        double mana = context.vars().player_mana;
        if (!Double.isFinite(mana) || mana < manaCost) {
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.imaginary_space.insufficient_mana", formatMana(manaCost)), true);
            return MagicExecutionResult.FAILED;
        }

        CastOutcome outcome = ImaginarySpaceService.cast(player);
        if (outcome.status() == CastStatus.NO_TARGET) {
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.imaginary_space.no_targets"), true);
            return MagicExecutionResult.FAILED;
        }
        if (outcome.status() == CastStatus.DIMENSION_UNAVAILABLE) {
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.imaginary_space.dimension_unavailable"), true);
            return MagicExecutionResult.FAILED;
        }
        if (outcome.status() != CastStatus.SUCCESS) {
            return MagicExecutionResult.FAILED;
        }

        player.displayClientMessage(Component.translatable(
                "message.typemoonworld.imaginary_space.cast", outcome.affectedTargets()), true);
        return new MagicExecutionResult(true, true, manaCost, 0);
    }

    private static String formatMana(double mana) {
        return Long.toString(Math.round(Math.max(0.0D, mana)));
    }
}
