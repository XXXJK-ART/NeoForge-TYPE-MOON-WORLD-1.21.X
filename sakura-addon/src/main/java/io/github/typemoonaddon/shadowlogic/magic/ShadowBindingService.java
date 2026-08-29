package io.github.typemoonaddon.shadowlogic.magic;

import io.github.typemoonaddon.entity.BlackShadowEntity;
import io.github.typemoonaddon.entity.ShadowFamiliarEntity;
import io.github.typemoonaddon.magic.BlackMudService;
import io.github.typemoonaddon.magic.GrailParticleService;
import io.github.typemoonaddon.magic.ImaginaryShadowService;
import io.github.typemoonaddon.magic.PollutionService;
import io.github.typemoonaddon.magic.RuleBreakerDispelService;
import io.github.typemoonaddon.shadowlogic.network.ShadowBindingEffectPayload;
import io.github.typemoonaddon.registry.ModEffects;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-authoritative control and particle-shell health for a familiar's Shadow Binding. */
public final class ShadowBindingService {
    public static final float PARTICLE_SHELL_HEALTH = 300.0F;
    public static final float ADDITIONAL_SOURCE_HEALTH = 50.0F;
    public static final int MAX_SOURCES_PER_TARGET = 5;
    private static final double PROJECTILE_BARRIER_INFLATION = 1.5D;
    private static final double GATE_PROJECTILE_INTERCEPT_DISTANCE = 4.0D;
    private static final double CONTROL_DEAD_ZONE = 10.0D;
    private static final double MAXIMUM_SLOWNESS_DISTANCE = 40.0D;
    private static final int MAXIMUM_SLOWNESS_LEVEL = 6;
    private static final double MAXIMUM_PULL_SPEED_PER_TICK = 1.0D / 20.0D;

    private static final Map<UUID, ShadowBinding> BINDINGS_BY_TARGET = new HashMap<>();
    private static final Map<UUID, Set<UUID>> TARGETS_BY_SOURCE = new HashMap<>();

    public static boolean canBegin(LivingEntity source, @Nullable LivingEntity target) {
        if (!source.isAlive()
            || target == null
            || !target.isAlive()
            || source == target
            || source.level() != target.level()) {
            return false;
        }
        Set<UUID> sourceTargets = TARGETS_BY_SOURCE.get(source.getUUID());
        int sourceTargetLimit = source instanceof BlackShadowEntity ? 5 : 1;
        if (sourceTargets != null
            && (sourceTargets.contains(target.getUUID()) || sourceTargets.size() >= sourceTargetLimit)) {
            return false;
        }
        ShadowBinding binding = BINDINGS_BY_TARGET.get(target.getUUID());
        return binding == null || binding.controlActive && binding.sourceIds.size() < MAX_SOURCES_PER_TARGET;
    }

    public static boolean begin(LivingEntity source, LivingEntity target) {
        if (!canBegin(source, target)) {
            return false;
        }
        UUID sourceId = source.getUUID();
        UUID targetId = target.getUUID();

        ShadowBinding binding = BINDINGS_BY_TARGET.get(targetId);
        if (binding == null) {
            binding = new ShadowBinding(
                targetId,
                target.level().dimension(),
                ImaginaryShadowService.originalTargetWasNoAi(target),
                target instanceof Mob mob && mob.getTarget() != null ? mob.getTarget().getUUID() : null,
                PARTICLE_SHELL_HEALTH
            );
            binding.sourceIds.add(sourceId);
            BINDINGS_BY_TARGET.put(targetId, binding);
        } else {
            if (!binding.sourceIds.add(sourceId)) {
                return false;
            }
            binding.shellHealth += ADDITIONAL_SOURCE_HEALTH;
        }
        TARGETS_BY_SOURCE.computeIfAbsent(sourceId, ignored -> new HashSet<>()).add(targetId);
        maintainLock(target, binding);
        target.level().playSound(
            null,
            target.blockPosition(),
            SoundEvents.WARDEN_HEARTBEAT,
            SoundSource.HOSTILE,
            0.9F,
            0.5F
        );
        return true;
    }

    public static boolean hasBinding(LivingEntity source, LivingEntity target) {
        ShadowBinding binding = BINDINGS_BY_TARGET.get(target.getUUID());
        return binding != null && binding.sourceIds.contains(source.getUUID());
    }

