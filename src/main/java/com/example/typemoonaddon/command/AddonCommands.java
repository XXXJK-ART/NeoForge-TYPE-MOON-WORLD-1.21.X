package com.example.typemoonaddon.command;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.config.AddonCommandConfig;
import com.example.typemoonaddon.magic.AddonMagicRegistration;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.xxxjk.TYPE_MOON_WORLD.api.MagicDefinitionRegistry;
import net.xxxjk.TYPE_MOON_WORLD.magic.registry.MagicModularRegistry;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/** Server-only administration commands for the addon. */
@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID)
public final class AddonCommands {
    private static final String ROOT = "typemoon_addon";

    private AddonCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal(ROOT)
                        .requires(source -> source.hasPermission(AddonCommandConfig.PERMISSION_LEVEL.get()))
                        .then(Commands.literal("magic")
                                .then(Commands.literal("unlock_all_max")
                                        .then(Commands.argument("target", EntityArgument.players())
                                                .executes(AddonCommands::unlockAllMax))))
        );
    }

    private static int unlockAllMax(CommandContext<CommandSourceStack> context) {
        final Collection<ServerPlayer> targets;
        try {
            targets = EntityArgument.getPlayers(context, "target");
        } catch (Exception exception) {
            context.getSource().sendFailure(Component.translatable(
                    "command.typemoonworld.unlock_all_max.invalid_target"));
            return 0;
        }

        int maximumTargets = Math.max(1, AddonCommandConfig.MAXIMUM_TARGETS.get());
        if (targets.isEmpty()) {
            context.getSource().sendFailure(Component.translatable(
                    "command.typemoonworld.unlock_all_max.no_targets"));
            return 0;
        }
        if (targets.size() > maximumTargets) {
            context.getSource().sendFailure(Component.translatable(
                    "command.typemoonworld.unlock_all_max.too_many_targets", maximumTargets));
            return 0;
        }

        MagicSnapshot snapshot = snapshotMagicIds();
        if (snapshot.validIds().isEmpty()) {
            context.getSource().sendFailure(Component.translatable(
                    "command.typemoonworld.unlock_all_max.no_registered_magic"));
            TypeMoonAddon.LOGGER.error(
                    "unlock_all_max found no valid addon magic definitions; registeredAddonIds={}, missingIds={}",
                    snapshot.registeredIds(), snapshot.missingIds());
            return 0;
        }
        if (!snapshot.missingIds().isEmpty()) {
            TypeMoonAddon.LOGGER.warn(
                    "unlock_all_max skipped addon magic IDs missing from the main registry: {}",
                    snapshot.missingIds());
        }

        int succeeded = 0;
        int failed = 0;
        int addedTotal = 0;
        List<String> failedTargets = new ArrayList<>();
        for (ServerPlayer target : targets) {
            try {
                ApplyResult result = apply(target, snapshot.validIds());
                succeeded++;
                addedTotal += result.addedMagicCount();
                TypeMoonAddon.LOGGER.info(
                        "unlock_all_max operator={} target={} uuid={} addedMagics={} totalAddonMagics={} mana={}/{} regen={} regenInterval={} skippedIds={}",
                        context.getSource().getTextName(),
                        target.getGameProfile().getName(),
                        target.getUUID(),
                        result.addedMagicCount(),
                        snapshot.validIds().size(),
                        result.mana(),
                        result.maxMana(),
                        result.regeneration(),
                        result.regenerationInterval(),
                        snapshot.missingIds());
            } catch (Exception exception) {
                failed++;
                failedTargets.add(target.getGameProfile().getName());
                TypeMoonAddon.LOGGER.error(
                        "unlock_all_max failed for target={} uuid={} operator={}",
                        target.getGameProfile().getName(), target.getUUID(), context.getSource().getTextName(), exception);
            }
        }

        if (succeeded == 0) {
            context.getSource().sendFailure(Component.translatable(
                    "command.typemoonworld.unlock_all_max.all_failed"));
            return 0;
        }
        int successfulTargets = succeeded;
        int addedMagicEntries = addedTotal;
        int failedTargetsCount = failed;
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.typemoonworld.unlock_all_max.success",
                successfulTargets,
                snapshot.validIds().size(),
                addedMagicEntries,
                format(AddonCommandConfig.MAXIMUM_MANA.get()),
                format(AddonCommandConfig.MAXIMUM_MANA.get()),
                format(AddonCommandConfig.MAXIMUM_MANA_REGENERATION.get()),
                format(AddonCommandConfig.MAXIMUM_MANA_REGENERATION_INTERVAL.get()),
                failedTargetsCount), true);
        if (!failedTargets.isEmpty()) {
            context.getSource().sendFailure(Component.translatable(
                    "command.typemoonworld.unlock_all_max.partial_failure", String.join(", ", failedTargets)));
        }
        return succeeded;
    }

    private static MagicSnapshot snapshotMagicIds() {
        Set<String> registered = AddonMagicRegistration.registeredMagicIds();
        Set<String> modularIds = MagicModularRegistry.registeredMagicIds();
        List<String> registeredIds = registered.stream().sorted().toList();
        List<String> validIds = registeredIds.stream()
                .filter(id -> modularIds.contains(id) && MagicDefinitionRegistry.contains(id))
                .toList();
        List<String> missingIds = registeredIds.stream()
                .filter(id -> !modularIds.contains(id) || !MagicDefinitionRegistry.contains(id))
                .toList();
        return new MagicSnapshot(registeredIds, validIds, missingIds);
    }

    private static ApplyResult apply(ServerPlayer player, List<String> magicIds) {
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        vars.ensureMagicSystemInitialized();
        if (vars.learned_magics == null) {
            vars.learned_magics = new ArrayList<>();
        }

        List<String> oldLearnedMagics = new ArrayList<>(vars.learned_magics);
        double oldMana = vars.player_mana;
        double oldMaxMana = vars.player_max_mana;
        double oldRegeneration = vars.player_mana_egenerated_every_moment;
        double oldRegenerationInterval = vars.player_restore_magic_moment;
        double configuredMaxMana = finiteNonNegative(AddonCommandConfig.MAXIMUM_MANA.get(), 100000.0D);
        double configuredRegeneration = finiteNonNegative(
                AddonCommandConfig.MAXIMUM_MANA_REGENERATION.get(), 100.0D);
        double configuredRegenerationInterval = Math.max(1.0D, finiteNonNegative(
                AddonCommandConfig.MAXIMUM_MANA_REGENERATION_INTERVAL.get(), 1.0D));
        try {
            int added = 0;
            for (String magicId : magicIds) {
                if (!vars.learned_magics.contains(magicId)) {
                    vars.learned_magics.add(magicId);
                    added++;
                }
            }
            vars.player_max_mana = configuredMaxMana;
            vars.player_mana = configuredMaxMana;
            vars.player_mana_egenerated_every_moment = configuredRegeneration;
            vars.player_restore_magic_moment = configuredRegenerationInterval;
            vars.syncPlayerVariables(player);
            vars.syncMana(player);
            return new ApplyResult(added, vars.player_mana, vars.player_max_mana,
                    vars.player_mana_egenerated_every_moment, vars.player_restore_magic_moment);
        } catch (Exception exception) {
            vars.learned_magics.clear();
            vars.learned_magics.addAll(oldLearnedMagics);
            vars.player_mana = oldMana;
            vars.player_max_mana = oldMaxMana;
            vars.player_mana_egenerated_every_moment = oldRegeneration;
            vars.player_restore_magic_moment = oldRegenerationInterval;
            try {
                vars.syncPlayerVariables(player);
                vars.syncMana(player);
            } catch (Exception rollbackException) {
                exception.addSuppressed(rollbackException);
            }
            throw exception;
        }
    }

    private static double finiteNonNegative(double value, double fallback) {
        return Double.isFinite(value) && value >= 0.0D ? value : fallback;
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", finiteNonNegative(value, 0.0D));
    }

    private record MagicSnapshot(List<String> registeredIds, List<String> validIds, List<String> missingIds) {
    }

    private record ApplyResult(int addedMagicCount, double mana, double maxMana, double regeneration,
                               double regenerationInterval) {
    }
}
