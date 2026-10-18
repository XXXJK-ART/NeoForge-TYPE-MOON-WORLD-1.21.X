package com.captain.servant.combat;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.xxxjk.TYPE_MOON_WORLD.entity.StoneManEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderEntity;

public final class AttackFeedbackService {
    private static final BlockParticleOption REDSTONE_BLOCK_PARTICLES =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState());

    @SubscribeEvent
    public void onDamage(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || event.getNewDamage() <= 0.0F || excluded(victim)) {
            return;
        }
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }

        level.playSound(
                null,
                victim.blockPosition(),
                SoundEvents.METAL_BREAK,
                SoundSource.HOSTILE,
                0.7F,
                1.0F
        );
        level.sendParticles(
                REDSTONE_BLOCK_PARTICLES,
                victim.getX(),
                victim.getY() + victim.getBbHeight() * 0.55D,
                victim.getZ(),
                10,
                victim.getBbWidth() * 0.35D,
                victim.getBbHeight() * 0.42D,
                victim.getBbWidth() * 0.35D,
                0.04D
        );
    }

    private static boolean excluded(Entity entity) {
        if (entity instanceof AbstractGolem
                || entity instanceof StoneManEntity
                || entity instanceof PaleRiderEntity) {
            return true;
        }
        if (!(entity instanceof net.minecraft.server.level.ServerPlayer player)) {
            return false;
        }
        TypeMoonWorldModVariables.PlayerVariables variables =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        return "pale_rider".equals(variables.servant_card_id);
    }
}
