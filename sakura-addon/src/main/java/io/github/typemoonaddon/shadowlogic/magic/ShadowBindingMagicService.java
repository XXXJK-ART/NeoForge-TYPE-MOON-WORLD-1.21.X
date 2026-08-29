package io.github.typemoonaddon.shadowlogic.magic;

import io.github.typemoonaddon.magic.HolyGrailService;
import io.github.typemoonaddon.magic.TypeMoonIntegration;

import java.util.Comparator;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class ShadowBindingMagicService {
    public static final double MANA_COST = 50.0D;
    public static final int COOLDOWN_TICKS = 20;
    private static final double RANGE = 50.0D;

    public static boolean cast(ServerPlayer player) {
        if (!player.isAlive()
            || player.isSpectator()
            || HolyGrailService.blocksAction(player)
            || !TypeMoonIntegration.isShadowBindingLearned(player)) {
            return false;
        }
        if (ShadowBindingService.hasSourceBinding(player)) {
            ShadowBindingService.releaseBySource(player);
            player.displayClientMessage(Component.translatable("message.typemoonaddon.shadow_binding.released"), true);
            return true;
        }

        LivingEntity target = findTarget(player);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.typemoonaddon.shadow_binding.no_target"), true);
            return false;
        }
        if (!ShadowBindingService.begin(player, target)) {
            player.displayClientMessage(Component.translatable("message.typemoonaddon.shadow_binding.failed"), true);
            return false;
        }
        player.displayClientMessage(Component.translatable("message.typemoonaddon.shadow_binding.bound"), true);
        return true;
    }

    private static LivingEntity findTarget(ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(RANGE));
        BlockHitResult blockHit = player.level().clip(new ClipContext(
            start,
            end,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            player
        ));
        double blockDistance = blockHit.getType() == net.minecraft.world.phys.HitResult.Type.MISS
            ? RANGE
            : start.distanceTo(blockHit.getLocation());
        Vec3 limitedEnd = start.add(player.getViewVector(1.0F).scale(blockDistance));
        AABB search = player.getBoundingBox().expandTowards(limitedEnd.subtract(start)).inflate(1.0D);

        return player.level().getEntitiesOfClass(
            LivingEntity.class,
            search,
            entity -> entity != player && entity.isAlive() && !entity.isSpectator()
        ).stream().map(entity -> {
            Optional<Vec3> intersection = entity.getBoundingBox().inflate(entity.getPickRadius()).clip(start, limitedEnd);
            return intersection.map(point -> new TargetHit(entity, start.distanceToSqr(point))).orElse(null);
        }).filter(hit -> hit != null)
            .min(Comparator.comparingDouble(TargetHit::distanceSquared))
            .map(TargetHit::entity)
            .orElse(null);
    }

    private record TargetHit(LivingEntity entity, double distanceSquared) {
    }

    private ShadowBindingMagicService() {
    }
}
