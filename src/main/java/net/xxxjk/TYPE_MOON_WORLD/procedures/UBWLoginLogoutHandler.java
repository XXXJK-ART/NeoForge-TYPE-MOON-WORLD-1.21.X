package net.xxxjk.TYPE_MOON_WORLD.procedures;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.server.level.ServerPlayer;

import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.ChantHandler;

@EventBusSubscriber
@SuppressWarnings("null")
public class UBWLoginLogoutHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            if (vars.is_in_ubw) {
                ChantHandler.returnFromUBW(player, vars);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            if (vars.is_in_ubw) {
                ChantHandler.returnFromUBW(player, vars);
            }
        }
    }
}

