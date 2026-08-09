package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.imaginary_space.ImaginarySpaceService;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceService.DepthChangeResult;
import com.example.typemoonaddon.kimaris.KimarisService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/** Server-side validation and application for the imaginary-space depth editor. */
public final class ImaginaryDiveMagic {
    public static final String MAGIC_ID = "imaginary_dive";
    public static final double MANA_COST = ImaginarySpaceService.SELF_MANA_COST;
    public static final double MAX_DEPTH = ImaginarySpaceService.MAX_DEPTH;
    public static final int COOLDOWN_TICKS = 10;

    private ImaginaryDiveMagic() {
    }

    public static MagicExecutionResult execute(MagicExecutionContext context) {
        ServerPlayer player = context.asServerPlayer();
        if (player == null) {
            return MagicExecutionResult.NOT_HANDLED;
        }
        if (!MAGIC_ID.equals(context.magicId())) {
            return MagicExecutionResult.NOT_HANDLED;
        }
        if (!ImaginarySpaceService.DIMENSION.equals(player.serverLevel().dimension())) {
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.imaginary_dive.needs_space"), true);
        } else {
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.imaginary_dive.open_panel"), true);
        }
        return MagicExecutionResult.FAILED;
    }

    public static void submitDepth(ServerPlayer player, double requestedDepth) {
        if (player == null || !player.isAlive() || player.isRemoved() || player.isSpectator()
                || KimarisService.isFrozen(player)) {
            return;
        }
        TypeMoonWorldModVariables.PlayerVariables vars =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        vars.ensureMagicSystemInitialized();
        vars.rebuildSelectedMagicsFromActiveWheel();
        PlayerMagicSelectionService.prepareCurrentSelection(player, vars);
        if (!vars.is_magus || !vars.is_magic_circuit_open
                || !PlayerMagicSelectionService.isCurrentSelection(vars, MAGIC_ID)) {
            return;
        }
        if (!ImaginarySpaceService.DIMENSION.equals(player.serverLevel().dimension())) {
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.imaginary_dive.needs_space"), true);
            return;
        }
        if (!Double.isFinite(requestedDepth)
                || requestedDepth < 0.0D
                || requestedDepth > MAX_DEPTH) {
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.imaginary_dive.invalid_depth"), true);
            return;
        }
        if (!Double.isFinite(vars.magic_cooldown) || vars.magic_cooldown > 0.0D) {
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.imaginary_dive.cooldown"), true);
            return;
        }
        double mana = Double.isFinite(vars.player_mana) ? Math.max(0.0D, vars.player_mana) : 0.0D;
        if (mana < MANA_COST) {
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.imaginary_dive.insufficient_mana", format(MANA_COST)), true);
            return;
        }

        DepthChangeResult result = ImaginarySpaceService.adjustDepth(player, requestedDepth);
        switch (result) {
            case NOT_IN_SPACE, REJECTED -> player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.imaginary_dive.needs_space"), true);
            case INVALID -> player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.imaginary_dive.invalid_depth"), true);
            case UNCHANGED -> player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.imaginary_dive.unchanged"), true);
            case CHANGED -> {
                vars.player_mana = Math.max(0.0D, mana - MANA_COST);
                vars.magic_cooldown = Math.max(vars.magic_cooldown, COOLDOWN_TICKS);
                vars.syncMana(player);
                player.displayClientMessage(Component.translatable(
                        "message.typemoonworld.imaginary_dive.changed", format(requestedDepth)), true);
            }
        }
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
