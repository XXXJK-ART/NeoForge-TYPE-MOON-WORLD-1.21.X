package com.example.typemoonaddon.nega_summon;

import com.example.typemoonaddon.network.AddonSpellVisualPayload;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GawainEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;

/** Server-authoritative target validation and forced death for Nega Summon. */
public final class NegaSummonService {
    public static final double TARGET_RADIUS = 50.0D;
    public static final double TARGET_RADIUS_SQUARED = TARGET_RADIUS * TARGET_RADIUS;

    private static final String CAUSAL_SEVERED_TAG = "CausalSevered";
    private static final String GAWAIN_GUTS_READY_TAG = "GawainGutsReady";
    private static final String LOCK_EFFECT = "typemoonworld:nega_summon_lock";
    private static final String KILL_EFFECT = "typemoonworld:nega_summon_kill";
    private static final double VFX_OBSERVER_RADIUS = 128.0D;

    private NegaSummonService() {
    }

    public static CastOutcome cast(ServerPlayer caster) {
        if (!isValidCaster(caster)) {
            return CastOutcome.rejected();
        }

        Vec3 sphereCenter = caster.getBoundingBox().getCenter();
        List<LivingEntity> targets = findEligibleTargets(caster, sphereCenter);
        if (targets.isEmpty()) {
            return CastOutcome.noTarget();
        }

        ServerLevel level = caster.serverLevel();
        AddonSpellVisualPayload.showAttached(
                level, AddonSpellVisualPayload.NEGA_SUMMON,
                AddonSpellVisualPayload.SIGIL, caster.getUUID(), caster,
                1.0F, 1.0F, 26, 0, VFX_OBSERVER_RADIUS);
        DamageSource deathSource = caster.damageSources().source(DamageTypes.GENERIC_KILL, caster);
        int affectedTargets = 0;
        for (LivingEntity target : targets) {
            if (!isEligibleTarget(caster, target, sphereCenter)) {
                continue;
            }
            Vec3 effectPosition = target.getBoundingBox().getCenter();
            float targetRadius = (float) Math.max(
                    1.2D, Math.max(target.getBbWidth() * 0.9D, target.getBbHeight() * 0.42D));
            float targetHeight = (float) Math.max(2.0D, target.getBbHeight() * 1.2D);
            VFXServerEffects.spawn(level, LOCK_EFFECT, effectPosition, VFX_OBSERVER_RADIUS);
            AddonSpellVisualPayload.showAttached(
                    level, AddonSpellVisualPayload.NEGA_SUMMON,
                    AddonSpellVisualPayload.NEGA_LOCK, caster.getUUID(), target,
                    targetRadius, targetHeight, 18, target.getId(), VFX_OBSERVER_RADIUS);

            prepareServantForForcedDeath(target);
            forceDeath(target, deathSource);

            VFXServerEffects.spawn(level, KILL_EFFECT, effectPosition, VFX_OBSERVER_RADIUS);
            AddonSpellVisualPayload.showAt(
                    level, AddonSpellVisualPayload.NEGA_SUMMON,
                    AddonSpellVisualPayload.NEGA_COLLAPSE, caster.getUUID(), effectPosition,
                    targetRadius, targetHeight, 30, target.getId(), VFX_OBSERVER_RADIUS);
            level.playSound(null, target.blockPosition(), SoundEvents.END_PORTAL_SPAWN,
                    SoundSource.PLAYERS, 0.85F, 0.62F);
            level.playSound(null, target.blockPosition(), SoundEvents.WITHER_HURT,
                    SoundSource.PLAYERS, 0.55F, 0.72F);
            affectedTargets++;
        }
        AddonSpellVisualPayload.showAt(
                level, AddonSpellVisualPayload.NEGA_SUMMON,
                AddonSpellVisualPayload.AFTERMATH, caster.getUUID(), sphereCenter,
                (float) TARGET_RADIUS, 1.0F, 32, 0, VFX_OBSERVER_RADIUS);
        return affectedTargets > 0
                ? CastOutcome.success(affectedTargets)
                : CastOutcome.noTarget();
    }

    private static List<LivingEntity> findEligibleTargets(ServerPlayer caster, Vec3 sphereCenter) {
        AABB query = new AABB(sphereCenter, sphereCenter).inflate(TARGET_RADIUS);
        List<LivingEntity> targets = caster.serverLevel().getEntitiesOfClass(
                LivingEntity.class,
                query,
                target -> isEligibleTarget(caster, target, sphereCenter)
        );
        targets.sort(Comparator
                .comparingDouble((LivingEntity target) ->
                        target.getBoundingBox().getCenter().distanceToSqr(sphereCenter))
                .thenComparingInt(LivingEntity::getId));
        return targets;
    }

    private static boolean isEligibleTarget(
            ServerPlayer caster,
            LivingEntity target,
            Vec3 sphereCenter
    ) {
        if (target == caster
                || !target.isAlive()
                || target.isRemoved()
                || target.level() != caster.level()
                || target.getBoundingBox().getCenter().distanceToSqr(sphereCenter)
                        > TARGET_RADIUS_SQUARED
                || !EntityUtils.isValidCombatTarget(caster, target)) {
            return false;
        }
        if (target instanceof ServerPlayer player) {
            return TypeMoonWorldApi.servantForm(player).transformed();
        }
        ResourceLocation entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        return target instanceof ServantEntity
                && entityTypeId != null
                && TYPE_MOON_WORLD.MOD_ID.equals(entityTypeId.getNamespace());
    }

    private static void prepareServantForForcedDeath(LivingEntity target) {
        if (!(target instanceof ServantEntity)) {
            return;
        }
        CompoundTag data = target.getPersistentData();
        data.putBoolean(CAUSAL_SEVERED_TAG, true);
        if (target instanceof GawainEntity) {
            data.remove(GAWAIN_GUTS_READY_TAG);
        }
    }

    private static void forceDeath(LivingEntity target, DamageSource source) {
        target.setInvulnerable(false);
        target.invulnerableTime = 0;
        target.hurtTime = 0;
        target.hurtDuration = 0;
        target.setAbsorptionAmount(0.0F);
        target.setHealth(0.0F);
        target.die(source);
    }

    private static boolean isValidCaster(ServerPlayer caster) {
        return caster != null
                && caster.isAlive()
                && !caster.isRemoved()
                && !caster.isSpectator()
                && !caster.hasDisconnected();
    }

    public enum CastResult {
        SUCCESS,
        NO_TARGET,
        REJECTED
    }

    public record CastOutcome(CastResult result, int affectedTargets) {
        private static CastOutcome success(int affectedTargets) {
            return new CastOutcome(CastResult.SUCCESS, affectedTargets);
        }

        private static CastOutcome noTarget() {
            return new CastOutcome(CastResult.NO_TARGET, 0);
        }

        private static CastOutcome rejected() {
            return new CastOutcome(CastResult.REJECTED, 0);
        }
    }
}
