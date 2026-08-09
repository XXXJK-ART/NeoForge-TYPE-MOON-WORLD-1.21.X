package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import net.minecraft.world.entity.player.Player;

public final class AbsorptionState {
    private static final String ACTIVE_TAG = TypeMoonAddon.MOD_ID + "_absorption_active";

    private AbsorptionState() {
    }

    public static boolean isActive(Player player) {
        return player != null && player.getPersistentData().getBoolean(ACTIVE_TAG);
    }

    public static boolean toggle(Player player) {
        boolean active = !isActive(player);
        player.getPersistentData().putBoolean(ACTIVE_TAG, active);
        return active;
    }

    public static void clear(Player player) {
        if (player != null) {
            player.getPersistentData().remove(ACTIVE_TAG);
        }
    }
}
