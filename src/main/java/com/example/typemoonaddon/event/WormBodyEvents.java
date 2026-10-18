package com.example.typemoonaddon.event;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.magic.WormMagicIntegration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent;

/** Enforces the permanent movement restrictions from an implanted worm. */
@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID)
public final class WormBodyEvents {
    private WormBodyEvents() {}

    @SubscribeEvent
    public static void onJump(LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof ServerPlayer player
                && player.getPersistentData().getBoolean("TypeMoonAddonEngravedWormImmobilized")) {
            player.setDeltaMovement(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
        }
    }

    public static void tick(ServerPlayer player) {
        if (player == null || !player.getPersistentData().getBoolean("TypeMoonAddonEngravedWormImmobilized")) return;
        player.setDeltaMovement(player.getDeltaMovement().x, player.getDeltaMovement().y, player.getDeltaMovement().z);
        if (!player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS,
                    Integer.MAX_VALUE, 1, false, true, true));
        }
        if (!player.hasEffect(net.minecraft.world.effect.MobEffects.CONFUSION)) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION,
                    Integer.MAX_VALUE, 1, false, true, true));
        }
    }
}
