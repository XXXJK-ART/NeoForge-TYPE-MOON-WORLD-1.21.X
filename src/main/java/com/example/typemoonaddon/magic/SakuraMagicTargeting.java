package com.example.typemoonaddon.magic;

import java.util.Comparator;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

final class SakuraMagicTargeting {
    @Nullable
    static LivingEntity rayTraceLiving(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 intendedEnd = start.add(player.getLookAngle().scale(range));
        BlockHitResult blockHit = player.level().clip(new ClipContext(
                start,
                intendedEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? intendedEnd : blockHit.getLocation();
        AABB search = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);

        return player.level().getEntitiesOfClass(
                        LivingEntity.class,
                        search,
                        entity -> validHostileTarget(player, entity)
                )
                .stream()
                .map(entity -> {
                    Optional<Vec3> intersection = entity.getBoundingBox().inflate(Math.max(0.3F, entity.getPickRadius())).clip(start, end);
                    return intersection.map(point -> new TargetHit(entity, start.distanceToSqr(point))).orElse(null);
                })
                .filter(hit -> hit != null)
                .min(Comparator.comparingDouble(TargetHit::distanceSquared))
                .map(TargetHit::entity)
                .orElse(null);
    }

    static boolean validHostileTarget(LivingEntity owner, LivingEntity target) {
        if (target == owner
                || !target.isAlive()
                || target.isRemoved()
                || !target.isAttackable()
                || target.isInvulnerable()
                || target.isAlliedTo(owner)
                || owner.isAlliedTo(target)) {
            return false;
        }
        return !(target instanceof Player player) || !player.isCreative() && !player.isSpectator();
    }

    private record TargetHit(LivingEntity entity, double distanceSquared) {
    }

    private SakuraMagicTargeting() {
    }
}
