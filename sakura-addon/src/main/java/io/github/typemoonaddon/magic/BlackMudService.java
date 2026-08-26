package io.github.typemoonaddon.magic;

import io.github.typemoonaddon.registry.ModAttachments;
import io.github.typemoonaddon.registry.ModEffects;
import io.github.typemoonaddon.registry.ModBlocks;
import io.github.typemoonaddon.registry.ModFluids;
import io.github.typemoonaddon.data.PollutionData;
import io.github.typemoonaddon.entity.BlackShadowEntity;
import io.github.typemoonaddon.entity.ShadowFamiliarEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import javax.annotation.Nullable;

public final class BlackMudService {
    public static final int CORRUPTION_STAGES = 20;
    public static final double MANA_DRAIN_PER_SECOND = 20.0D;
    private static final int EFFECT_DURATION_TICKS = 12;
    private static final int TICKS_PER_CORRUPTION_STAGE = 5;

    public static void touch(LivingEntity entity, BlockPos mudPosition) {
        if (entity.level().isClientSide()) {
            return;
        }
        if (isImmune(entity)) {
            clearMovementRestriction(entity);
            return;
        }

        MobEffectInstance current = entity.getEffect(ModEffects.BLACK_MUD_CORRUPTION);
        boolean firstContactThisTick = current == null || current.getDuration() < EFFECT_DURATION_TICKS;
        int amplifier = current == null ? 0 : current.getAmplifier();
        if (firstContactThisTick
            && current != null
            && entity.tickCount % TICKS_PER_CORRUPTION_STAGE == 0) {
            amplifier = Math.min(CORRUPTION_STAGES - 1, amplifier + 1);
        }
        entity.forceAddEffect(
            new MobEffectInstance(
                ModEffects.BLACK_MUD_CORRUPTION,
                EFFECT_DURATION_TICKS,
                amplifier,
                false,
                true,
                true
            ),
            null
        );
        entity.forceAddEffect(
            new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_DURATION_TICKS, 5, false, true, true),
            null
        );
        if (firstContactThisTick && entity.tickCount % 20 == 0) {
            TypeMoonIntegration.drainMana(entity, MANA_DRAIN_PER_SECOND);
        }
        if (entity.level() instanceof ServerLevel level) {
            var owner = SummonBlackMudService.ownerAt(level, mudPosition);
            PollutionService.expose(entity, owner, true, Vec3.atCenterOf(mudPosition));
        }
    }

    public static void maintainImmobilization(LivingEntity entity) {
        if (!entity.hasEffect(ModEffects.BLACK_MUD_CORRUPTION)) {
            return;
        }
        if (isImmune(entity)) {
            clearMovementRestriction(entity);
            return;
        }
    }

    public static boolean isImmune(LivingEntity entity) {
        if (entity instanceof Player player
            && player.getData(ModAttachments.IMAGINARY_SPACE.get()).grailWormAscended()) {
            return true;
        }
        if (entity instanceof ShadowFamiliarEntity familiar
            && familiar.getOwner() instanceof Player owner
            && owner.getData(ModAttachments.IMAGINARY_SPACE.get()).grailWormAscended()) {
            return true;
        }
        if (entity instanceof BlackShadowEntity shadow && shadow.isGrailOwned()) {
            return true;
        }
        PollutionData pollution = entity.getExistingDataOrNull(ModAttachments.POLLUTION.get());
        return pollution != null && pollution.fullyCorrupted() && pollution.controllerId() != null;
    }

    public static boolean canWalkOnSurface(LivingEntity entity, FluidState fluidState) {
        if (!fluidState.getType().isSame(ModFluids.BLACK_MUD.get())
            && !fluidState.getType().isSame(ModFluids.FLOWING_BLACK_MUD.get())) {
            return false;
        }
        if (!entity.level().isClientSide()) {
            return isImmune(entity);
        }
        // Owner UUIDs are server-persistent rather than client entity data; these two summon types are always
        // validated by the authoritative server before their fluid collision is accepted.
        return entity instanceof ShadowFamiliarEntity
            || entity instanceof BlackShadowEntity
            || isImmune(entity);
    }

    public static boolean isOnOrInBlackMud(LivingEntity entity) {
        BlockPos feet = BlockPos.containing(entity.getX(), entity.getBoundingBox().minY + 0.05D, entity.getZ());
        BlockPos center = BlockPos.containing(entity.getBoundingBox().getCenter());
        return isMud(entity.level(), feet)
            || isMud(entity.level(), feet.below())
            || isMud(entity.level(), center);
    }

    public static boolean isSubmergedInBlackMud(LivingEntity entity) {
        BlockPos center = BlockPos.containing(entity.getBoundingBox().getCenter());
        BlockPos eyes = BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ());
        return isMud(entity.level(), center) || isMud(entity.level(), eyes);
    }

    public static boolean hasMudCoverage(LivingEntity entity) {
        double radius = 2.0D;
        for (int index = 0; index < 8; index++) {
            double angle = index * Math.PI / 4.0D;
            BlockPos sample = BlockPos.containing(
                entity.getX() + Math.cos(angle) * radius,
                entity.getBoundingBox().minY + 0.05D,
                entity.getZ() + Math.sin(angle) * radius
            );
            if (!isMud(entity.level(), sample) && !isMud(entity.level(), sample.below())) {
                return false;
            }
        }
        return isOnOrInBlackMud(entity);
    }

    public static void avoidNearbyMud(Mob mob) {
        if (mob.level().isClientSide() || isImmune(mob) || !mob.isAlive() || mob.isNoAi()) {
            return;
        }
        boolean touchingMud = mob.hasEffect(ModEffects.BLACK_MUD_CORRUPTION);
        int interval = touchingMud ? 5 : 20;
        if (Math.floorMod(mob.tickCount + mob.getId(), interval) != 0) {
            return;
        }
        BlockPos mud = nearestMud(mob, touchingMud ? 7 : 5);
        if (mud == null) {
            return;
        }

        Vec3 destination = null;
        if (mob instanceof PathfinderMob pathfinder) {
            for (int attempt = 0; attempt < 4; attempt++) {
                Vec3 candidate = DefaultRandomPos.getPosAway(pathfinder, 12, 6, Vec3.atCenterOf(mud));
                if (candidate != null && !isMud(mob.level(), BlockPos.containing(candidate))) {
                    destination = candidate;
                    break;
                }
            }
        }
        if (destination == null) {
            Vec3 away = mob.position().subtract(Vec3.atCenterOf(mud));
            if (away.lengthSqr() < 0.01D) {
                away = new Vec3(1.0D, 0.0D, 0.0D);
            }
            destination = mob.position().add(away.normalize().scale(10.0D));
        }
        mob.setTarget(null);
        mob.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.2D);
    }

    @Nullable
    private static BlockPos nearestMud(Mob mob, int radius) {
        BlockPos center = mob.blockPosition();
        BlockPos closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (BlockPos candidate : BlockPos.betweenClosed(
            center.offset(-radius, -2, -radius),
            center.offset(radius, 2, radius)
        )) {
            if (!isMud(mob.level(), candidate)) {
                continue;
            }
            double distance = candidate.distSqr(center);
            if (distance < closestDistance) {
                closest = candidate.immutable();
                closestDistance = distance;
            }
        }
        return closest;
    }

    private static boolean isMud(net.minecraft.world.level.Level level, BlockPos position) {
        return level.getBlockState(position).is(ModBlocks.BLACK_MUD.get());
    }

    public static void clearMovementRestriction(LivingEntity entity) {
        entity.removeEffect(ModEffects.BLACK_MUD_CORRUPTION);
        removeMudSlowness(entity);
    }

    private static void removeMudSlowness(LivingEntity entity) {
        MobEffectInstance slowness = entity.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (slowness != null && slowness.getAmplifier() == 5 && slowness.getDuration() <= EFFECT_DURATION_TICKS) {
            entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
    }

    private BlackMudService() {
    }
}
