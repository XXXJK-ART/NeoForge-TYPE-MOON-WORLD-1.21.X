package com.example.typemoonaddon.antores;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.network.AntoresBeamVisualPayload;
import com.example.typemoonaddon.network.AddonSpellVisualPayload;
import com.example.typemoonaddon.registry.AddonMobEffects;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.example.typemoonaddon.network.AddonNetwork;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.DeferredTerrainDestruction;

public final class AntoresService {
    public static final double MAX_RANGE = 60.0D;
    public static final int BEAM_COUNT = 6;
    public static final float MAX_DIRECT_DAMAGE_PER_TARGET = 200.0F;
    public static final float DAMAGE_PER_BEAM = MAX_DIRECT_DAMAGE_PER_TARGET / BEAM_COUNT;
    public static final int CURSE_DURATION_TICKS = 200;
    public static final int CURSE_AMPLIFIER = 1;
    public static final int CURSE_DAMAGE_INTERVAL_TICKS = 20;
    public static final int CURSE_MAX_PULSES = 10;
    public static final float CURSE_DAMAGE_PER_PULSE = 5.0F;

    public static final ResourceKey<DamageType> CURSE_DAMAGE = ResourceKey.create(
            net.minecraft.core.registries.Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "demon_god_curse")
    );

    private static final double EXCALIBUR_REFERENCE_RANGE = 150.0D;
    private static final double BEAM_COLLISION_RADIUS = 0.72D;
    private static final double TERRAIN_CUT_WIDTH = 2.6D;
    private static final double TERRAIN_CUT_HEIGHT = 2.6D;
    private static final double MIN_DISTANCE_DAMAGE_MULTIPLIER = 0.35D;
    private static final int CAST_DEBOUNCE_TICKS = 2;
    private static final double VFX_OBSERVER_RADIUS = 144.0D;

    // Keep the large gates well outside the caster's first-person sightline.
    private static final double[][] BEAM_GATE_OFFSETS = {
            {-4.40D, 0.75D, -2.20D},
            {4.40D, 0.75D, -2.20D},
            {-5.20D, 2.15D, -2.45D},
            {5.20D, 2.15D, -2.45D},
            {-4.45D, 3.85D, -2.70D},
            {4.45D, 3.85D, -2.70D}
    };

    private static final String RELEASE_EFFECT = "typemoonworld:antores_release";
    private static final String BEAM_EFFECT = "typemoonworld:antores_beam";
    private static final String BEAM_SIGILLUM_EFFECT = "typemoonworld:antores_beam_sigillum";
    private static final String HIT_EFFECT = "typemoonworld:antores_hit";
    private static final String CURSE_EFFECT = "typemoonworld:antores_curse";

    private static final String TAG_CURSE_CASTER = "TypeMoonAddonAntoresCurseCaster";
    private static final String TAG_CURSE_DIMENSION = "TypeMoonAddonAntoresCurseDimension";
    private static final String TAG_CURSE_NEXT_DAMAGE = "TypeMoonAddonAntoresCurseNextDamage";
    private static final String TAG_CURSE_PULSES = "TypeMoonAddonAntoresCursePulses";

    private static final Map<UUID, Long> LAST_SUCCESSFUL_CAST_TICK = new HashMap<>();

    private AntoresService() {
    }

    public static CastOutcome cast(ServerPlayer caster) {
        if (caster == null || !caster.isAlive() || caster.isRemoved() || caster.isSpectator()) {
            return CastOutcome.REJECTED;
        }

        ServerLevel level = caster.serverLevel();
        long now = level.getGameTime();
        Long lastCast = LAST_SUCCESSFUL_CAST_TICK.get(caster.getUUID());
        if (lastCast != null && now >= lastCast && now - lastCast < CAST_DEBOUNCE_TICKS) {
            return CastOutcome.REJECTED;
        }

        Vec3 look = caster.getViewVector(1.0F);
        if (look.lengthSqr() < 1.0E-6D) {
            look = caster.getLookAngle();
        }
        if (look.lengthSqr() < 1.0E-6D) {
            return CastOutcome.REJECTED;
        }
        look = look.normalize();
        AddonSpellVisualPayload.showAttached(
                level, AddonSpellVisualPayload.ANTORES, AddonSpellVisualPayload.SIGIL,
                caster.getUUID(), caster, 1.0F, 1.0F, 24, 0, VFX_OBSERVER_RADIUS);

        Vec3 aimPoint = caster.getEyePosition(1.0F).add(look.scale(MAX_RANGE));
        Map<UUID, AggregatedHit> targetHits = new HashMap<>();
        List<AntoresBeamVisualPayload.BeamPath> visualBeams = new ArrayList<>(BEAM_COUNT);
        for (int beamIndex = 0; beamIndex < BEAM_COUNT; beamIndex++) {
            Vec3 gateStart = beamGateStart(caster, beamIndex, look);
            double fanDegrees = Mth.lerp(beamIndex / (double) (BEAM_COUNT - 1), -4.5D, 4.5D);
            Vec3 traceDirection = rotateAroundY(aimPoint.subtract(gateStart).normalize(), fanDegrees);
            BeamTrace trace = traceBeam(level, caster, gateStart, traceDirection);
            Vec3 gateDirection = traceDirection;
            visualBeams.add(AntoresBeamVisualPayload.BeamPath.between(gateStart, trace.end()));

            VFXServerEffects.spawn(level, RELEASE_EFFECT, gateStart, VFX_OBSERVER_RADIUS);
            VFXServerEffects.spawnOriented(
                    level, BEAM_SIGILLUM_EFFECT, gateStart, gateDirection, VFX_OBSERVER_RADIUS);
            VFXServerEffects.spawnOriented(
                    level, BEAM_EFFECT, gateStart, gateDirection, VFX_OBSERVER_RADIUS);
            queueTerrainCut(level, gateStart, traceDirection);
            if (trace.target() != null) {
                AggregatedHit hit = targetHits.computeIfAbsent(
                        trace.target().getUUID(), ignored -> new AggregatedHit(trace.target()));
                hit.record(trace.distance());
            } else if (trace.blocked()) {
                VFXServerEffects.spawn(level, HIT_EFFECT, trace.end(), VFX_OBSERVER_RADIUS);
            }
        }
        AddonNetwork.sendNear(
                level,
                caster.getX(),
                caster.getY(),
                caster.getZ(),
                VFX_OBSERVER_RADIUS,
                new AntoresBeamVisualPayload(visualBeams)
        );

        int damagedTargets = 0;
        DamageSource directSource = caster.damageSources().source(DamageTypes.MAGIC, caster);
        for (AggregatedHit hit : targetHits.values()) {
            LivingEntity target = hit.target;
            if (!EntityUtils.isValidCombatTarget(caster, target)) {
                continue;
            }

            double distanceMultiplier = distanceDamageMultiplier(hit.closestDistance);
            float requestedDamage = (float) Mth.clamp(
                    DAMAGE_PER_BEAM * hit.beamHits * distanceMultiplier,
                    0.0D,
                    MAX_DIRECT_DAMAGE_PER_TARGET
            );
            if (!Float.isFinite(requestedDamage) || requestedDamage <= 0.0F) {
                continue;
            }

            int previousInvulnerability = target.invulnerableTime;
            target.invulnerableTime = 0;
            boolean hurt = target.hurt(directSource, requestedDamage);
            if (hurt) {
                target.invulnerableTime = 0;
                damagedTargets++;
                Vec3 impact = target.getBoundingBox().getCenter();
                VFXServerEffects.spawn(level, HIT_EFFECT, impact, VFX_OBSERVER_RADIUS);
                applyCurse(caster, target, now);
                EntityUtils.triggerSwarmAnger(level, caster, target);
            } else {
                target.invulnerableTime = previousInvulnerability;
            }
        }

        LAST_SUCCESSFUL_CAST_TICK.put(caster.getUUID(), now);
        return new CastOutcome(true, damagedTargets);
    }

    public static void tickCurse(LivingEntity target) {
        if (target == null || target.level().isClientSide()) {
            return;
        }
        if (!target.hasEffect(AddonMobEffects.DEMON_GOD_CURSE)) {
            clearCurseData(target);
            return;
        }
        if (!(target.level() instanceof ServerLevel level) || !target.isAlive() || target.isRemoved()) {
            clearCurse(target);
            return;
        }

        CompoundTag data = target.getPersistentData();
        String dimensionId = level.dimension().location().toString();
        if (!dimensionId.equals(data.getString(TAG_CURSE_DIMENSION))) {
            clearCurse(target);
            return;
        }

        int pulses = Mth.clamp(data.getInt(TAG_CURSE_PULSES), 0, CURSE_MAX_PULSES);
        if (pulses >= CURSE_MAX_PULSES) {
            return;
        }

        long now = level.getGameTime();
        long nextDamageTick = data.getLong(TAG_CURSE_NEXT_DAMAGE);
        if (nextDamageTick <= 0L) {
            data.putLong(TAG_CURSE_NEXT_DAMAGE, now + CURSE_DAMAGE_INTERVAL_TICKS);
            return;
        }
        if (now < nextDamageTick) {
            return;
        }

        ServerPlayer caster = null;
        if (data.hasUUID(TAG_CURSE_CASTER)) {
            caster = level.getServer().getPlayerList().getPlayer(data.getUUID(TAG_CURSE_CASTER));
        }
        applyCurseDamage(level, target, caster);

        data.putInt(TAG_CURSE_PULSES, pulses + 1);
        data.putLong(TAG_CURSE_NEXT_DAMAGE, now + CURSE_DAMAGE_INTERVAL_TICKS);
    }

    public static void clearCurse(LivingEntity target) {
        if (target == null) {
            return;
        }
        target.removeEffect(AddonMobEffects.DEMON_GOD_CURSE);
        clearCurseData(target);
    }

    public static void clearRuntimeState() {
        LAST_SUCCESSFUL_CAST_TICK.clear();
    }

    public static double excaliburRangeRatio() {
        return MAX_RANGE / EXCALIBUR_REFERENCE_RANGE;
    }

    private static BeamTrace traceBeam(
            ServerLevel level,
            ServerPlayer caster,
            Vec3 start,
            Vec3 direction
    ) {
        Vec3 maximumEnd = start.add(direction.scale(MAX_RANGE));
        BlockHitResult blockHit = level.clip(new ClipContext(
                start,
                maximumEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                caster
        ));
        boolean blocked = blockHit.getType() != HitResult.Type.MISS;
        Vec3 blockLimitedEnd = blocked ? blockHit.getLocation() : maximumEnd;
        AABB searchArea = new AABB(start, blockLimitedEnd).inflate(BEAM_COLLISION_RADIUS);
        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                searchArea,
                target -> EntityUtils.isValidCombatTarget(caster, target)
        );

        LivingEntity closestTarget = null;
        Vec3 closestPoint = null;
        double closestDistanceSquared = Double.POSITIVE_INFINITY;
        for (LivingEntity candidate : candidates) {
            AABB hitBox = candidate.getBoundingBox().inflate(BEAM_COLLISION_RADIUS);
            Vec3 intersection;
            if (hitBox.contains(start)) {
                intersection = start;
            } else {
                Optional<Vec3> clipped = hitBox.clip(start, blockLimitedEnd);
                if (clipped.isEmpty()) {
                    continue;
                }
                intersection = clipped.get();
            }
            double distanceSquared = intersection.distanceToSqr(start);
            if (distanceSquared < closestDistanceSquared) {
                closestDistanceSquared = distanceSquared;
                closestTarget = candidate;
                closestPoint = intersection;
            }
        }

        if (closestTarget != null && closestPoint != null) {
            return new BeamTrace(
                    closestPoint,
                    closestTarget,
                    Math.sqrt(Math.max(0.0D, closestDistanceSquared)),
                    false
            );
        }
        return new BeamTrace(blockLimitedEnd, null, start.distanceTo(blockLimitedEnd), blocked);
    }

    private static Vec3 beamGateStart(ServerPlayer caster, int beamIndex, Vec3 look) {
        int clampedIndex = Mth.clamp(beamIndex, 0, BEAM_GATE_OFFSETS.length - 1);
        double[] offset = BEAM_GATE_OFFSETS[clampedIndex];

        Vec3 horizontalForward = new Vec3(look.x, 0.0D, look.z);
        if (horizontalForward.lengthSqr() < 1.0E-6D) {
            double yaw = Math.toRadians(caster.getYRot());
            horizontalForward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        } else {
            horizontalForward = horizontalForward.normalize();
        }
        Vec3 right = new Vec3(horizontalForward.z, 0.0D, -horizontalForward.x);
        return caster.position()
                .add(right.scale(offset[0]))
                .add(0.0D, offset[1], 0.0D)
                .add(horizontalForward.scale(offset[2]));
    }

    private static Vec3 rotateAroundY(Vec3 direction, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(
                direction.x * cos - direction.z * sin,
                direction.y,
                direction.x * sin + direction.z * cos
        ).normalize();
    }

    private static void queueTerrainCut(ServerLevel level, Vec3 start, Vec3 direction) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        DeferredTerrainDestruction.queueDirectionalCut(
                level,
                start,
                direction,
                MAX_RANGE,
                TERRAIN_CUT_WIDTH,
                TERRAIN_CUT_HEIGHT,
                false
        );
    }

    private static double distanceDamageMultiplier(double distance) {
        double normalized = Mth.clamp(distance / MAX_RANGE, 0.0D, 1.0D);
        if (normalized <= 0.2D) {
            return 1.0D;
        }
        return Mth.lerp(
                (normalized - 0.2D) / 0.8D,
                1.0D,
                MIN_DISTANCE_DAMAGE_MULTIPLIER
        );
    }

    private static void applyCurse(ServerPlayer caster, LivingEntity target, long currentTick) {
        MobEffectInstance curse = new MobEffectInstance(
                AddonMobEffects.DEMON_GOD_CURSE,
                CURSE_DURATION_TICKS,
                CURSE_AMPLIFIER,
                false,
                true,
                true
        );
        target.addEffect(curse, caster);
        if (!target.hasEffect(AddonMobEffects.DEMON_GOD_CURSE)) {
            return;
        }

        CompoundTag data = target.getPersistentData();
        data.putUUID(TAG_CURSE_CASTER, caster.getUUID());
        data.putString(TAG_CURSE_DIMENSION, target.level().dimension().location().toString());
        data.putLong(TAG_CURSE_NEXT_DAMAGE, currentTick + CURSE_DAMAGE_INTERVAL_TICKS);
        data.putInt(TAG_CURSE_PULSES, 1);
        VFXServerEffects.spawn(
                caster.serverLevel(), CURSE_EFFECT, target.getBoundingBox().getCenter(), VFX_OBSERVER_RADIUS);
        applyCurseDamage(caster.serverLevel(), target, caster);
    }

    private static void applyCurseDamage(
            ServerLevel level,
            LivingEntity target,
            ServerPlayer caster
    ) {
        DamageSource resistanceSource = caster != null
                ? caster.damageSources().source(DamageTypes.MAGIC, caster)
                : level.damageSources().magic();
        float adjustedDamage = MagicResistanceHelper.applyMagicDamageReduction(
                target, resistanceSource, CURSE_DAMAGE_PER_PULSE);
        if (!Float.isFinite(adjustedDamage) || adjustedDamage <= 0.0F) {
            return;
        }

        DamageSource curseSource = caster != null
                ? level.damageSources().source(CURSE_DAMAGE, caster)
                : level.damageSources().source(CURSE_DAMAGE);
        if (target.hurt(curseSource, adjustedDamage) && caster != null) {
            EntityUtils.triggerSwarmAnger(level, caster, target);
        }
    }

    private static void clearCurseData(LivingEntity target) {
        CompoundTag data = target.getPersistentData();
        data.remove(TAG_CURSE_CASTER);
        data.remove(TAG_CURSE_DIMENSION);
        data.remove(TAG_CURSE_NEXT_DAMAGE);
        data.remove(TAG_CURSE_PULSES);
    }

    public record CastOutcome(boolean accepted, int damagedTargets) {
        private static final CastOutcome REJECTED = new CastOutcome(false, 0);
    }

    private record BeamTrace(Vec3 end, LivingEntity target, double distance, boolean blocked) {
    }

    private static final class AggregatedHit {
        private final LivingEntity target;
        private int beamHits;
        private double closestDistance = MAX_RANGE;

        private AggregatedHit(LivingEntity target) {
            this.target = target;
        }

        private void record(double distance) {
            beamHits = Math.min(BEAM_COUNT, beamHits + 1);
            closestDistance = Math.min(closestDistance, Mth.clamp(distance, 0.0D, MAX_RANGE));
        }
    }
}
