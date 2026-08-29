package io.github.typemoonaddon.shadowlogic.magic;

import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.compat.TypeMoonContractBridge;
import io.github.typemoonaddon.compat.TypeMoonServantBridge;
import io.github.typemoonaddon.config.GameplayConfig;
import io.github.typemoonaddon.data.ImaginarySpaceData.ShadowArtMode;
import io.github.typemoonaddon.shadowlogic.entity.ShadowArtRibbonEntity;
import io.github.typemoonaddon.entity.BlackShadowEntity;
import io.github.typemoonaddon.magic.PollutionService;
import io.github.typemoonaddon.magic.BlackMudService;
import io.github.typemoonaddon.magic.TypeMoonIntegration;
import io.github.typemoonaddon.registry.ModAttachments;
import io.github.typemoonaddon.registry.ModBlocks;
import io.github.typemoonaddon.registry.ModEntities;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Shared scans and authoritative lifecycle for all ten ribbon entities owned by a caster. */
public final class ShadowArtService {
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        TypeMoonAddon.id("shadow_art")
    );
    private static final DustParticleOptions SHADOW_RED_DUST = new DustParticleOptions(
        new Vector3f(0.48F, 0.015F, 0.025F),
        0.75F
    );
    private static final Map<HoldKey, Integer> TARGET_HOLD_TICKS = new HashMap<>();
    private static final Map<HoldKey, Vec3> TARGET_LIFT_ORIGINS = new HashMap<>();
    private static final Map<HoldKey, Vec3> TARGET_DESTINATIONS = new HashMap<>();
    private static final Map<HoldKey, Long> TARGET_LAST_ACTIVE_TICK = new HashMap<>();
    private static final Set<HoldKey> TARGET_BLACK_SHADOW_PINS = new HashSet<>();
    private static final Map<UUID, Long> CANCEL_CONFIRM_UNTIL = new HashMap<>();
    private static final Map<PierceAttempt, PierceDecision> PIERCE_DECISIONS = new HashMap<>();

    public static boolean cast(ServerPlayer player) {
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        if (!data.shadowArtUnlocked() || !data.grailErosionFull() || !player.isAlive() || player.isSpectator()) return false;
        if (data.shadowArtActive()) {
            long now = player.serverLevel().getGameTime();
            if (CANCEL_CONFIRM_UNTIL.getOrDefault(player.getUUID(), 0L) >= now) {
                CANCEL_CONFIRM_UNTIL.remove(player.getUUID());
                return deactivate(player, false);
            }
            CANCEL_CONFIRM_UNTIL.put(player.getUUID(), now + 100L);
            player.displayClientMessage(Component.translatable("message.typemoonaddon.shadow_art.confirm_cancel"), true);
            return true;
        }
        if (!data.activateShadowArt()) return false;
        for (int index = 0; index < GameplayConfig.SHADOW_ART_RIBBON_COUNT; index++) {
            if (data.shadowArtRespawnTicks(index) == 0) spawnRibbon(player, index, ShadowArtRibbonEntity.REGROW);
        }
        data.finishShadowArtActivation(); player.syncData(ModAttachments.IMAGINARY_SPACE.get());
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 1.0F, 0.55F);
        return true;
    }

    public static boolean confirmCancel(ServerPlayer player, boolean confirmed) {
        if (!confirmed) return player.getData(ModAttachments.IMAGINARY_SPACE.get()).shadowArtActive();
        return deactivate(player, false);
    }

    public static boolean deactivate(ServerPlayer player, boolean dispelled) {
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        boolean changed = dispelled ? data.dispelShadowArt() : data.deactivateShadowArt();
        if (!changed && !dispelled) return false;
        for (ShadowArtRibbonEntity ribbon : ribbons(player)) {
            ribbon.setAction(ShadowArtRibbonEntity.RETRACT, null);
        }
        if (dispelled) {
            ribbons(player).forEach(Entity::discard);
        }
        CANCEL_CONFIRM_UNTIL.remove(player.getUUID());
        clearOwnerState(player.getUUID());
        player.syncData(ModAttachments.IMAGINARY_SPACE.get());
        return true;
    }

    public static void selectMode(ServerPlayer player, ShadowArtMode mode) {
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        if (!data.shadowArtUnlocked()) return;
        data.setShadowArtMode(mode); player.syncData(ModAttachments.IMAGINARY_SPACE.get());
        player.displayClientMessage(Component.translatable("message.typemoonaddon.shadow_art.mode." + mode.serializedName()), true);
    }

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        PIERCE_DECISIONS.values().removeIf(decision -> decision.tick() + 2L < now);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) tickPlayer(player);
        Set<HoldKey> stale = new HashSet<>();
        TARGET_LAST_ACTIVE_TICK.forEach((key, lastActive) -> {
            if (lastActive + 1L < now) {
                stale.add(key);
            }
        });
        stale.forEach(ShadowArtService::clearTargetState);
    }

    private static void tickPlayer(ServerPlayer player) {
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        if (!data.shadowArtActive()) return;
        if (!player.isAlive() || player.isSpectator() || !data.shadowArtUnlocked()) { deactivate(player, false); return; }
        List<ShadowArtRibbonEntity> ribbons = ribbons(player);
        if (data.shadowArtState() == io.github.typemoonaddon.data.ImaginarySpaceData.ShadowArtState.DEACTIVATING) {
            if (ribbons.isEmpty()) {
                data.finishShadowArtDeactivation();
                player.syncData(ModAttachments.IMAGINARY_SPACE.get());
            }
            return;
        }
        boolean[] present = new boolean[GameplayConfig.SHADOW_ART_RIBBON_COUNT];
        for (ShadowArtRibbonEntity ribbon : ribbons) {
            int index = ribbon.ribbonIndex();
            if (present[index]) ribbon.discard();
            else present[index] = true;
        }
        ribbons.removeIf(Entity::isRemoved);
        boolean synced = false;
        for (int index = 0; index < present.length; index++) {
            if (present[index]) continue;
            if (data.tickShadowArtRibbonRecovery(index)) { spawnRibbon(player, index, ShadowArtRibbonEntity.REGROW); synced = true; }
            else if (data.shadowArtRespawnTicks(index) == 0) spawnRibbon(player, index, ShadowArtRibbonEntity.REGROW);
        }
        if (synced) player.syncData(ModAttachments.IMAGINARY_SPACE.get());
        if (player.tickCount % 20 == 0) {
            double cost = ribbons.size() * GameplayConfig.SHADOW_ART_MANA_PER_RIBBON_SECOND;
            if (cost > 0 && !TypeMoonIntegration.tryConsumeMana(player, cost)) { deactivate(player, false); return; }
        }
        if (player.tickCount % GameplayConfig.SHADOW_ART_PARTICLE_INTERVAL_TICKS == 0) {
            spawnShadowCircleParticles(player);
        }
        if (defenseAllowed(data.shadowArtMode()) && player.tickCount % 2 == 0) interceptThreat(player, ribbons);
        if (attackAllowed(data.shadowArtMode()) && player.tickCount % GameplayConfig.SHADOW_ART_TARGET_SCAN_TICKS == 0) assignAttack(player, ribbons);
        maintainPiercedTargets(player, ribbons);
    }

    public static void tickRibbon(ShadowArtRibbonEntity ribbon) {
        LivingEntity owner = owner(ribbon);
        if (owner == null || owner instanceof ServerPlayer player
            && !player.getData(ModAttachments.IMAGINARY_SPACE.get()).shadowArtActive()) {
            ribbon.discard(); return;
        }
        ribbon.refreshOwnerEntity(owner);
        int index = ribbon.ribbonIndex(); double angle = Math.PI * 2.0 * index / GameplayConfig.SHADOW_ART_RIBBON_COUNT;
        Vec3 root = owner.position().add(Math.cos(angle) * GameplayConfig.SHADOW_ART_ROOT_RADIUS, 0.12, Math.sin(angle) * GameplayConfig.SHADOW_ART_ROOT_RADIUS);
        if (ribbon.action() == ShadowArtRibbonEntity.RETRACT) {
            ribbon.setPos(ribbon.position().lerp(root, 0.42D));
            if (ribbon.tickCount - ribbon.actionStart() >= GameplayConfig.SHADOW_ART_RETRACT_TICKS) ribbon.discard();
            return;
        }
        Entity target = ribbon.targetEntityId() < 0 ? null : ribbon.level().getEntity(ribbon.targetEntityId());
        if (isTargetAction(ribbon.action()) && target instanceof LivingEntity living
            && legalRibbonTarget(owner, living, ribbon.action())) {
            Vec3 endpoint = piercePoint(living, index);
            Vec3 delta = endpoint.subtract(root); if (delta.lengthSqr() > square(GameplayConfig.SHADOW_ART_MAX_LENGTH)) { ribbon.setAction(ShadowArtRibbonEntity.IDLE, null); return; }
            double progress = ribbon.action() == ShadowArtRibbonEntity.ATTACK
                ? smoothStep(Math.clamp((ribbon.tickCount - ribbon.actionStart()) / (double) GameplayConfig.SHADOW_ART_STRIKE_TICKS, 0, 1))
                : 1.0D;
            ribbon.setPos(root.add(delta.scale(progress)));
            if (progress >= 1 && ribbon.action() == ShadowArtRibbonEntity.ATTACK) {
                long now = owner.level().getGameTime();
                PierceAttempt attempt = new PierceAttempt(owner.getUUID(), living.getUUID());
                PierceDecision decision = PIERCE_DECISIONS.get(attempt);
                boolean hitAccepted;
                if (decision != null && decision.tick() + 2L >= now) {
                    hitAccepted = decision.accepted();
                } else {
                    float healthBefore = living.getHealth();
                    hitAccepted = living.hurt(
                        shadowArtDamage(owner, ribbon),
                        GameplayConfig.SHADOW_ART_ATTACK_DAMAGE
                    );
                    if (owner instanceof BlackShadowEntity shadow
                        && !ShadowBindingService.hasBlackShadowSource(living)) {
                        shadow.rewardOwnerManaFromDamage(healthBefore - living.getHealth());
                    }
                    PIERCE_DECISIONS.put(attempt, new PierceDecision(now, hitAccepted));
                }
                if (hitAccepted && TypeMoonServantBridge.isServantOrCardUser(living)) ribbon.setAction(ShadowArtRibbonEntity.PIERCED, living);
                else ribbon.setAction(ShadowArtRibbonEntity.IDLE, null);
            }
            return;
        }
        double time = owner.tickCount;
        double seed = index * 1.731D;
        double radius = 2.6D + index % 4 * 0.58D;
        double x = Math.sin(time * (0.021D + index * 0.0007D) + seed) * radius
            + Math.sin(time * 0.009D + seed * 2.3D) * 0.75D;
        double z = Math.sin(time * (0.017D + index * 0.0005D) + seed * 1.41D) * radius
            + Math.cos(time * 0.011D + seed * 0.77D) * 0.65D;
        double y = 1.15D + Math.sin(time * 0.026D + seed * 1.8D) * 0.8D
            + Math.sin(time * 0.007D + seed) * 0.35D;
        Vec3 idle = root.add(x, y, z);
        ribbon.setPos(idle);
        if (ribbon.action() != ShadowArtRibbonEntity.REGROW || ribbon.tickCount - ribbon.actionStart() > 12) ribbon.setAction(ShadowArtRibbonEntity.IDLE, null);
    }

    public static void ribbonBroken(ShadowArtRibbonEntity ribbon) {
        LivingEntity owner = owner(ribbon); if (owner instanceof ServerPlayer player) {
            var data = player.getData(ModAttachments.IMAGINARY_SPACE.get()); data.breakShadowArtRibbon(ribbon.ribbonIndex()); player.syncData(ModAttachments.IMAGINARY_SPACE.get());
            player.serverLevel().sendParticles(ParticleTypes.SQUID_INK, ribbon.getX(), ribbon.getY(), ribbon.getZ(), 10, .2, .2, .2, .02);
        } else if (owner instanceof BlackShadowEntity shadow) {
            shadow.breakShadowArtRibbon(ribbon.ribbonIndex());
            ((ServerLevel)shadow.level()).sendParticles(ParticleTypes.SQUID_INK, ribbon.getX(), ribbon.getY(), ribbon.getZ(), 10, .2, .2, .2, .02);
        }
        ribbon.setAction(ShadowArtRibbonEntity.RETRACT, null); ribbon.discard();
    }

    public static boolean isPierced(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level)) return false;
        return level.getEntitiesOfClass(
            ShadowArtRibbonEntity.class,
            target.getBoundingBox().inflate(GameplayConfig.SHADOW_ART_ATTACK_RADIUS),
            ribbon -> isHoldingAction(ribbon.action()) && ribbon.targetEntityId() == target.getId()
        ).size() > 0;
    }

    public static void tickBlackShadow(
        BlackShadowEntity shadow,
        @Nullable LivingEntity target,
        boolean mudReady,
        boolean forceOpeningAttack
    ) {
        if (!(shadow.level() instanceof ServerLevel) || !shadow.isAlive()) {
            return;
        }
        List<ShadowArtRibbonEntity> ribbons = ribbons(shadow);
        boolean[] present = new boolean[GameplayConfig.SHADOW_ART_RIBBON_COUNT];
        for (ShadowArtRibbonEntity ribbon : ribbons) {
            int index = ribbon.ribbonIndex();
            if (present[index]) ribbon.discard();
            else present[index] = true;
        }
        ribbons.removeIf(Entity::isRemoved);
        for (int index = 0; index < present.length; index++) {
            if (present[index]) continue;
            if (shadow.shadowArtRecoveryTicks(index) == 0 || shadow.tickShadowArtRecovery(index)) {
                spawnRibbon(shadow, index, ShadowArtRibbonEntity.REGROW);
            }
        }
        if (shadow.tickCount % GameplayConfig.SHADOW_ART_PARTICLE_INTERVAL_TICKS == 0) {
            spawnShadowCircleParticles(shadow);
        }
        maintainPiercedTargets(shadow, ribbons);
        if (!legalTarget(shadow, target) || !mudReady && !forceOpeningAttack) {
            return;
        }
        if (forceOpeningAttack || shadow.tickCount % GameplayConfig.SHADOW_ART_TARGET_SCAN_TICKS == 0) {
            for (ShadowArtRibbonEntity ribbon : ribbons) {
                boolean openingReady = forceOpeningAttack
                    && (ribbon.action() == ShadowArtRibbonEntity.IDLE
                        || ribbon.action() == ShadowArtRibbonEntity.REGROW);
                boolean normalReady = ribbon.action() == ShadowArtRibbonEntity.IDLE
                    && ribbon.tickCount - ribbon.actionStart() >= GameplayConfig.SHADOW_ART_ATTACK_COOLDOWN_TICKS;
                if (openingReady || normalReady) {
                    ribbon.setAction(ShadowArtRibbonEntity.ATTACK, target);
                }
            }
        }
    }

    public static boolean hasRibbons(BlackShadowEntity shadow) {
        return shadow.level() instanceof ServerLevel && !ribbons(shadow).isEmpty();
    }

    public static boolean isOwnedHoldingTarget(BlackShadowEntity shadow, @Nullable LivingEntity target) {
        return target != null && ribbons(shadow).stream().anyMatch(
            ribbon -> isHoldingAction(ribbon.action()) && ribbon.targetEntityId() == target.getId()
        );
    }

    public static int ownedHoldingRibbonCount(BlackShadowEntity shadow, @Nullable LivingEntity target) {
        if (target == null) {
            return 0;
        }
        return (int)ribbons(shadow).stream()
            .filter(ribbon -> isHoldingAction(ribbon.action())
                && ribbon.targetEntityId() == target.getId())
            .count();
    }

    public static void maintainBlackShadowHeldTargets(BlackShadowEntity shadow) {
        if (shadow.level() instanceof ServerLevel && shadow.isAlive()) {
            maintainPiercedTargets(shadow, ribbons(shadow));
        }
    }

    public static boolean isOwnedPinnedTarget(BlackShadowEntity shadow, @Nullable LivingEntity target) {
        return target != null && ribbons(shadow).stream().anyMatch(
            ribbon -> ribbon.action() == ShadowArtRibbonEntity.PINNED
                && ribbon.targetEntityId() == target.getId()
        );
    }

    public static boolean shouldKeepOwnedTarget(BlackShadowEntity shadow, @Nullable LivingEntity target) {
        return target != null
            && target.isAlive()
            && !target.isRemoved()
            && shadow.canAttack(target)
            && shadow.distanceToSqr(target) <= square(GameplayConfig.SHADOW_ART_MAX_LENGTH)
            && isOwnedHoldingTarget(shadow, target);
    }

    public static boolean payBlackShadowRibbonUpkeep(BlackShadowEntity shadow) {
        if (!(shadow.level() instanceof ServerLevel) || shadow.tickCount % 20 != 0) {
            return true;
        }
        List<ShadowArtRibbonEntity> ownedRibbons = ribbons(shadow);
        long activeRibbonCount = ownedRibbons.stream()
            .filter(ribbon -> isTargetAction(ribbon.action()))
            .count();
        double cost = activeRibbonCount * GameplayConfig.SHADOW_ART_MANA_PER_RIBBON_SECOND;
        if (cost <= 0.0D || shadow.tryConsumeActionMana(cost)) {
            return true;
        }
        for (ShadowArtRibbonEntity ribbon : ownedRibbons) {
            if (isTargetAction(ribbon.action())) {
                ribbon.setAction(ShadowArtRibbonEntity.IDLE, null);
            }
        }
        clearOwnerState(shadow.getUUID());
        return false;
    }

    public static void releaseOwnedTarget(BlackShadowEntity shadow, @Nullable LivingEntity target) {
        if (target == null) {
            return;
        }
        List<ShadowArtRibbonEntity> ownedRibbons = ribbons(shadow);
        releaseTarget(ownedRibbons, target);
        clearTargetState(new HoldKey(shadow.getUUID(), target.getUUID()));
    }

    public static void blackShadowUnavailable(BlackShadowEntity shadow) {
        if (!(shadow.level() instanceof ServerLevel)) return;
        ribbons(shadow).forEach(Entity::discard);
        clearOwnerState(shadow.getUUID());
    }

    private static void assignAttack(ServerPlayer owner, List<ShadowArtRibbonEntity> ribbons) {
        int limit = owner.getData(ModAttachments.IMAGINARY_SPACE.get()).shadowArtMode() == ShadowArtMode.BALANCED ? 5 : 10;
        List<ShadowArtRibbonEntity> available = ribbons.stream()
            .filter(ribbon -> ribbon.ribbonIndex() < limit
                && ribbon.action() == ShadowArtRibbonEntity.IDLE
                && ribbon.tickCount - ribbon.actionStart() >= GameplayConfig.SHADOW_ART_ATTACK_COOLDOWN_TICKS)
            .sorted(Comparator.comparingInt(ShadowArtRibbonEntity::ribbonIndex))
            .toList();
        if (available.isEmpty()) {
            return;
        }
        List<LivingEntity> targets = findTargets(owner, Math.max(1, available.size() / 2));
        if (targets.isEmpty()) {
            return;
        }
        int pairedRibbonCount = Math.min(available.size(), targets.size() * 2);
        for (int index = 0; index < available.size(); index++) {
            int targetIndex = index < pairedRibbonCount
                ? index / 2
                : index % targets.size();
            available.get(index).setAction(ShadowArtRibbonEntity.ATTACK, targets.get(targetIndex));
        }
    }

    private static List<LivingEntity> findTargets(ServerPlayer owner, int limit) {
        LinkedHashSet<LivingEntity> ordered = new LinkedHashSet<>();
        LivingEntity retaliation = owner.getLastHurtByMob();
        if (legalTarget(owner, retaliation)
            && owner.distanceToSqr(retaliation) <= square(GameplayConfig.SHADOW_ART_ATTACK_RADIUS)) {
            ordered.add(retaliation);
        }
        List<LivingEntity> candidates = owner.serverLevel().getEntitiesOfClass(
            LivingEntity.class,
            owner.getBoundingBox().inflate(GameplayConfig.SHADOW_ART_ATTACK_RADIUS),
            target -> legalTarget(owner, target)
                && owner.distanceToSqr(target) <= square(GameplayConfig.SHADOW_ART_ATTACK_RADIUS)
        );
        candidates.stream()
            .filter(target -> target instanceof Mob mob && mob.getTarget() == owner)
            .sorted(Comparator.comparingDouble(owner::distanceToSqr))
            .forEach(ordered::add);
        LivingEntity attacked = owner.getLastHurtMob();
        if (legalTarget(owner, attacked)
            && owner.distanceToSqr(attacked) <= square(GameplayConfig.SHADOW_ART_ATTACK_RADIUS)) {
            ordered.add(attacked);
        }
        candidates.stream()
            .filter(TypeMoonServantBridge::isServantOrCardUser)
            .sorted(Comparator.comparingDouble(owner::distanceToSqr))
            .forEach(ordered::add);
        return ordered.stream().limit(limit).toList();
    }

    private static boolean legalTarget(ServerPlayer owner, @Nullable LivingEntity target) {
        if (target == null || target == owner || !target.isAlive() || target.isRemoved() || target.level() != owner.level() || target.isAlliedTo(owner) || owner.isAlliedTo(target) || PollutionService.isShadowFactionMember(target)) return false;
        if (target instanceof ServerPlayer player && (player.isCreative() || player.isSpectator())) return false;
        return !TypeMoonContractBridge.isContractedTo(owner, target);
    }

    private static boolean legalTarget(LivingEntity owner, @Nullable LivingEntity target) {
        return legalRibbonTarget(owner, target, ShadowArtRibbonEntity.ATTACK);
    }

    private static boolean legalRibbonTarget(
        LivingEntity owner,
        @Nullable LivingEntity target,
        byte action
    ) {
        double maximumLength = action == ShadowArtRibbonEntity.ATTACK
            ? GameplayConfig.SHADOW_ART_ATTACK_RADIUS
            : GameplayConfig.SHADOW_ART_MAX_LENGTH;
        if (owner instanceof ServerPlayer player) {
            return legalTarget(player, target)
                && player.distanceToSqr(target) <= square(maximumLength);
        }
        return owner instanceof BlackShadowEntity shadow
            && target != null
            && shadow.canAttack(target)
            && shadow.distanceToSqr(target) <= square(maximumLength);
    }

    private static void interceptThreat(ServerPlayer owner, List<ShadowArtRibbonEntity> ribbons) {
        int firstDefense = owner.getData(ModAttachments.IMAGINARY_SPACE.get()).shadowArtMode() == ShadowArtMode.BALANCED ? 5 : 0;
        ShadowArtRibbonEntity defender = ribbons.stream().filter(r -> r.ribbonIndex() >= firstDefense && !isHoldingAction(r.action())).findFirst().orElse(null); if (defender == null) return;
        for (Projectile projectile : owner.serverLevel().getEntitiesOfClass(Projectile.class, owner.getBoundingBox().inflate(GameplayConfig.SHADOW_ART_PROJECTILE_SCAN_RADIUS), p -> p.isAlive() && p.getOwner() != owner)) {
            Vec3 velocity = projectile.getDeltaMovement(); if (velocity.lengthSqr() < 0.0025) continue;
            Vec3 toOwner = owner.getEyePosition().subtract(projectile.position()); double along = toOwner.dot(velocity) / velocity.lengthSqr();
            if (along < 0 || along > GameplayConfig.SHADOW_ART_PROJECTILE_SCAN_RADIUS
                || projectile.position().add(velocity.scale(along)).distanceToSqr(owner.getEyePosition()) > GameplayConfig.SHADOW_ART_PROJECTILE_HIT_RADIUS_SQR) continue;
            defender.setAction(ShadowArtRibbonEntity.DEFEND, projectile); defender.setPos(projectile.position());
            defender.hurt(owner.damageSources().thrown(projectile, projectile.getOwner()), GameplayConfig.SHADOW_ART_PROJECTILE_BLOCK_DAMAGE); projectile.discard(); return;
        }
    }

    private static void maintainPiercedTargets(LivingEntity owner, List<ShadowArtRibbonEntity> ribbons) {
        Map<LivingEntity, Integer> counts = new HashMap<>();
        for (ShadowArtRibbonEntity ribbon : ribbons) {
            if (isHoldingAction(ribbon.action()) && ribbon.targetEntityId() >= 0
                && ribbon.level().getEntity(ribbon.targetEntityId()) instanceof LivingEntity target
                && legalRibbonTarget(owner, target, ribbon.action())) {
                counts.merge(target, 1, Integer::sum);
            }
        }
        for (Map.Entry<LivingEntity, Integer> entry : counts.entrySet()) {
            LivingEntity target = entry.getKey();
            if (entry.getValue() < GameplayConfig.SHADOW_ART_HOLD_RIBBON_COUNT) continue;
            HoldKey holdKey = new HoldKey(owner.getUUID(), target.getUUID());
            TARGET_LAST_ACTIVE_TICK.put(holdKey, owner.level().getGameTime());
            int ticks = TARGET_HOLD_TICKS.merge(holdKey, 1, Integer::sum);
            Vec3 liftOrigin = TARGET_LIFT_ORIGINS.computeIfAbsent(holdKey, ignored -> target.position());
            boolean targetInMud = BlackMudService.isOnOrInBlackMud(target);

            target.setDeltaMovement(Vec3.ZERO);
            target.fallDistance = 0;
            boolean permanentBlackShadowPin = TARGET_BLACK_SHADOW_PINS.contains(holdKey);
            if (owner instanceof BlackShadowEntity && targetInMud) {
                if (PollutionService.isUnpollutableServantOrCardUser(target)) {
                    TARGET_BLACK_SHADOW_PINS.add(holdKey);
                    permanentBlackShadowPin = true;
                } else {
                    setTargetAction(ribbons, target, ShadowArtRibbonEntity.PIERCED);
                    moveHeldTarget(target, liftOrigin);
                    target.hurtMarked = true;
                    continue;
                }
            }

            if (permanentBlackShadowPin) {
                Vec3 pinned = TARGET_DESTINATIONS.computeIfAbsent(
                    holdKey,
                    ignored -> liftOrigin.add(0, GameplayConfig.SHADOW_ART_LIFT_HEIGHT, 0)
                );
                if (ticks <= GameplayConfig.SHADOW_ART_LIFT_TICKS) {
                    setTargetAction(ribbons, target, ShadowArtRibbonEntity.LIFT);
                    double progress = smoothStep(ticks / (double) GameplayConfig.SHADOW_ART_LIFT_TICKS);
                    moveHeldTarget(target, liftOrigin.lerp(pinned, progress));
                } else {
                    setTargetAction(ribbons, target, ShadowArtRibbonEntity.PINNED);
                    moveHeldTarget(target, pinned);
                }
                target.hurtMarked = true;
                continue;
            }

            Vec3 destination = TARGET_DESTINATIONS.get(holdKey);
            if (destination == null) {
                if (targetInMud) {
                    destination = liftOrigin.add(0, GameplayConfig.SHADOW_ART_LIFT_HEIGHT, 0);
                } else {
                    BlockPos mud;
                    if (owner instanceof ServerPlayer) {
                        mud = nearestLoadedMud(
                            (ServerLevel)owner.level(),
                            owner.blockPosition(),
                            GameplayConfig.SHADOW_ART_PREFERRED_MUD_MIN_RADIUS,
                            GameplayConfig.SHADOW_ART_PREFERRED_MUD_MAX_RADIUS
                        );
                        if (mud == null) {
                            mud = nearestLoadedMud(
                                (ServerLevel)owner.level(),
                                owner.blockPosition(),
                                0,
                                (int)GameplayConfig.SHADOW_ART_ATTACK_RADIUS
                            );
                        }
                    } else {
                        mud = nearestLoadedMud(
                            (ServerLevel)owner.level(),
                            owner.blockPosition(),
                            0,
                            GameplayConfig.BLACK_SHADOW_MUD_RADIUS
                        );
                    }
                    destination = throwDestination(owner, target, mud);
                }
                TARGET_DESTINATIONS.put(holdKey, destination);
            }

            if (ticks <= GameplayConfig.SHADOW_ART_LIFT_TICKS) {
                setTargetAction(ribbons, target, ShadowArtRibbonEntity.LIFT);
                double progress = smoothStep(ticks / (double) GameplayConfig.SHADOW_ART_LIFT_TICKS);
                Vec3 lifted = liftOrigin.add(0, GameplayConfig.SHADOW_ART_LIFT_HEIGHT, 0);
                moveHeldTarget(target, liftOrigin.lerp(lifted, progress));
            } else if (targetInMud) {
                setTargetAction(ribbons, target, ShadowArtRibbonEntity.PINNED);
                moveHeldTarget(target, destination);
            } else {
                setTargetAction(ribbons, target, ShadowArtRibbonEntity.THROW);
                int throwTick = ticks - GameplayConfig.SHADOW_ART_LIFT_TICKS;
                double progress = smoothStep(Math.clamp(throwTick / (double) GameplayConfig.SHADOW_ART_THROW_TICKS, 0, 1));
                Vec3 start = liftOrigin.add(0, GameplayConfig.SHADOW_ART_LIFT_HEIGHT, 0);
                Vec3 control = start.add(destination).scale(0.5D).add(0, Math.max(2.0D, start.distanceTo(destination) * 0.22D), 0);
                moveHeldTarget(target, quadraticBezier(start, control, destination, progress));
                if (throwTick >= GameplayConfig.SHADOW_ART_THROW_TICKS) {
                    releaseTarget(ribbons, target);
                    clearTargetState(holdKey);
                }
            }
            target.hurtMarked = true;
        }
    }

    private static void releaseTarget(List<ShadowArtRibbonEntity> ribbons, LivingEntity target) { for (ShadowArtRibbonEntity ribbon : ribbons) if (ribbon.targetEntityId() == target.getId()) ribbon.setAction(ShadowArtRibbonEntity.IDLE, null); }
    private static void spawnShadowCircleParticles(LivingEntity owner) {
        for (int index = 0; index < GameplayConfig.SHADOW_ART_BLACK_PARTICLES; index++) {
            Vec3 position = randomShadowCirclePosition(owner);
            ((ServerLevel)owner.level()).sendParticles(
                ParticleTypes.SQUID_INK,
                position.x, position.y, position.z,
                1, 0.02D, 0.025D, 0.02D, 0.008D
            );
        }
        for (int index = 0; index < GameplayConfig.SHADOW_ART_RED_PARTICLES; index++) {
            Vec3 position = randomShadowCirclePosition(owner);
            ((ServerLevel)owner.level()).sendParticles(
                SHADOW_RED_DUST,
                position.x, position.y, position.z,
                1, 0.015D, 0.02D, 0.015D, 0.0D
            );
        }
    }
    private static Vec3 randomShadowCirclePosition(LivingEntity owner) {
        double radius = Math.sqrt(owner.getRandom().nextDouble()) * GameplayConfig.SHADOW_ART_IDLE_RADIUS;
        double angle = owner.getRandom().nextDouble() * Math.PI * 2.0D;
        return owner.position().add(Math.cos(angle) * radius, 0.08D, Math.sin(angle) * radius);
    }
    private static void setTargetAction(List<ShadowArtRibbonEntity> ribbons, LivingEntity target, byte action) {
        for (ShadowArtRibbonEntity ribbon : ribbons) {
            if (ribbon.targetEntityId() == target.getId() && ribbon.action() != action) ribbon.setAction(action, target);
        }
    }
    private static void moveHeldTarget(LivingEntity target, Vec3 position) {
        target.teleportTo(position.x, position.y, position.z);
        target.setDeltaMovement(Vec3.ZERO);
        target.fallDistance = 0;
    }
    private static Vec3 piercePoint(LivingEntity target, int ribbonIndex) {
        double width = Math.max(0.25D, target.getBbWidth() * 0.42D);
        double height = target.getBbHeight();
        double angle = Math.PI * 2.0D * ribbonIndex / GameplayConfig.SHADOW_ART_RIBBON_COUNT + (ribbonIndex % 2) * 0.31D;
        double yFraction = switch (ribbonIndex % 5) {
            case 0 -> 0.78D;
            case 1 -> 0.64D;
            case 2 -> 0.52D;
            case 3 -> 0.38D;
            default -> 0.24D;
        };
        return target.position().add(Math.cos(angle) * width, height * yFraction, Math.sin(angle) * width);
    }
    private static Vec3 throwDestination(LivingEntity owner, LivingEntity target, @Nullable BlockPos mud) {
        Vec3 requested = mud == null ? safeThrowPoint(owner, target) : Vec3.atCenterOf(mud).add(0, 0.2D, 0);
        return clampToRibbonSphere(owner, requested);
    }
    private static Vec3 clampToRibbonSphere(LivingEntity owner, Vec3 requested) {
        Vec3 offset = requested.subtract(owner.position());
        double maximum = GameplayConfig.SHADOW_ART_MAX_LENGTH - 0.5D;
        return offset.lengthSqr() <= maximum * maximum ? requested : owner.position().add(offset.normalize().scale(maximum));
    }
    private static Vec3 quadraticBezier(Vec3 start, Vec3 control, Vec3 end, double progress) {
        double inverse = 1.0D - progress;
        return start.scale(inverse * inverse).add(control.scale(2.0D * inverse * progress)).add(end.scale(progress * progress));
    }
    private static double smoothStep(double value) {
        double clamped = Math.clamp(value, 0.0D, 1.0D);
        return clamped * clamped * (3.0D - 2.0D * clamped);
    }
    private static boolean isTargetAction(byte action) {
        return action == ShadowArtRibbonEntity.ATTACK || isHoldingAction(action);
    }
    public static boolean isHoldingAction(byte action) {
        return action == ShadowArtRibbonEntity.PIERCED || action == ShadowArtRibbonEntity.LIFT
            || action == ShadowArtRibbonEntity.THROW || action == ShadowArtRibbonEntity.PINNED;
    }
    private static void clearTargetState(HoldKey holdKey) {
        TARGET_HOLD_TICKS.remove(holdKey);
        TARGET_LIFT_ORIGINS.remove(holdKey);
        TARGET_DESTINATIONS.remove(holdKey);
        TARGET_LAST_ACTIVE_TICK.remove(holdKey);
        TARGET_BLACK_SHADOW_PINS.remove(holdKey);
    }
    private static void clearOwnerState(UUID ownerId) {
        Set<HoldKey> owned = new HashSet<>();
        TARGET_LAST_ACTIVE_TICK.keySet().stream()
            .filter(key -> key.ownerId().equals(ownerId))
            .forEach(owned::add);
        owned.forEach(ShadowArtService::clearTargetState);
    }
    @Nullable private static BlockPos nearestLoadedMud(ServerLevel level, BlockPos center, int minimumRadius, int maximumRadius) {
        BlockPos closest = null; double best = Double.MAX_VALUE;
        int minimumSqr = minimumRadius * minimumRadius;
        int maximumSqr = maximumRadius * maximumRadius;
        for (int x = -maximumRadius; x <= maximumRadius; x += 2) for (int z = -maximumRadius; z <= maximumRadius; z += 2) for (int y = -4; y <= 4; y++) {
            int distanceSqr = x * x + y * y + z * z;
            BlockPos pos = center.offset(x, y, z); if (distanceSqr < minimumSqr || distanceSqr > maximumSqr || !level.hasChunkAt(pos) || !level.getBlockState(pos).is(ModBlocks.BLACK_MUD.get())) continue;
            double distance = pos.distSqr(center); if (distance < best) { best = distance; closest = pos.immutable(); }
        }
        return closest;
    }
    private static Vec3 safeThrowPoint(LivingEntity owner, LivingEntity target) { Vec3 direction = target.position().subtract(owner.position()); if (direction.lengthSqr() < .01) direction = owner.getLookAngle(); return owner.position().add(direction.normalize().scale(Math.min(12, GameplayConfig.SHADOW_ART_ATTACK_RADIUS - 1))); }
    private static void spawnRibbon(LivingEntity owner, int index, byte action) { if (!(owner.level() instanceof ServerLevel level)) return; ShadowArtRibbonEntity ribbon = ModEntities.SHADOW_ART_RIBBON.get().create(level); if (ribbon == null) return; ribbon.initialize(owner, index); ribbon.setPos(owner.position()); ribbon.setAction(action, null); level.addFreshEntity(ribbon); }
    private static List<ShadowArtRibbonEntity> ribbons(LivingEntity owner) { return ((ServerLevel)owner.level()).getEntitiesOfClass(ShadowArtRibbonEntity.class, new AABB(owner.blockPosition()).inflate(GameplayConfig.SHADOW_ART_MAX_LENGTH + 8), ribbon -> owner.getUUID().equals(ribbon.ownerId())); }
    @Nullable private static LivingEntity owner(ShadowArtRibbonEntity ribbon) { UUID id = ribbon.ownerId(); if (id == null || !(ribbon.level() instanceof ServerLevel level)) return null; Entity entity = level.getEntity(id); return entity instanceof LivingEntity living && living.isAlive() ? living : null; }
    private static boolean attackAllowed(ShadowArtMode mode) { return mode != ShadowArtMode.AUTO_DEFENSE; }
    private static boolean defenseAllowed(ShadowArtMode mode) { return mode != ShadowArtMode.AUTO_ATTACK; }
    private static double square(double value) { return value * value; }
    public static void playerUnavailable(ServerPlayer player) {
        CANCEL_CONFIRM_UNTIL.remove(player.getUUID());
        if (player.getData(ModAttachments.IMAGINARY_SPACE.get()).shadowArtActive()) {
            deactivate(player, false);
            ribbons(player).forEach(Entity::discard);
            player.getData(ModAttachments.IMAGINARY_SPACE.get()).finishShadowArtDeactivation();
            player.syncData(ModAttachments.IMAGINARY_SPACE.get());
        }
    }
    public static void serverStopping() {
        TARGET_HOLD_TICKS.clear();
        TARGET_LIFT_ORIGINS.clear();
        TARGET_DESTINATIONS.clear();
        TARGET_LAST_ACTIVE_TICK.clear();
        TARGET_BLACK_SHADOW_PINS.clear();
        PIERCE_DECISIONS.clear();
        CANCEL_CONFIRM_UNTIL.clear();
    }
    public static boolean isUnavoidableInBlackMud(LivingEntity target, DamageSource source) {
        return source != null && source.is(DAMAGE_TYPE) && BlackMudService.isOnOrInBlackMud(target);
    }
    private static DamageSource shadowArtDamage(LivingEntity owner, ShadowArtRibbonEntity ribbon) {
        ServerLevel level = (ServerLevel)owner.level();
        return new DamageSource(
            level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE_TYPE),
            ribbon,
            owner
        );
    }
    private record HoldKey(UUID ownerId, UUID targetId) {}
    private record PierceAttempt(UUID ownerId, UUID targetId) {}
    private record PierceDecision(long tick, boolean accepted) {}
    private ShadowArtService() {}
}