    public static boolean hasBlackShadowSource(LivingEntity target) {
        ShadowBinding binding = BINDINGS_BY_TARGET.get(target.getUUID());
        MinecraftServer server = target.getServer();
        if (binding == null || server == null) {
            return false;
        }
        for (UUID sourceId : binding.sourceIds) {
            if (living(server, binding.dimension, sourceId) instanceof BlackShadowEntity) {
                return true;
            }
        }
        return false;
    }

    public static void rewardBlackShadowManaFromDamage(LivingEntity target, double actualDamage) {
        if (!Double.isFinite(actualDamage) || actualDamage <= 0.0D) {
            return;
        }
        ShadowBinding binding = BINDINGS_BY_TARGET.get(target.getUUID());
        MinecraftServer server = target.getServer();
        if (binding == null || server == null) {
            return;
        }
        Set<BlackShadowEntity> shadows = new HashSet<>();
        for (UUID sourceId : binding.sourceIds) {
            if (living(server, binding.dimension, sourceId) instanceof BlackShadowEntity shadow) {
                shadows.add(shadow);
            }
        }
        if (shadows.isEmpty()) {
            return;
        }
        double share = actualDamage / shadows.size();
        shadows.forEach(shadow -> shadow.rewardOwnerManaFromDamage(share));
    }

    public static boolean isControlling(LivingEntity source, LivingEntity target) {
        ShadowBinding binding = BINDINGS_BY_TARGET.get(target.getUUID());
        return binding != null
            && binding.controlActive
            && binding.sourceIds.contains(source.getUUID());
    }

    public static boolean hasDormantBinding(LivingEntity source, LivingEntity target) {
        ShadowBinding binding = BINDINGS_BY_TARGET.get(target.getUUID());
        return binding != null
            && !binding.controlActive
            && binding.sourceIds.contains(source.getUUID());
    }

    public static boolean isBound(LivingEntity target) {
        return BINDINGS_BY_TARGET.containsKey(target.getUUID());
    }

    public static boolean hasSourceBinding(Entity source) {
        Set<UUID> targets = TARGETS_BY_SOURCE.get(source.getUUID());
        return targets != null && !targets.isEmpty();
    }

    public static boolean isAtSourceCapacity(@Nullable LivingEntity target) {
        if (target == null) {
            return false;
        }
        ShadowBinding binding = BINDINGS_BY_TARGET.get(target.getUUID());
        return binding != null
            && (!binding.controlActive || binding.sourceIds.size() >= MAX_SOURCES_PER_TARGET);
    }

    public static boolean originalTargetWasNoAi(LivingEntity target) {
        ShadowBinding binding = BINDINGS_BY_TARGET.get(target.getUUID());
        return binding != null ? binding.targetWasNoAi : target instanceof Mob mob && mob.isNoAi();
    }

    public static void releaseBySource(Entity source) {
        Set<UUID> targetIds = TARGETS_BY_SOURCE.get(source.getUUID());
        if (targetIds == null || targetIds.isEmpty()) {
            return;
        }
        for (UUID targetId : Set.copyOf(targetIds)) {
            ShadowBinding binding = BINDINGS_BY_TARGET.get(targetId);
            if (binding == null) {
                removeSourceMapping(source.getUUID(), targetId);
                continue;
            }
            if (!binding.controlActive && source instanceof ShadowFamiliarEntity) {
                continue;
            }
            removeSource(binding, source.getUUID(), source.getServer(), true);
        }
    }

    public static void release(LivingEntity source, LivingEntity target) {
        ShadowBinding binding = BINDINGS_BY_TARGET.get(target.getUUID());
        if (binding != null && binding.sourceIds.contains(source.getUUID())) {
            removeSource(binding, source.getUUID(), source.getServer(), true);
        } else {
            removeSourceMapping(source.getUUID(), target.getUUID());
        }
    }

