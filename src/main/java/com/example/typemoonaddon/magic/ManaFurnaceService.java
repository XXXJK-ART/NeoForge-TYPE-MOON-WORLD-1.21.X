package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.block.entity.ManaFurnaceBlockEntity;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Shared mana source query used by wards and future continuous magic effects. */
public final class ManaFurnaceService {
    private ManaFurnaceService() {
    }

    public static boolean hasInfiniteSupply(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        return !ManaFurnaceBlockEntity.nearby(level, player).isEmpty();
    }

    public static boolean trySupply(ServerPlayer player, double amount) {
        return amount >= 0.0D && hasInfiniteSupply(player);
    }

    public static List<ManaFurnaceBlockEntity> nearby(ServerPlayer player) {
        return player != null && player.level() instanceof ServerLevel level
                ? ManaFurnaceBlockEntity.nearby(level, player) : List.of();
    }
}
