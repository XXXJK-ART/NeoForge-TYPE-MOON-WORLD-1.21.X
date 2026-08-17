package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.registry.AddonAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class SakuraRuleBreakerDispelService {
    public static boolean canDispel(LivingEntity target) {
        if (!(target instanceof ServerPlayer player)) {
            return false;
        }
        var data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        return data.protectionActive()
                || data.grailWormAscended()
                || data.cursedArmorPresent()
                || data.shadowArtUnlocked();
    }

    public static boolean dispel(LivingEntity target) {
        if (!(target instanceof ServerPlayer player) || !canDispel(player)) {
            return false;
        }
        var data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        if (data.cursedArmorPresent()) {
            CursedArmorService.beginDissolution(player);
        }
        if (data.shadowArtUnlocked()) {
            SakuraShadowArtService.deactivate(player, true);
        }
        if (data.grailWormAscended()) {
            data.revokeGrailWormAscension();
        } else {
            data.clearProtection();
        }
        AddonAttachments.sync(player, AddonAttachments.IMAGINARY_SPACE);
        CursedArmorService.sync(player);
        return true;
    }

    public static boolean lockGrailMagicKnowledge(ServerPlayer player) {
        return true;
    }

    private SakuraRuleBreakerDispelService() {
    }
}
