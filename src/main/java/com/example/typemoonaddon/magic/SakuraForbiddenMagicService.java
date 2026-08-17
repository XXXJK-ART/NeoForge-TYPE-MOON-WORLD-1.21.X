package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.registry.AddonAttachments;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SakuraForbiddenMagicService {
    private static final double RANGE = 16.0D;
    private static final double DOT_THRESHOLD = 0.35D;
    private static final float DAMAGE = 18.0F;
    private static final double MANA_DRAIN = 80.0D;

    public static boolean cast(ServerPlayer player) {
        if (player == null || !SakuraTypeMoonIntegration.isForbiddenMagicLearned(player)) {
            return false;
        }
        if (!SakuraTypeMoonIntegration.tryConsumeMana(player, 120.0D)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
            return false;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        List<LivingEntity> targets = player.serverLevel()
                .getEntitiesOfClass(LivingEntity.class, new AABB(player.blockPosition()).inflate(RANGE), target -> validTarget(player, target, eye, look))
                .stream()
                .sorted(Comparator.comparingDouble(player::distanceToSqr))
                .limit(12)
                .toList();
        if (targets.isEmpty()) {
            SakuraTypeMoonIntegration.refundMana(player, 120.0D);
            player.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
            return false;
        }
        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().magic(), DAMAGE);
            SakuraTypeMoonIntegration.registry().mana(target).tryConsume(MANA_DRAIN);
            SakuraPollutionService.expose(target, player, false);
            player.serverLevel().sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 18, 0.35D, 0.45D, 0.35D, 0.04D);
        }
        player.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0D, player.getZ(), 64, 1.2D, 0.8D, 1.2D, 0.08D);
        player.getData(AddonAttachments.IMAGINARY_SPACE.get()).unlockForbiddenMagic();
        AddonAttachments.sync(player, AddonAttachments.IMAGINARY_SPACE);
        SakuraTypeMoonIntegration.addProficiency(player, 0.35D);
        return true;
    }

    private static boolean validTarget(ServerPlayer player, LivingEntity target, Vec3 eye, Vec3 look) {
        if (target == player || !target.isAlive() || target.isSpectator() || target.isAlliedTo(player)) {
            return false;
        }
        Vec3 toTarget = target.getBoundingBox().getCenter().subtract(eye);
        double distance = toTarget.length();
        return distance <= RANGE && (distance <= 4.0D || toTarget.normalize().dot(look) >= DOT_THRESHOLD);
    }

    private SakuraForbiddenMagicService() {
    }
}
