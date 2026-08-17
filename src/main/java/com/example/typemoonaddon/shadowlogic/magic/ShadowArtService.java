package com.example.typemoonaddon.shadowlogic.magic;

import com.example.typemoonaddon.entity.SakuraShadowArtRibbonEntity;
import com.example.typemoonaddon.magic.SakuraShadowArtService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class ShadowArtService {
    public static boolean isUnavoidableInBlackMud(LivingEntity target, DamageSource source) {
        return source != null && source.getDirectEntity() instanceof SakuraShadowArtRibbonEntity;
    }

    public static boolean isPierced(LivingEntity target) {
        if (target == null) {
            return false;
        }
        return !target.level().getEntitiesOfClass(
                SakuraShadowArtRibbonEntity.class,
                target.getBoundingBox().inflate(96.0D),
                ribbon -> ribbon.targetEntityId() == target.getId() && ribbon.action() == SakuraShadowArtRibbonEntity.PIERCED
        ).isEmpty();
    }

    public static boolean deactivate(ServerPlayer player, boolean dispelled) {
        return SakuraShadowArtService.deactivate(player, dispelled);
    }

    private ShadowArtService() {
    }
}
