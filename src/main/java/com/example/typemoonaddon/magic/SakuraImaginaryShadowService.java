package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.config.GameplayConfig;
import com.example.typemoonaddon.network.DeathFadePayload;
import com.example.typemoonaddon.network.VoidAbsorptionLinkPayload;
import com.example.typemoonaddon.registry.AddonMobEffects;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Living-target channel used by evolved Imaginary Absorption. */
public final class SakuraImaginaryShadowService {
    private static final int ABSORPTION_DEBUFF_DURATION_TICKS = 60;
    private static final int WEAKNESS_III_AMPLIFIER = 2;
    private static final Map<UUID, ShadowBinding> BINDINGS = new HashMap<>();
    private static final Map<UUID, DirectTargetLock> TARGETS = new HashMap<>();
    private static final Map<UUID, Long> NEXT_DAMAGE_TICK = new HashMap<>();
    private static final Map<UUID, PendingDissolution> DISSOLUTIONS = new HashMap<>();

    public static boolean castFromAbsorption(ServerPlayer caster) {
        ShadowBinding active = BINDINGS.get(caster.getUUID());
        if (active != null) {
            endBinding(caster.server, active, "message.typemoonworld.shadow.ended");
            return true;
        }
        if (!validCaster(caster)) {
            notify(caster, "message.typemoonworld.shadow.cannot_cast");
            return false;
        }

        LivingEntity target = rayTraceLiving(caster);
        if (!validTarget(caster, target)) {
            notify(caster, "message.typemoonworld.shadow.no_target");
            return false;
        }
        if (DISSOLUTIONS.containsKey(target.getUUID())) {
            notify(caster, "message.typemoonworld.shadow.no_target");
            return false;
        }

        boolean targetWasNoAi = target instanceof Mob mob && mob.isNoAi();
        ShadowBinding binding = new ShadowBinding(
                caster.getUUID(),
                target.getUUID(),
                caster.level().dimension(),
                target.position(),
                targetWasNoAi
        );
        BINDINGS.put(caster.getUUID(), binding);
        DirectTargetLock targetLock = TARGETS.computeIfAbsent(
                target.getUUID(),
                ignored -> new DirectTargetLock(targetWasNoAi)
        );
        boolean firstAbsorber = targetLock.casterIds.isEmpty();
        targetLock.casterIds.add(caster.getUUID());
        NEXT_DAMAGE_TICK.put(caster.getUUID(), caster.serverLevel().getGameTime() + GameplayConfig.SHADOW_DAMAGE_INTERVAL_TICKS);
        if (!(target instanceof EnderDragon)) {
            applyAbsorptionDebuffs(target, caster);
            SakuraPollutionService.expose(target, caster, true);
        }
        if (target instanceof ServerPlayer targetPlayer && firstAbsorber) {
            notify(targetPlayer, "message.typemoonworld.shadow.target_started");
        }
        caster.serverLevel().playSound(
                null,
                caster.blockPosition(),
                SoundEvents.SCULK_SHRIEKER_SHRIEK,
                SoundSource.PLAYERS,
                0.45F,
                1.35F
        );
        notify(caster, "message.typemoonworld.shadow.started");
        return true;
    }

    public static boolean hasLivingTarget(ServerPlayer caster) {
        LivingEntity target = rayTraceLiving(caster);
        return validTarget(caster, target);
    }

    public static void tick(MinecraftServer server) {
        tickBindings(server);
        tickDissolutions(server);
    }

    public static void playerUnavailable(ServerPlayer player) {
        ShadowBinding binding = BINDINGS.get(player.getUUID());
        if (binding != null) {
            endBinding(player.server, binding, null);
        }
    }

    public static void entityLeavingLevel(Entity entity) {
        ShadowBinding casterBinding = BINDINGS.get(entity.getUUID());
        if (casterBinding != null && entity instanceof ServerPlayer player) {
            endBinding(player.server, casterBinding, null);
        }
        DirectTargetLock targetLock = TARGETS.get(entity.getUUID());
        if (targetLock != null && entity.getServer() != null) {
            for (UUID ownerId : Set.copyOf(targetLock.casterIds)) {
                ShadowBinding targetBinding = BINDINGS.get(ownerId);
                if (targetBinding != null) {
                    endBinding(entity.getServer(), targetBinding, null);
                }
            }
        }
        TARGETS.remove(entity.getUUID());
        DISSOLUTIONS.remove(entity.getUUID());
    }

    public static void stopForUpgrade(ServerPlayer player) {
        playerUnavailable(player);
    }

