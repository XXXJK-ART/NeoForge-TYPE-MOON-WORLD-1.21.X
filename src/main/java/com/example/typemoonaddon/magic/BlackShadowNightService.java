package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.entity.SakuraBlackShadowEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class BlackShadowNightService {
    public static boolean blocksSleepSkip(ServerLevel level) {
        if (level == null) {
            return false;
        }
        for (var entity : level.getAllEntities()) {
            if (entity instanceof SakuraBlackShadowEntity shadow && shadow.isAlive()) {
                return true;
            }
        }
        return false;
    }

    public static void playerUnavailable(ServerPlayer player) {
        if (player == null) {
            return;
        }
        player.serverLevel().getEntitiesOfClass(
                SakuraBlackShadowEntity.class,
                player.getBoundingBox().inflate(128.0D),
                shadow -> player.getUUID().equals(shadow.getOwnerId())
        ).forEach(SakuraBlackShadowEntity::discard);
    }

    private BlackShadowNightService() {
    }
}
