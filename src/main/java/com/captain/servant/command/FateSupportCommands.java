package com.captain.servant.command;

import com.captain.servant.lancelot.KnightArmoryService;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardLancelotBerserkerSkills;

public final class FateSupportCommands {
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("fatesupport")
                .then(Commands.literal("knight_of_owner_unlock")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    if (!ServantCardLancelotBerserkerSkills.isActiveCard(player)) {
                                        return 0;
                                    }
                                    KnightArmoryService.setAllUnlocked(
                                            player,
                                            BoolArgumentType.getBool(context, "enabled")
                                    );
                                    return 1;
                                }))));
    }
}