    public static void serverStopping(MinecraftServer server) {
        Set<ShadowBinding> directBindings = Set.copyOf(BINDINGS.values());
        BINDINGS.clear();
        TARGETS.clear();
        NEXT_DAMAGE_TICK.clear();
        for (ShadowBinding binding : directBindings) {
            forceRestoreTarget(server, binding.dimension(), binding.targetId(), binding.targetWasNoAi());
        }
        for (PendingDissolution dissolution : DISSOLUTIONS.values()) {
            LivingEntity target = living(server, dissolution.dimension(), dissolution.targetId());
            if (target != null) {
                target.setInvulnerable(dissolution.wasInvulnerable());
                restoreMobAi(target, dissolution.wasNoAi());
            }
        }
        DISSOLUTIONS.clear();
    }

    private static void tickBindings(MinecraftServer server) {
        Iterator<ShadowBinding> iterator = BINDINGS.values().iterator();
        while (iterator.hasNext()) {
            ShadowBinding binding = iterator.next();
            ServerPlayer caster = server.getPlayerList().getPlayer(binding.casterId());
            LivingEntity target = living(server, binding.dimension(), binding.targetId());
            if (!validCaster(caster)
                    || !validTarget(caster, target)
                    || !isAimingAt(caster, target)
                    || DISSOLUTIONS.containsKey(binding.targetId())) {
                iterator.remove();
                boolean targetReleased = releaseDirectTarget(binding);
                NEXT_DAMAGE_TICK.remove(binding.casterId());
                restoreTarget(server, binding);
                if (targetReleased && target instanceof ServerPlayer targetPlayer) {
                    notify(targetPlayer, "message.typemoonworld.shadow.target_ended");
                }
                if (caster != null) {
                    notify(caster, "message.typemoonworld.shadow.ended");
                }
                continue;
            }

            if (!(target instanceof EnderDragon)) {
                applyAbsorptionDebuffs(target, caster);
                SakuraPollutionService.expose(target, caster, true);
            }
            syncVoidAbsorptionLink(caster, target);
            long now = caster.serverLevel().getGameTime();
            if (target instanceof ServerPlayer targetPlayer && now % 20L == 0L) {
                if (SakuraPollutionService.isPolluted(targetPlayer)) {
                    notify(
                            targetPlayer,
                            "message.typemoonworld.shadow.target_active_pollution",
                            Math.round(SakuraPollutionService.progress(targetPlayer) * 100.0F)
                    );
                } else {
                    notify(targetPlayer, "message.typemoonworld.shadow.target_active");
                }
            }
            if (now >= NEXT_DAMAGE_TICK.getOrDefault(binding.casterId(), now + GameplayConfig.SHADOW_DAMAGE_INTERVAL_TICKS)) {
                NEXT_DAMAGE_TICK.put(binding.casterId(), now + GameplayConfig.SHADOW_DAMAGE_INTERVAL_TICKS);
                pulseAbsorption(caster, target, binding, iterator);
            }
        }
    }

    private static void tickDissolutions(MinecraftServer server) {
        Iterator<PendingDissolution> iterator = DISSOLUTIONS.values().iterator();
        while (iterator.hasNext()) {
            PendingDissolution dissolution = iterator.next();
            LivingEntity target = living(server, dissolution.dimension(), dissolution.targetId());
            if (target == null) {
                iterator.remove();
                continue;
            }

            target.setDeltaMovement(Vec3.ZERO);
            if (target instanceof Mob mob) {
                mob.getNavigation().stop();
            }
            if (server.overworld().getGameTime() < dissolution.finishTick()) {
                continue;
            }

            iterator.remove();
            target.setInvulnerable(dissolution.wasInvulnerable());
            restoreMobAi(target, dissolution.wasNoAi());
            if (dissolution.killAtEnd()) {
                target.kill();
            } else {
                target.discard();
            }
        }
    }

    private static void pulseAbsorption(
            ServerPlayer caster,
            LivingEntity target,
            ShadowBinding binding,
            Iterator<ShadowBinding> iterator
    ) {
        float damage = SakuraTypeMoonIntegration.imaginaryDamage(caster, target, GameplayConfig.SHADOW_DAMAGE_PER_SECOND);
        if (!(target instanceof EnderDragon)) {
            SakuraTypeMoonIntegration.transferMana(target, caster, GameplayConfig.ABSORPTION_MANA_DRAIN_PER_SECOND);
        }
        if (target instanceof EnderDragon dragon) {
            dragon.hurt(dragon.head, caster.damageSources().indirectMagic(caster, caster), damage);
            if (dragon.getPhaseManager().getCurrentPhase().getPhase() == EnderDragonPhase.DYING) {
                iterator.remove();
                releaseDirectTarget(binding);
                NEXT_DAMAGE_TICK.remove(binding.casterId());
                restoreTarget(caster.server, binding);
                notify(caster, "message.typemoonworld.shadow.ended");
            }
            return;
        }
        if (SakuraShadowBindingService.absorbDirectDamage(target, caster, damage)) {
            return;
        }
        if (target.getHealth() <= damage) {
            iterator.remove();
            releaseDirectTarget(binding);
            NEXT_DAMAGE_TICK.remove(binding.casterId());
            startDissolution(caster, target, binding);
        } else {
            target.setHealth(target.getHealth() - damage);
        }
    }

