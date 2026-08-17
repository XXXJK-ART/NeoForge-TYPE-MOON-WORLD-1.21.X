package com.example.typemoonaddon.compat;

import com.example.typemoonaddon.TypeMoonAddon;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

/** Keeps Epic Fight integration optional at class-load time. */
public final class EpicFightBridge {
    private static final Methods METHODS = findMethods();

    private EpicFightBridge() {
    }

    public static boolean isBattleMode(Player player) {
        if (METHODS == null) {
            return false;
        }
        try {
            Object patch = METHODS.getPlayerPatch().invoke(null, player);
            return patch != null && (boolean) METHODS.isEpicFightMode().invoke(patch);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            TypeMoonAddon.LOGGER.warn("Could not read the current Epic Fight player mode", exception);
            return false;
        }
    }

    private static Methods findMethods() {
        if (!ModList.get().isLoaded("epicfight")) {
            return null;
        }
        try {
            Class<?> capabilities = Class.forName("yesman.epicfight.world.capabilities.EpicFightCapabilities");
            Class<?> playerPatch = Class.forName("yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch");
            return new Methods(
                capabilities.getMethod("getPlayerPatch", Player.class),
                playerPatch.getMethod("isEpicFightMode")
            );
        } catch (ReflectiveOperationException | LinkageError exception) {
            TypeMoonAddon.LOGGER.warn("Epic Fight is loaded, but its player-mode API is unavailable", exception);
            return null;
        }
    }

    private record Methods(Method getPlayerPatch, Method isEpicFightMode) {
    }
}
