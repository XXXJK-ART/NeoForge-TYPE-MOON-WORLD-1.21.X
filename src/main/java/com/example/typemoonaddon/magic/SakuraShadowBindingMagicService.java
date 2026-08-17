package com.example.typemoonaddon.magic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class SakuraShadowBindingMagicService {
    private static final double RANGE = 50.0D;

    public static boolean cast(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.isSpectator() || !SakuraTypeMoonIntegration.isShadowBindingLearned(player)) {
            return false;
        }
        if (SakuraShadowBindingService.hasSourceBinding(player)) {
            SakuraShadowBindingService.releaseBySource(player);
            player.displayClientMessage(Component.translatable("message.typemoonworld.shadow_binding.released"), true);
            return true;
        }
        LivingEntity target = SakuraMagicTargeting.rayTraceLiving(player, RANGE);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.shadow_binding.no_target"), true);
            return false;
        }
        if (!SakuraShadowBindingService.begin(player, target)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.shadow_binding.failed"), true);
            return false;
        }
        player.displayClientMessage(Component.translatable("message.typemoonworld.shadow_binding.bound"), true);
        return true;
    }

    private SakuraShadowBindingMagicService() {
    }
}