    private static void startDissolution(ServerPlayer caster, LivingEntity target, ShadowBinding binding) {
        target.removeEffect(AddonMobEffects.BANISHMENT);
        target.setHealth(1.0F);
        boolean wasInvulnerable = target.isInvulnerable();
        target.setInvulnerable(true);
        lock(target, binding);
        long finishTick = caster.serverLevel().getGameTime() + GameplayConfig.SHADOW_DISSOLUTION_TICKS;
        DISSOLUTIONS.put(target.getUUID(), new PendingDissolution(
                target.getUUID(),
                binding.dimension(),
                finishTick,
                wasInvulnerable,
                binding.targetWasNoAi(),
                target instanceof ServerPlayer
        ));
        playDissolutionVisual(caster.serverLevel(), target, caster);
    }

    public static boolean dissolveAndKill(Entity source, LivingEntity target) {
        if (source == null
                || target == null
                || !target.isAlive()
                || !(target.level() instanceof ServerLevel level)
                || DISSOLUTIONS.containsKey(target.getUUID())) {
            return false;
        }
        boolean wasNoAi = target instanceof Mob mob && mob.isNoAi();
        boolean wasInvulnerable = target.isInvulnerable();
        target.removeEffect(AddonMobEffects.BANISHMENT);
        target.setHealth(1.0F);
        target.setInvulnerable(true);
        target.setDeltaMovement(Vec3.ZERO);
        if (target instanceof Mob mob) {
            mob.setNoAi(true);
            mob.getNavigation().stop();
        }
        DISSOLUTIONS.put(target.getUUID(), new PendingDissolution(
                target.getUUID(),
                level.dimension(),
                level.getGameTime() + GameplayConfig.SHADOW_DISSOLUTION_TICKS,
                wasInvulnerable,
                wasNoAi,
                true
        ));
        playDissolutionVisual(level, target, source);
        return true;
    }