    /** The shell absorbs the entire breaking hit; later hits reach the released target normally. */
    public static boolean absorbDamage(LivingIncomingDamageEvent event) {
        RuleBreakerDispelService.dispelFromDamage(event.getEntity(), event.getSource());
        if (isFriendlyDamage(event.getEntity(), event.getSource())
            || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return absorbDirectDamage(event.getEntity(), null, event.getAmount());
    }

    /** A bound entity's outgoing attack strikes its enclosing shell instead of the intended victim. */
    public static boolean absorbBoundAttackerDamage(LivingIncomingDamageEvent event) {
        RuleBreakerDispelService.dispelFromDamage(event.getEntity(), event.getSource());
        Entity attacker = resolveAttacker(event.getSource());
        if (!(attacker instanceof LivingEntity boundAttacker) || event.getAmount() <= 0.0F) {
            return false;
        }
        ShadowBinding binding = BINDINGS_BY_TARGET.get(boundAttacker.getUUID());
        return binding != null && damageShell(boundAttacker, binding, event.getAmount());
    }

    public static boolean absorbDirectDamage(LivingEntity target, @Nullable Entity attacker, float amount) {
        ShadowBinding binding = BINDINGS_BY_TARGET.get(target.getUUID());
        if (binding == null || amount <= 0.0F || isFriendly(binding, attacker, target.getServer())) {
            return false;
        }
        return damageShell(target, binding, amount);
    }

    private static boolean damageShell(LivingEntity target, ShadowBinding binding, float amount) {
        binding.shellHealth -= amount;
        boolean broken = binding.shellHealth <= 0.0F;
        spawnShellHit(target, binding, broken);
        if (broken) {
            release(binding, target.getServer(), true);
        }
        return true;
    }

    public static boolean isFriendlyDamage(LivingEntity target, DamageSource source) {
        ShadowBinding binding = BINDINGS_BY_TARGET.get(target.getUUID());
        if (binding == null) {
            return false;
        }
        return isFriendly(binding, resolveAttacker(source), target.getServer());
    }

    public static void maintainTargetLock(Entity entity) {
        if (!(entity instanceof Mob mob) || entity.level().isClientSide()) {
            return;
        }
        ShadowBinding binding = BINDINGS_BY_TARGET.get(entity.getUUID());
        if (binding != null && binding.controlActive) {
            lockTarget(mob, binding);
        }
    }

    @Nullable
    public static LivingEntity forcedTarget(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return null;
        }
        ShadowBinding binding = BINDINGS_BY_TARGET.get(entity.getUUID());
        return binding == null || !binding.controlActive ? null : bindingSource(entity, binding);
    }

    /** Removes hostile projectiles before their movement can cross a bound target's enlarged shadow shell. */
    public static boolean interceptProjectile(Projectile projectile) {
        if (projectile.level().isClientSide() || projectile.isRemoved()) {
            return false;
        }
        MinecraftServer server = projectile.getServer();
        if (server == null) {
            return false;
        }

        Entity attacker = projectile.getOwner();
        ShadowBinding attackerBinding = attacker == null ? null : BINDINGS_BY_TARGET.get(attacker.getUUID());
        if (attackerBinding != null && attackerBinding.dimension.equals(projectile.level().dimension())) {
            LivingEntity boundAttacker = living(server, attackerBinding.dimension, attackerBinding.targetId);
            if (boundAttacker != null) {
                spawnProjectileBlock(boundAttacker, attackerBinding, projectile);
                projectile.discard();
                return true;
            }
        }

        AABB projectileBounds = projectile.getBoundingBox();
        Vec3 start = projectileBounds.getCenter();
        Vec3 end = start.add(projectile.getDeltaMovement());
        for (ShadowBinding binding : BINDINGS_BY_TARGET.values()) {
            if (!binding.dimension.equals(projectile.level().dimension()) || isFriendly(binding, attacker, server)) {
                continue;
            }
            LivingEntity target = living(server, binding.dimension, binding.targetId);
            if (target == null) {
                continue;
            }

            AABB barrier = target.getBoundingBox()
                .inflate(PROJECTILE_BARRIER_INFLATION)
                .inflate(
                    projectileBounds.getXsize() * 0.5D,
                    projectileBounds.getYsize() * 0.5D,
                    projectileBounds.getZsize() * 0.5D
                );
            if (!barrier.contains(start) && barrier.clip(start, end).isEmpty()) {
                continue;
            }

            spawnProjectileBlock(target, binding, projectile);
            projectile.discard();
            return true;
        }
        return false;
    }

