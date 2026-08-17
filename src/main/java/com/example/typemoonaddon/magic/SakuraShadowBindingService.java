package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.registry.AddonMobEffects;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class SakuraShadowBindingService {
    public static final float PARTICLE_SHELL_HEALTH = 300.0F;
    public static final float ADDITIONAL_SOURCE_HEALTH = 50.0F;
    public static final int MAX_SOURCES_PER_TARGET = 5;
    private static final double CONTROL_DEAD_ZONE = 10.0D;
    private static final double MAXIMUM_SLOWNESS_DISTANCE = 40.0D;
    private static final int MAXIMUM_SLOWNESS_LEVEL = 6;
    private static final double MAXIMUM_PULL_SPEED_PER_TICK = 1.0D / 20.0D;
    private static final Map<UUID, ShadowBinding> BINDINGS_BY_TARGET = new HashMap<>();
    private static final Map<UUID, Set<UUID>> TARGETS_BY_SOURCE = new HashMap<>();

    public static boolean canBegin(LivingEntity source, @Nullable LivingEntity target) {
        if (!source.isAlive() || target == null || !target.isAlive() || source == target || source.level() != target.level()) {
            return false;
        }
        Set<UUID> sourceTargets = TARGETS_BY_SOURCE.get(source.getUUID());
        int sourceTargetLimit = 1;
        if (sourceTargets != null && (sourceTargets.contains(target.getUUID()) || sourceTargets.size() >= sourceTargetLimit)) {
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
        ShadowBinding binding = BINDINGS_BY_TARGET.get(target.getUUID());
        if (binding == null) {
            binding = new ShadowBinding(
                    target.getUUID(),
                    target.level().dimension(),
                    target instanceof Mob mob && mob.isNoAi(),
                    target instanceof Mob mob && mob.getTarget() != null ? mob.getTarget().getUUID() : null,
                    PARTICLE_SHELL_HEALTH
            );
            BINDINGS_BY_TARGET.put(target.getUUID(), binding);
        } else {
            binding.shellHealth += ADDITIONAL_SOURCE_HEALTH;
        }
        binding.sourceIds.add(sourceId);
        TARGETS_BY_SOURCE.computeIfAbsent(sourceId, ignored -> new HashSet<>()).add(target.getUUID());
        maintainLock(target, binding);
        target.level().playSound(null, target.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 0.9F, 0.5F);
        return true;
    }

    public static boolean isBound(LivingEntity target) {
        return BINDINGS_BY_TARGET.containsKey(target.getUUID());
    }

    public static boolean hasSourceBinding(Entity source) {
        Set<UUID> targets = TARGETS_BY_SOURCE.get(source.getUUID());
        return targets != null && !targets.isEmpty();
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
            } else {
                removeSource(binding, source.getUUID(), source.getServer(), true);
            }
        }
    }

    public static boolean absorbDamage(LivingIncomingDamageEvent event) {
        if (isFriendlyDamage(event.getEntity(), event.getSource()) || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return absorbDirectDamage(event.getEntity(), resolveAttacker(event.getSource()), event.getAmount());
    }

    public static boolean absorbDirectDamage(LivingEntity target, @Nullable Entity attacker, float amount) {
        ShadowBinding binding = BINDINGS_BY_TARGET.get(target.getUUID());
        if (binding == null || amount <= 0.0F || isFriendly(binding, attacker, target.getServer())) {
            return false;
        }
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
        return binding != null && isFriendly(binding, resolveAttacker(source), target.getServer());
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
            boolean sourceWithinRange = false;
            Iterator<UUID> sources = binding.sourceIds.iterator();
            while (sources.hasNext()) {
                UUID sourceId = sources.next();
                LivingEntity source = living(server, binding.dimension, sourceId);
                if (source == null || !source.isAlive()) {
                    sources.remove();
                    removeSourceMapping(sourceId, binding.targetId);
                    continue;
                }
                if (source.distanceToSqr(target) <= 50.0D * 50.0D) {
                    sourceWithinRange = true;
                }
                if (target.level() instanceof ServerLevel level && target.tickCount % 3 == 0) {
                    SakuraParticleService.send(level, ParticleTypes.SQUID_INK, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 2, target.getBbWidth() * 0.7D, target.getBbHeight() * 0.4D, target.getBbWidth() * 0.7D, 0.015D);
                }
            }
            if (binding.sourceIds.isEmpty()) {
                iterator.remove();
                restoreTarget(target, binding);
            } else if (sourceWithinRange) {
                maintainLock(target, binding);
            } else {
                restoreTarget(target, binding);
                binding.controlActive = false;
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
                ShadowBinding binding = BINDINGS_BY_TARGET.get(targetId);
                if (binding != null) {
                    removeSource(binding, entity.getUUID(), entity.getServer(), true);
                } else {
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
            if (target != null) {
                restoreTarget(target, binding);
            }
        }
    }

    private static void maintainLock(LivingEntity target, ShadowBinding binding) {
        binding.controlActive = true;
        LivingEntity source = bindingSource(target, binding);
        applyDistanceControl(target, source, SakuraBlackMudService.isOnOrInBlackMud(target));
        if (target instanceof Mob mob) {
            mob.setNoAi(binding.targetWasNoAi);
            if (source != null && mob.getTarget() != source) {
                mob.setTarget(source);
            }
        }
    }

    private static void applyDistanceControl(LivingEntity target, @Nullable LivingEntity source, boolean forceMaximumMudControl) {
        if (source == null) {
            target.removeEffect(AddonMobEffects.SHADOW_BINDING_SLOWNESS);
            return;
        }
        Vec3 offset = source.getBoundingBox().getCenter().subtract(target.getBoundingBox().getCenter());
        double distance = offset.length();
        if (!forceMaximumMudControl && distance <= CONTROL_DEAD_ZONE) {
            target.removeEffect(AddonMobEffects.SHADOW_BINDING_SLOWNESS);
            return;
        }
        double slowStep = (MAXIMUM_SLOWNESS_DISTANCE - CONTROL_DEAD_ZONE) / MAXIMUM_SLOWNESS_LEVEL;
        int slownessLevel = forceMaximumMudControl ? MAXIMUM_SLOWNESS_LEVEL : Mth.clamp((int) Math.ceil((distance - CONTROL_DEAD_ZONE) / slowStep), 1, MAXIMUM_SLOWNESS_LEVEL);
        target.forceAddEffect(new MobEffectInstance(AddonMobEffects.SHADOW_BINDING_SLOWNESS, 10, slownessLevel - 1, false, true, true), null);
        Vec3 velocity = target.getDeltaMovement();
        double pullProgress = forceMaximumMudControl ? 1.0D : Mth.clamp((distance - CONTROL_DEAD_ZONE) / (MAXIMUM_SLOWNESS_DISTANCE - CONTROL_DEAD_ZONE), 0.0D, 1.0D);
        double remainingDistance = forceMaximumMudControl ? distance : distance - CONTROL_DEAD_ZONE;
        double pullDistance = Math.min(MAXIMUM_PULL_SPEED_PER_TICK * pullProgress, remainingDistance);
        Vec3 pullDirection = offset.normalize();
        double currentPullSpeed = velocity.dot(pullDirection);
        target.setDeltaMovement(velocity.add(pullDirection.scale(pullDistance - currentPullSpeed)));
        target.hurtMarked = true;
    }

    @Nullable
    private static LivingEntity bindingSource(LivingEntity target, ShadowBinding binding) {
        MinecraftServer server = target.getServer();
        if (server == null) {
            return null;
        }
        LivingEntity closest = null;
        double best = Double.MAX_VALUE;
        for (UUID sourceId : binding.sourceIds) {
            LivingEntity source = living(server, binding.dimension, sourceId);
            if (source != null && target.distanceToSqr(source) < best) {
                closest = source;
                best = target.distanceToSqr(source);
            }
        }
        return closest;
    }

    private static void restoreTarget(LivingEntity target, ShadowBinding binding) {
        target.removeEffect(AddonMobEffects.SHADOW_BINDING_SLOWNESS);
        if (target instanceof Mob mob) {
            mob.setNoAi(binding.targetWasNoAi);
            LivingEntity originalTarget = binding.originalTargetId == null || target.getServer() == null ? null : living(target.getServer(), binding.dimension, binding.originalTargetId);
            mob.setTarget(originalTarget);
        }
    }

    private static void release(ShadowBinding binding, MinecraftServer server, boolean restoreTarget) {
        BINDINGS_BY_TARGET.remove(binding.targetId);
        removeSourceMappings(binding);
        if (restoreTarget && server != null) {
            LivingEntity target = living(server, binding.dimension, binding.targetId);
            if (target != null) {
                restoreTarget(target, binding);
            }
        }
    }

    private static void removeSource(ShadowBinding binding, UUID sourceId, MinecraftServer server, boolean restoreTarget) {
        removeSourceMapping(sourceId, binding.targetId);
        if (!binding.sourceIds.remove(sourceId) || !binding.sourceIds.isEmpty()) {
            return;
        }
        BINDINGS_BY_TARGET.remove(binding.targetId);
        if (restoreTarget && server != null) {
            LivingEntity target = living(server, binding.dimension, binding.targetId);
            if (target != null) {
                restoreTarget(target, binding);
            }
        }
    }

    private static void removeSourceMappings(ShadowBinding binding) {
        for (UUID sourceId : binding.sourceIds) {
            removeSourceMapping(sourceId, binding.targetId);
        }
    }

    private static void removeSourceMapping(UUID sourceId, UUID targetId) {
        Set<UUID> targets = TARGETS_BY_SOURCE.get(sourceId);
        if (targets != null && targets.remove(targetId) && targets.isEmpty()) {
            TARGETS_BY_SOURCE.remove(sourceId);
        }
    }

    private static boolean isFriendly(ShadowBinding binding, @Nullable Entity attacker, @Nullable MinecraftServer server) {
        if (attacker == null || server == null) {
            return false;
        }
        for (UUID sourceId : binding.sourceIds) {
            LivingEntity source = living(server, binding.dimension, sourceId);
            if (source != null && (source == attacker || source.isAlliedTo(attacker) || attacker.isAlliedTo(source))) {
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
        return attacker instanceof Projectile projectile && projectile.getOwner() != null ? projectile.getOwner() : attacker;
    }

    private static void spawnShellHit(LivingEntity target, ShadowBinding binding, boolean broken) {
        if (target.level() instanceof ServerLevel level) {
            SakuraParticleService.send(level, ParticleTypes.SQUID_INK, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), broken ? 32 : 12, target.getBbWidth() * 0.7D, target.getBbHeight() * 0.45D, target.getBbWidth() * 0.7D, broken ? 0.08D : 0.025D);
            level.playSound(null, target.blockPosition(), broken ? SoundEvents.SHIELD_BREAK : SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, broken ? 1.0F : 0.65F, broken ? 0.55F : 0.75F);
        }
    }

    @Nullable
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

        private ShadowBinding(UUID targetId, ResourceKey<Level> dimension, boolean targetWasNoAi, @Nullable UUID originalTargetId, float shellHealth) {
            this.targetId = targetId;
            this.dimension = dimension;
            this.targetWasNoAi = targetWasNoAi;
            this.originalTargetId = originalTargetId;
            this.shellHealth = shellHealth;
        }
    }

    private SakuraShadowBindingService() {
    }
}