    public static void playDissolutionVisual(ServerLevel level, LivingEntity target, Entity source) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                target,
                new DeathFadePayload(
                        target.getId(),
                        target.getX(),
                        target.getY(),
                        target.getZ(),
                        target.getBbWidth(),
                        target.getBbHeight(),
                        SakuraParticleService.palette(source)
                )
        );
        level.playSound(null, target.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.8F, 0.55F);
    }

    private static void endBinding(MinecraftServer server, ShadowBinding binding, String messageKey) {
        BINDINGS.remove(binding.casterId());
        LivingEntity target = living(server, binding.dimension(), binding.targetId());
        boolean targetReleased = releaseDirectTarget(binding);
        NEXT_DAMAGE_TICK.remove(binding.casterId());
        restoreTarget(server, binding);
        if (targetReleased && target instanceof ServerPlayer targetPlayer) {
            notify(targetPlayer, "message.typemoonworld.shadow.target_ended");
        }
        ServerPlayer caster = server.getPlayerList().getPlayer(binding.casterId());
        if (caster != null && messageKey != null) {
            notify(caster, messageKey);
        }
    }

    private static void restoreTarget(MinecraftServer server, ShadowBinding binding) {
        LivingEntity target = living(server, binding.dimension(), binding.targetId());
        if (target != null) {
            restoreTargetIfUnlocked(target, binding.targetWasNoAi());
        }
    }

    private static boolean releaseDirectTarget(ShadowBinding binding) {
        DirectTargetLock lock = TARGETS.get(binding.targetId());
        if (lock == null || !lock.casterIds.remove(binding.casterId()) || !lock.casterIds.isEmpty()) {
            return false;
        }
        TARGETS.remove(binding.targetId());
        return true;
    }

    private static void restoreTargetIfUnlocked(LivingEntity target, boolean targetWasNoAi) {
        UUID targetId = target.getUUID();
        if (TARGETS.containsKey(targetId) || DISSOLUTIONS.containsKey(targetId)) {
            return;
        }
        target.removeEffect(AddonMobEffects.BANISHMENT);
        restoreMobAi(target, targetWasNoAi);
    }

    private static void forceRestoreTarget(
            MinecraftServer server,
            ResourceKey<Level> dimension,
            UUID targetId,
            boolean targetWasNoAi
    ) {
        LivingEntity target = living(server, dimension, targetId);
        if (target != null) {
            target.removeEffect(AddonMobEffects.BANISHMENT);
            restoreMobAi(target, targetWasNoAi);
        }
    }

    private static void lock(LivingEntity target, ShadowBinding binding) {
        target.setDeltaMovement(Vec3.ZERO);
        target.fallDistance = 0.0F;
        if (target instanceof Mob mob) {
            mob.setNoAi(true);
            mob.getNavigation().stop();
        }
        if (target.position().distanceToSqr(binding.targetAnchor()) > 0.0025D) {
            target.teleportTo(binding.targetAnchor().x, binding.targetAnchor().y, binding.targetAnchor().z);
        }
    }

    private static void restoreMobAi(LivingEntity target, boolean wasNoAi) {
        if (target instanceof Mob mob) {
            mob.setNoAi(SakuraShadowBindingService.isBound(target) || wasNoAi);
        }
    }

    private static void applyAbsorptionDebuffs(LivingEntity target, LivingEntity source) {
        target.addEffect(new MobEffectInstance(AddonMobEffects.BANISHMENT, 25, 0, false, true, true), source);
        target.addEffect(new MobEffectInstance(
                MobEffects.WEAKNESS,
                ABSORPTION_DEBUFF_DURATION_TICKS,
                WEAKNESS_III_AMPLIFIER,
                false,
                true,
                true
        ), source);
    }

    private static void syncVoidAbsorptionLink(Entity source, LivingEntity target) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                source,
                new VoidAbsorptionLinkPayload(source.getId(), target.getId(), 3, SakuraParticleService.palette(source))
        );
    }

    private static LivingEntity rayTraceLiving(ServerPlayer player) {
        double range = GameplayConfig.LIVING_ABSORPTION_RANGE.get();
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
        LivingEntity closest = null;
        double closestDistance = start.distanceToSqr(end);
        for (Entity candidate : player.level().getEntities(
                player,
                search,
                entity -> entity instanceof LivingEntity living && living.isAlive()
                        || entity instanceof EnderDragonPart part && part.parentMob.isAlive()
        )) {
            AABB bounds = candidate.getBoundingBox().inflate(Math.max(0.3F, candidate.getPickRadius()));
            var intersection = bounds.clip(start, end);
            if (intersection.isPresent()) {
                double distance = start.distanceToSqr(intersection.get());
                if (distance <= closestDistance) {
                    closest = candidate instanceof EnderDragonPart part ? part.parentMob : (LivingEntity) candidate;
                    closestDistance = distance;
                }
            }
        }
        return closest;
    }

    private static boolean isAimingAt(ServerPlayer caster, LivingEntity target) {
        LivingEntity aimedTarget = rayTraceLiving(caster);
        return aimedTarget != null && aimedTarget.getUUID().equals(target.getUUID());
    }

    private static boolean validCaster(ServerPlayer caster) {
        return caster != null
                && caster.isAlive()
                && !caster.isSpectator()
                && SakuraTypeMoonIntegration.isAbsorptionLearned(caster);
    }

    private static boolean validTarget(ServerPlayer caster, LivingEntity target) {
        if (target == null
                || target == caster
                || !target.isAlive()
                || target.isRemoved()
                || target.level() != caster.level()) {
            return false;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        double range = GameplayConfig.LIVING_ABSORPTION_RANGE.get();
        return target.getBoundingBox().distanceToSqr(caster.getEyePosition()) <= range * range;
    }

    private static LivingEntity living(MinecraftServer server, ResourceKey<Level> dimension, UUID id) {
        ServerLevel level = server.getLevel(dimension);
        Entity entity = level == null ? null : level.getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private static void notify(ServerPlayer player, String key) {
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(key), true);
    }

    private static void notify(ServerPlayer player, String key, Object... arguments) {
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(key, arguments), true);
    }

    private record ShadowBinding(UUID casterId, UUID targetId, ResourceKey<Level> dimension, Vec3 targetAnchor, boolean targetWasNoAi) {
    }

    private record PendingDissolution(
            UUID targetId,
            ResourceKey<Level> dimension,
            long finishTick,
            boolean wasInvulnerable,
            boolean wasNoAi,
            boolean killAtEnd
    ) {
    }

    private static final class DirectTargetLock {
        private final boolean targetWasNoAi;
        private final Set<UUID> casterIds = new HashSet<>();

        private DirectTargetLock(boolean targetWasNoAi) {
            this.targetWasNoAi = targetWasNoAi;
        }
    }

    private SakuraImaginaryShadowService() {
    }
}