    /** Lets Gate of Babylon weapons approach the binding source, then erases them before their explosive tick. */
    public static boolean interceptGateProjectile(Entity entity) {
        if (!(entity instanceof Projectile projectile)
            || entity.level().isClientSide()
            || entity.isRemoved()) {
            return false;
        }
        Entity owner = projectile.getOwner();
        LivingEntity attacker = owner instanceof LivingEntity living ? living : null;
        ShadowBinding binding = attacker == null ? null : BINDINGS_BY_TARGET.get(attacker.getUUID());
        if (binding == null || !binding.dimension.equals(entity.level().dimension())) {
            return false;
        }
        LivingEntity source = bindingSource(attacker, binding);
        if (source == null) {
            return false;
        }

        AABB projectileBounds = entity.getBoundingBox();
        Vec3 start = projectileBounds.getCenter();
        Vec3 end = start.add(entity.getDeltaMovement());
        AABB interceptZone = source.getBoundingBox()
            .inflate(GATE_PROJECTILE_INTERCEPT_DISTANCE)
            .inflate(
                projectileBounds.getXsize() * 0.5D,
                projectileBounds.getYsize() * 0.5D,
                projectileBounds.getZsize() * 0.5D
            );
        if (!interceptZone.contains(start) && interceptZone.clip(start, end).isEmpty()) {
            return false;
        }

        spawnGateProjectileErasure(source, entity);
        entity.discard();
        return true;
    }

    public static void tick(MinecraftServer server) {
        Iterator<ShadowBinding> iterator = BINDINGS_BY_TARGET.values().iterator();
        while (iterator.hasNext()) {
            ShadowBinding binding = iterator.next();
            LivingEntity target = living(server, binding.dimension, binding.targetId);
            if (target == null) {
                iterator.remove();
                removeSourceMappings(binding);
                continue;
            }

            boolean sourceWithinControlRange = false;
            Iterator<UUID> sources = binding.sourceIds.iterator();
            while (sources.hasNext()) {
                UUID sourceId = sources.next();
                LivingEntity source = living(server, binding.dimension, sourceId);
                if (source == null || !source.isAlive()) {
                    if (binding.controlActive) {
                        sources.remove();
                        removeSourceMapping(sourceId, binding.targetId);
                    } else {
                        sendBindingVisual(target, -1, GrailParticleService.BLACK);
                    }
                    continue;
                }

                boolean withinRange = source instanceof ShadowFamiliarEntity familiar
                    ? familiar.isWithinShadowBindingRange(target)
                    : source.distanceToSqr(target) <= 50.0D * 50.0D;
                if (withinRange) {
                    sourceWithinControlRange = true;
                    PollutionService.expose(target, source, true);
                }

                sendBindingVisual(target, source.getId(), GrailParticleService.palette(source));
            }
            if (binding.sourceIds.isEmpty()) {
                iterator.remove();
                if (binding.controlActive) {
                    restoreTarget(target, binding);
                }
                continue;
            }

            if (binding.controlActive && !sourceWithinControlRange) {
                suspendControl(target, binding);
            } else if (binding.controlActive) {
                maintainLock(target, binding);
            }
        }
    }

    public static void entityLeavingLevel(Entity entity) {
        ShadowBinding targetBinding = BINDINGS_BY_TARGET.get(entity.getUUID());
        if (targetBinding != null) {
            release(targetBinding, entity.getServer(), false);
        }
        Set<UUID> sourceTargetIds = TARGETS_BY_SOURCE.get(entity.getUUID());
        if (sourceTargetIds != null) {
            for (UUID targetId : Set.copyOf(sourceTargetIds)) {
                ShadowBinding sourceBinding = BINDINGS_BY_TARGET.get(targetId);
                if (sourceBinding != null
                    && (sourceBinding.controlActive || entity instanceof BlackShadowEntity)) {
                    removeSource(sourceBinding, entity.getUUID(), entity.getServer(), true);
                } else if (sourceBinding == null) {
                    removeSourceMapping(entity.getUUID(), targetId);
                }
            }
        }
    }

    public static void serverStopping(MinecraftServer server) {
        Set<ShadowBinding> bindings = new HashSet<>(BINDINGS_BY_TARGET.values());
        BINDINGS_BY_TARGET.clear();
        TARGETS_BY_SOURCE.clear();
        for (ShadowBinding binding : bindings) {
            LivingEntity target = living(server, binding.dimension, binding.targetId);
            if (target != null && binding.controlActive) {
                restoreTarget(target, binding);
            }
        }
    }

    private static void release(ShadowBinding binding, MinecraftServer server, boolean restoreTarget) {
        BINDINGS_BY_TARGET.remove(binding.targetId);
        removeSourceMappings(binding);
        if (!restoreTarget || server == null) {
            return;
        }
        LivingEntity target = living(server, binding.dimension, binding.targetId);
        if (target != null && binding.controlActive) {
            restoreTarget(target, binding);
        }
    }

    private static void removeSource(
        ShadowBinding binding,
        UUID sourceId,
        MinecraftServer server,
        boolean restoreTarget
    ) {
        removeSourceMapping(sourceId, binding.targetId);
        if (!binding.sourceIds.remove(sourceId)) {
            return;
        }
        if (!binding.sourceIds.isEmpty()) {
            LivingEntity target = server == null ? null : living(server, binding.dimension, binding.targetId);
            if (target != null && binding.controlActive) {
                maintainLock(target, binding);
            }
            return;
        }
        BINDINGS_BY_TARGET.remove(binding.targetId);
        if (!restoreTarget || server == null) {
            return;
        }
        LivingEntity target = living(server, binding.dimension, binding.targetId);
        if (target != null && binding.controlActive) {
            restoreTarget(target, binding);
        }
    }

    private static void removeSourceMappings(ShadowBinding binding) {
        for (UUID sourceId : binding.sourceIds) {
            removeSourceMapping(sourceId, binding.targetId);
        }
    }

    private static void removeSourceMapping(UUID sourceId, UUID targetId) {
        Set<UUID> targets = TARGETS_BY_SOURCE.get(sourceId);
        if (targets == null || !targets.remove(targetId)) {
            return;
        }
        if (targets.isEmpty()) {
            TARGETS_BY_SOURCE.remove(sourceId);
        }
    }

    private static boolean isFriendly(
        ShadowBinding binding,
        @Nullable Entity attacker,
        @Nullable MinecraftServer server
    ) {
        if (attacker == null || server == null) {
            return false;
        }
        for (UUID sourceId : binding.sourceIds) {
            LivingEntity source = living(server, binding.dimension, sourceId);
            if (source != null
                && (source == attacker || source.isAlliedTo(attacker) || attacker.isAlliedTo(source))) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static Entity resolveAttacker(DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker == null) {
            attacker = source.getDirectEntity();
        }
        if (attacker instanceof Projectile projectile && projectile.getOwner() != null) {
            return projectile.getOwner();
        }
        return attacker;
    }

    private static void maintainLock(LivingEntity target, ShadowBinding binding) {
        LivingEntity mudSource = mudBindingSource(target, binding);
        LivingEntity source = mudSource != null ? mudSource : bindingSource(target, binding);
        boolean forceMaximumMudControl = mudSource != null && !BlackMudService.isOnOrInBlackMud(target);
        applyDistanceControl(target, source, forceMaximumMudControl);
        if (target instanceof Mob mob) {
            mob.setNoAi(binding.targetWasNoAi);
            if (source != null && mob.getTarget() != source) {
                mob.setTarget(source);
            }
        }
    }

    private static void applyDistanceControl(
        LivingEntity target,
        @Nullable LivingEntity source,
        boolean forceMaximumMudControl
    ) {
        if (source == null) {
            target.removeEffect(ModEffects.SHADOW_BINDING_SLOWNESS);
            return;
        }

        Vec3 targetCenter = target.getBoundingBox().getCenter();
        Vec3 offset = source.getBoundingBox().getCenter().subtract(targetCenter);
        double distance = offset.length();
        if (!forceMaximumMudControl && distance <= CONTROL_DEAD_ZONE) {
            target.removeEffect(ModEffects.SHADOW_BINDING_SLOWNESS);
            return;
        }

        double slowStep = (MAXIMUM_SLOWNESS_DISTANCE - CONTROL_DEAD_ZONE) / MAXIMUM_SLOWNESS_LEVEL;
        int slownessLevel = forceMaximumMudControl
            ? MAXIMUM_SLOWNESS_LEVEL
            : Mth.clamp(
                (int)Math.ceil((distance - CONTROL_DEAD_ZONE) / slowStep),
                1,
                MAXIMUM_SLOWNESS_LEVEL
            );
        int amplifier = slownessLevel - 1;
        MobEffectInstance current = target.getEffect(ModEffects.SHADOW_BINDING_SLOWNESS);
        if (current != null && current.getAmplifier() != amplifier) {
            target.removeEffect(ModEffects.SHADOW_BINDING_SLOWNESS);
        }
        target.forceAddEffect(
            new MobEffectInstance(ModEffects.SHADOW_BINDING_SLOWNESS, 10, amplifier, false, true, true),
            null
        );

        Vec3 velocity = target.getDeltaMovement();
        if (!target.onGround()) {
            double airSpeedMultiplier = Math.max(0.1D, 1.0D - 0.15D * slownessLevel);
            velocity = velocity.scale(airSpeedMultiplier);
        }

        double pullProgress = forceMaximumMudControl
            ? 1.0D
            : Mth.clamp(
                (distance - CONTROL_DEAD_ZONE) / (MAXIMUM_SLOWNESS_DISTANCE - CONTROL_DEAD_ZONE),
                0.0D,
                1.0D
            );
        double remainingDistance = forceMaximumMudControl ? distance : distance - CONTROL_DEAD_ZONE;
        double pullDistance = Math.min(MAXIMUM_PULL_SPEED_PER_TICK * pullProgress, remainingDistance);
        Vec3 pullDirection = offset.normalize();
        double currentPullSpeed = velocity.dot(pullDirection);
        target.setDeltaMovement(velocity.add(pullDirection.scale(pullDistance - currentPullSpeed)));
        target.hurtMarked = true;
    }

    @Nullable
    private static LivingEntity mudBindingSource(LivingEntity target, ShadowBinding binding) {
        if (BlackMudService.isOnOrInBlackMud(target) || target.getServer() == null) {
            return null;
        }
        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (UUID sourceId : binding.sourceIds) {
            LivingEntity source = living(target.getServer(), binding.dimension, sourceId);
            if (!(source instanceof BlackShadowEntity || source instanceof ShadowFamiliarEntity)
                || !BlackMudService.isOnOrInBlackMud(source)) {
                continue;
            }
            boolean withinRange = source instanceof ShadowFamiliarEntity familiar
                ? familiar.isWithinShadowBindingRange(target)
                : source.distanceToSqr(target) <= 50.0D * 50.0D;
            if (!withinRange) {
                continue;
            }
            double distance = target.distanceToSqr(source);
            if (distance < closestDistance) {
                closest = source;
                closestDistance = distance;
            }
        }
        return closest;
    }

    private static void suspendControl(LivingEntity target, ShadowBinding binding) {
        binding.controlActive = false;
        restoreTarget(target, binding);
    }

    private static void sendBindingVisual(LivingEntity target, int sourceEntityId, byte palette) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
            target,
            new ShadowBindingEffectPayload(sourceEntityId, target.getId(), 3, palette)
        );
    }

    private static void lockTarget(Mob target, ShadowBinding binding) {
        LivingEntity source = bindingSource(target, binding);
        if (source != null && target.getTarget() != source) {
            target.setTarget(source);
        }
    }

    @Nullable
    private static LivingEntity bindingSource(LivingEntity target, ShadowBinding binding) {
        MinecraftServer server = target.getServer();
        if (server == null) {
            return null;
        }
        LivingEntity current = target instanceof Mob mob ? mob.getTarget() : null;
        if (current != null && binding.sourceIds.contains(current.getUUID()) && current.isAlive()) {
            return current;
        }

        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (UUID sourceId : binding.sourceIds) {
            LivingEntity source = living(server, binding.dimension, sourceId);
            if (source == null) {
                continue;
            }
            double distance = target.distanceToSqr(source);
            if (distance < closestDistance) {
                closest = source;
                closestDistance = distance;
            }
        }
        return closest;
    }

    private static void restoreTarget(LivingEntity target, ShadowBinding binding) {
        target.removeEffect(ModEffects.SHADOW_BINDING_SLOWNESS);
        if (target instanceof Mob mob) {
            mob.setNoAi(ImaginaryShadowService.preventsServantAction(target) || binding.targetWasNoAi);
            LivingEntity originalTarget = binding.originalTargetId == null || target.getServer() == null
                ? null
                : living(target.getServer(), binding.dimension, binding.originalTargetId);
            mob.setTarget(originalTarget);
        }
    }

    private static void spawnShellHit(LivingEntity target, ShadowBinding binding, boolean broken) {
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        byte palette = GrailParticleService.BLACK;
        for (UUID sourceId : binding.sourceIds) {
            LivingEntity source = living(level.getServer(), binding.dimension, sourceId);
            if (source != null) {
                palette = GrailParticleService.highest(palette, GrailParticleService.palette(source));
            }
        }
        GrailParticleService.send(
            level,
            palette,
            ParticleTypes.SQUID_INK,
            target.getX(),
            target.getY() + target.getBbHeight() * 0.5D,
            target.getZ(),
            broken ? 32 : 12,
            target.getBbWidth() * 0.7D,
            target.getBbHeight() * 0.45D,
            target.getBbWidth() * 0.7D,
            broken ? 0.08D : 0.025D
        );
        level.playSound(
            null,
            target.blockPosition(),
            broken ? SoundEvents.SHIELD_BREAK : SoundEvents.SHIELD_BLOCK,
            SoundSource.HOSTILE,
            broken ? 1.0F : 0.65F,
            broken ? 0.55F : 0.75F
        );
    }

    private static void spawnProjectileBlock(LivingEntity target, ShadowBinding binding, Projectile projectile) {
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        byte palette = GrailParticleService.BLACK;
        for (UUID sourceId : binding.sourceIds) {
            LivingEntity source = living(level.getServer(), binding.dimension, sourceId);
            if (source != null) {
                palette = GrailParticleService.highest(palette, GrailParticleService.palette(source));
            }
        }
        GrailParticleService.send(
            level,
            palette,
            ParticleTypes.SQUID_INK,
            projectile.getX(),
            projectile.getY(0.5D),
            projectile.getZ(),
            8,
            0.18D,
            0.18D,
            0.18D,
            0.02D
        );
        level.playSound(
            null,
            projectile.blockPosition(),
            SoundEvents.SHIELD_BLOCK,
            SoundSource.HOSTILE,
            0.55F,
            0.7F
        );
    }

    private static void spawnGateProjectileErasure(
        LivingEntity source,
        Entity projectile
    ) {
        if (!(source.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = projectile.getBoundingBox().getCenter();
        double spread = Math.max(0.3D, projectile.getBoundingBox().getSize() * 0.65D);
        GrailParticleService.send(
            level,
            source,
            ParticleTypes.SQUID_INK,
            center.x,
            center.y,
            center.z,
            24,
            spread,
            spread,
            spread,
            0.035D
        );
        GrailParticleService.send(
            level,
            source,
            ParticleTypes.REVERSE_PORTAL,
            center.x,
            center.y,
            center.z,
            18,
            spread * 0.75D,
            spread * 0.75D,
            spread * 0.75D,
            0.02D
        );
        level.playSound(
            null,
            projectile.blockPosition(),
            SoundEvents.SHIELD_BLOCK,
            SoundSource.HOSTILE,
            0.7F,
            0.55F
        );
    }

    private static LivingEntity living(MinecraftServer server, ResourceKey<Level> dimension, UUID id) {
        ServerLevel level = server.getLevel(dimension);
        Entity entity = level == null ? null : level.getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private static final class ShadowBinding {
        private final Set<UUID> sourceIds = new HashSet<>();
        private final UUID targetId;
        private final ResourceKey<Level> dimension;
        private final boolean targetWasNoAi;
        @Nullable
        private final UUID originalTargetId;
        private float shellHealth;
        private boolean controlActive = true;

        private ShadowBinding(
            UUID targetId,
            ResourceKey<Level> dimension,
            boolean targetWasNoAi,
            @Nullable UUID originalTargetId,
            float shellHealth
        ) {
            this.targetId = targetId;
            this.dimension = dimension;
            this.targetWasNoAi = targetWasNoAi;
            this.originalTargetId = originalTargetId;
            this.shellHealth = shellHealth;
        }
    }

    private ShadowBindingService() {
    }
}
