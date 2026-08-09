package com.example.typemoonaddon.storm;

import com.example.typemoonaddon.magic.StormMagic;
import com.example.typemoonaddon.network.StormCastInputPayload;
import com.example.typemoonaddon.network.AddonSpellVisualPayload;
import com.example.typemoonaddon.kimaris.KimarisService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.combat.OriginBulletHelper;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.martial.BajiquanCombatService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

public final class StormService {
    public static final double MAX_INJECTED_MANA = 500.0D;
    public static final double MANA_PER_TICK = 5.0D;
    public static final int MAX_FULL_CHARGE_MAINTAIN_TICKS = 600;

    public static final int FULL_TORNADO_LIFETIME_TICKS = 200;
    public static final int FULL_TORNADO_COUNT = 8;
    public static final double FULL_TORNADO_WIDTH = 5.0D;
    public static final double FULL_TORNADO_HEIGHT = 14.0D;
    public static final double FULL_TORNADO_PULL_AREA_SIZE = 20.0D;
    public static final float BROKEN_PHANTASM_TIER_DAMAGE = 250.0F;
    public static final float FULL_TORNADO_DAMAGE = BROKEN_PHANTASM_TIER_DAMAGE;
    public static final int FULL_TORNADO_DAMAGE_INTERVAL_TICKS = 10;

    public static final double MIN_EFFECT_RADIUS = 4.0D;
    public static final double MAX_EFFECT_RADIUS = 24.0D;
    public static final float MIN_DAMAGE = 40.0F;
    public static final float MAX_DAMAGE = BROKEN_PHANTASM_TIER_DAMAGE;
    public static final int DAMAGE_COOLDOWN_TICKS = 10;
    public static final double MIN_HORIZONTAL_KNOCKBACK = 0.5D;
    public static final double MAX_HORIZONTAL_KNOCKBACK = 3.0D;
    public static final double MIN_UPWARD_FORCE = 0.05D;
    public static final double MAX_UPWARD_FORCE = 0.7D;
    public static final int MIN_CYCLONE_COUNT = 2;
    public static final int MAX_CYCLONE_COUNT = 8;
    public static final double MIN_CYCLONE_RADIUS = 2.0D;
    public static final double MAX_CYCLONE_RADIUS = 4.5D;
    public static final double MIN_CYCLONE_HEIGHT = 6.0D;
    public static final double MAX_CYCLONE_HEIGHT = 18.0D;
    public static final double PLAYER_KNOCKBACK_MULTIPLIER = 0.65D;

    public static final int ENTITY_SCAN_INTERVAL_TICKS = 5;
    public static final int MANA_SYNC_INTERVAL_TICKS = 5;
    public static final int VFX_BROADCAST_INTERVAL_TICKS = 5;
    private static final int SIGIL_VFX_INTERVAL_TICKS = 10;
    private static final int FULL_TORNADO_VFX_INTERVAL_TICKS = 8;
    private static final double CYCLONE_BELOW_BASE_TOLERANCE = 1.5D;
    private static final double CYCLONE_ATTRACTION_MULTIPLIER = 0.36D;
    private static final double CYCLONE_TANGENTIAL_MULTIPLIER = 0.48D;
    private static final double CYCLONE_LIFT_MULTIPLIER = 0.75D;
    private static final double COMBINED_FORCE_CAP_MULTIPLIER = 1.2D;
    private static final double COMBINED_LIFT_CAP_MULTIPLIER = 1.25D;
    private static final double MIN_OUTER_FORCE_FACTOR = 0.08D;
    private static final double MIN_VFX_OBSERVER_RADIUS = 64.0D;
    private static final double MAX_VFX_OBSERVER_RADIUS = 192.0D;
    private static final double FULL_TORNADO_MOVE_SPEED = 0.12D;
    private static final double FULL_TORNADO_MIN_PULL = 0.12D;
    private static final double FULL_TORNADO_MAX_PULL = 0.72D;
    private static final double FULL_TORNADO_MAX_HORIZONTAL_SPEED = 1.35D;
    private static final double FULL_TORNADO_THROW_HORIZONTAL = 2.8D;
    private static final double FULL_TORNADO_THROW_UPWARD = 1.1D;

    private static final Map<UUID, ChannelState> CHANNELS = new HashMap<>();

    private StormService() {
    }

    public static void handleCastInput(ServerPlayer player, byte action) {
        if (KimarisService.isFrozen(player)) {
            stop(player, false);
            return;
        }
        switch (action) {
            case StormCastInputPayload.PRESS -> begin(player);
            case StormCastInputPayload.RELEASE -> finish(player, EndReason.RELEASED);
            case StormCastInputPayload.CANCEL -> finish(player, EndReason.INVALID);
            default -> {
            }
        }
    }

    public static void tick(ServerPlayer player) {
        ChannelState state = CHANNELS.get(player.getUUID());
        if (state == null) {
            return;
        }
        if (!isValidCaster(player) || !state.dimension.equals(player.serverLevel().dimension())) {
            finish(player, EndReason.INVALID);
            return;
        }

        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (state.fullChargeTick >= 0L
                && now - state.fullChargeTick >= MAX_FULL_CHARGE_MAINTAIN_TICKS) {
            finish(player, EndReason.MAX_DURATION);
            return;
        }

        state.activeTicks++;
        boolean manaDepleted = injectMana(player, state, now);
        StormParameters parameters = StormParameters.fromMana(state.injectedMana);
        boolean fullCharge = state.fullChargeTick >= 0L;

        if (now - state.lastSigilVfxTick >= SIGIL_VFX_INTERVAL_TICKS) {
            state.lastSigilVfxTick = now;
            double observerRadius = Mth.lerp(parameters.power,
                    MIN_VFX_OBSERVER_RADIUS, MAX_VFX_OBSERVER_RADIUS);
            VFXServerEffects.spawn(level, "typemoonworld:storm_sigil",
                    player, observerRadius);
            AddonSpellVisualPayload.showAttached(
                    level, AddonSpellVisualPayload.STORM, AddonSpellVisualPayload.SIGIL,
                    player.getUUID(), player, (float) parameters.power, 1.0F,
                    SIGIL_VFX_INTERVAL_TICKS + 6, 0, observerRadius);
        }

        if (fullCharge) {
            tickFullChargeTornadoes(player, state, now);
        }

        if (now - state.lastScanTick >= ENTITY_SCAN_INTERVAL_TICKS) {
            state.lastScanTick = now;
            if (!fullCharge) {
                affectTargets(player, state, parameters, now);
            }
        }
        if (now - state.lastVfxTick >= VFX_BROADCAST_INTERVAL_TICKS) {
            state.lastVfxTick = now;
            if (fullCharge) {
                VFXServerEffects.spawn(level, "typemoonworld:storm_gale_high",
                        player, MAX_VFX_OBSERVER_RADIUS);
            } else {
                broadcastVfx(player, state, parameters, now);
            }
        }

        if (manaDepleted) {
            finish(player, EndReason.MANA_DEPLETED);
        }
    }

    public static void stop(Player player, boolean notify) {
        if (player instanceof ServerPlayer serverPlayer) {
            finish(serverPlayer, notify ? EndReason.RELEASED : EndReason.INVALID);
        } else if (player != null) {
            CHANNELS.remove(player.getUUID());
        }
    }

    public static void clearAll(MinecraftServer server) {
        for (UUID playerId : List.copyOf(CHANNELS.keySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                finish(player, EndReason.INVALID);
            }
        }
        CHANNELS.clear();
    }

    private static void begin(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (CHANNELS.containsKey(playerId) || !isValidCaster(player)) {
            return;
        }

        TypeMoonWorldModVariables.PlayerVariables vars =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        double currentMana = sanitizeMana(vars);
        if (currentMana <= 0.0D) {
            vars.syncMana(player);
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.storm.mana_depleted"), true);
            return;
        }

        ServerLevel level = player.serverLevel();
        CHANNELS.put(playerId, new ChannelState(
                level.dimension(),
                level.getRandom().nextDouble() * Math.PI * 2.0D
        ));
        player.displayClientMessage(Component.translatable(
                "message.typemoonworld.storm.started"), true);
    }

    private static void finish(ServerPlayer player, EndReason reason) {
        ChannelState state = CHANNELS.remove(player.getUUID());
        if (state == null) {
            return;
        }
        syncManaIfDirty(player, state);
        state.hitCooldowns.clear();
        dissipateFullTornadoes(player, state, true);
        AddonSpellVisualPayload.clear(
                player, AddonSpellVisualPayload.STORM, MAX_VFX_OBSERVER_RADIUS);

        String messageKey = switch (reason) {
            case RELEASED, MAX_DURATION -> "message.typemoonworld.storm.ended";
            case MANA_DEPLETED -> "message.typemoonworld.storm.mana_depleted";
            case INVALID -> null;
        };
        if (messageKey != null) {
            player.displayClientMessage(Component.translatable(messageKey), true);
        }
    }

    private static boolean injectMana(ServerPlayer player, ChannelState state, long now) {
        double remaining = Math.max(0.0D, MAX_INJECTED_MANA - state.injectedMana);
        if (remaining <= 0.0D) {
            return false;
        }

        TypeMoonWorldModVariables.PlayerVariables vars =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        double rawMana = vars.player_mana;
        double currentMana = sanitizeMana(vars);
        if (rawMana != currentMana) {
            state.manaDirty = true;
        }
        double injected = Math.min(MANA_PER_TICK, Math.min(currentMana, remaining));
        if (injected > 0.0D) {
            vars.player_mana = Math.max(0.0D, currentMana - injected);
            state.injectedMana = Mth.clamp(
                    state.injectedMana + injected, 0.0D, MAX_INJECTED_MANA);
            state.manaDirty = true;
        }

        boolean reachedMaximum = state.injectedMana >= MAX_INJECTED_MANA;
        if (reachedMaximum && state.fullChargeTick < 0L) {
            state.injectedMana = MAX_INJECTED_MANA;
            state.fullChargeTick = now;
            syncManaIfDirty(player, state);
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.storm.max_charge"), true);
            return false;
        }

        boolean depleted = injected < Math.min(MANA_PER_TICK, remaining)
                || vars.player_mana <= 0.0D;
        if (state.activeTicks % MANA_SYNC_INTERVAL_TICKS == 0 || depleted) {
            syncManaIfDirty(player, state);
        }
        return depleted;
    }

    private static void affectTargets(
            ServerPlayer player,
            ChannelState state,
            StormParameters parameters,
            long now
    ) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position().add(0.0D, 0.2D, 0.0D);
        AABB query = new AABB(
                center.x - parameters.effectRadius,
                center.y - CYCLONE_BELOW_BASE_TOLERANCE,
                center.z - parameters.effectRadius,
                center.x + parameters.effectRadius,
                center.y + parameters.cycloneHeight,
                center.z + parameters.effectRadius
        );
        List<Cyclone> cyclones = allCyclonePositions(player, state, parameters, now);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                query,
                target -> EntityUtils.isValidCombatTarget(player, target)
        );
        DamageSource damageSource = player.damageSources().source(DamageTypes.MAGIC, player);

        pruneCooldowns(state, now);
        for (LivingEntity target : targets) {
            Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            if (!isInsideCyclone(targetCenter, cyclones, parameters.cycloneHeight)
                    || !player.hasLineOfSight(target)) {
                continue;
            }

            if (now >= state.hitCooldowns.getOrDefault(target.getUUID(), Long.MIN_VALUE)) {
                if (target.hurt(damageSource, parameters.damage)) {
                    state.hitCooldowns.put(target.getUUID(), now + DAMAGE_COOLDOWN_TICKS);
                }
            }

            if (!isForceImmune(target)) {
                applyCombinedCycloneForce(target, targetCenter, cyclones, parameters);
            }
        }
    }

    private static void applyCombinedCycloneForce(
            LivingEntity target,
            Vec3 targetCenter,
            List<Cyclone> cyclones,
            StormParameters parameters
    ) {
        Vec3 horizontalForce = Vec3.ZERO;
        double upwardForce = 0.0D;

        for (Cyclone cyclone : cyclones) {
            if (!isWithinCycloneHeight(targetCenter, cyclone, parameters.cycloneHeight)) {
                continue;
            }
            Vec3 fromCyclone = horizontal(targetCenter.subtract(cyclone.center));
            double cycloneDistance = fromCyclone.length();
            if (cycloneDistance > cyclone.radius) {
                continue;
            }
            double cycloneFactor = Mth.clamp(
                    1.0D - cycloneDistance / cyclone.radius, 0.0D, 1.0D);
            Vec3 outward = normalizedOrDeterministic(fromCyclone, target.getUUID());
            Vec3 towardCenter = outward.scale(-1.0D);
            Vec3 tangent = new Vec3(-outward.z, 0.0D, outward.x);
            horizontalForce = horizontalForce
                    .add(towardCenter.scale(parameters.horizontalKnockback
                            * CYCLONE_ATTRACTION_MULTIPLIER
                            * Math.max(MIN_OUTER_FORCE_FACTOR, cycloneFactor)))
                    .add(tangent.scale(parameters.horizontalKnockback
                            * CYCLONE_TANGENTIAL_MULTIPLIER
                            * Math.max(MIN_OUTER_FORCE_FACTOR, cycloneFactor)));
            upwardForce += parameters.upwardForce * CYCLONE_LIFT_MULTIPLIER
                    * Math.max(MIN_OUTER_FORCE_FACTOR, cycloneFactor);
        }

        double resistance = Mth.clamp(
                target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0D, 1.0D);
        double targetMultiplier = target instanceof Player ? PLAYER_KNOCKBACK_MULTIPLIER : 1.0D;
        double effectiveMultiplier = (1.0D - resistance) * targetMultiplier;
        if (effectiveMultiplier <= 0.0001D) {
            return;
        }

        double maxHorizontal = parameters.horizontalKnockback * COMBINED_FORCE_CAP_MULTIPLIER;
        if (horizontalForce.lengthSqr() > maxHorizontal * maxHorizontal) {
            horizontalForce = horizontalForce.normalize().scale(maxHorizontal);
        }
        horizontalForce = horizontalForce.scale(effectiveMultiplier);
        upwardForce = Mth.clamp(
                upwardForce, 0.0D, parameters.upwardForce * COMBINED_LIFT_CAP_MULTIPLIER)
                * effectiveMultiplier;

        Vec3 movement = target.getDeltaMovement();
        target.setDeltaMovement(movement.x + horizontalForce.x,
                movement.y + upwardForce, movement.z + horizontalForce.z);
        target.hasImpulse = true;
        target.hurtMarked = true;
    }

    private static List<Cyclone> cyclonePositions(
            ServerPlayer player,
            ChannelState state,
            StormParameters parameters,
            long now
    ) {
        int count = Mth.clamp(parameters.cycloneCount, MIN_CYCLONE_COUNT, MAX_CYCLONE_COUNT);
        List<Cyclone> result = new ArrayList<>(count);
        double baseOrbit = parameters.effectRadius * 0.72D;
        double maxOrbit = Math.max(1.25D,
                parameters.effectRadius - parameters.cycloneRadius);
        for (int index = 0; index < count; index++) {
            double phase = state.phaseOffset + state.activeTicks * 0.105D
                    + Math.PI * 2.0D * index / count;
            double wander = Math.sin(state.activeTicks * 0.071D + index * 1.73D)
                    * parameters.effectRadius * 0.1D;
            double orbit = Math.min(maxOrbit, Math.max(1.25D, baseOrbit + wander));
            Vec3 center = player.position().add(
                    Math.cos(phase) * orbit,
                    0.2D,
                    Math.sin(phase) * orbit
            );
            result.add(new Cyclone(center, parameters.cycloneRadius));
        }
        return result;
    }

    private static List<Cyclone> allCyclonePositions(
            ServerPlayer player,
            ChannelState state,
            StormParameters parameters,
            long now
    ) {
        List<Cyclone> result = new ArrayList<>(parameters.cycloneCount + 1);
        result.add(new Cyclone(player.position().add(0.0D, 0.2D, 0.0D),
                parameters.cycloneRadius));
        result.addAll(cyclonePositions(player, state, parameters, now));
        return result;
    }

    private static boolean isInsideCyclone(
            Vec3 targetCenter,
            List<Cyclone> cyclones,
            double cycloneHeight
    ) {
        for (Cyclone cyclone : cyclones) {
            if (isWithinCycloneHeight(targetCenter, cyclone, cycloneHeight)
                    && horizontal(targetCenter.subtract(cyclone.center)).lengthSqr()
                    <= cyclone.radius * cyclone.radius) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWithinCycloneHeight(
            Vec3 targetCenter,
            Cyclone cyclone,
            double cycloneHeight
    ) {
        return targetCenter.y >= cyclone.center.y - CYCLONE_BELOW_BASE_TOLERANCE
                && targetCenter.y <= cyclone.center.y + cycloneHeight;
    }

    private static void tickFullChargeTornadoes(
            ServerPlayer player,
            ChannelState state,
            long now
    ) {
        if (!state.fullTornadoes.isEmpty()
                && now - state.fullTornadoes.getFirst().spawnTick >= FULL_TORNADO_LIFETIME_TICKS) {
            dissipateFullTornadoes(player, state, true);
        }
        if (state.fullTornadoes.isEmpty()) {
            createFullTornadoes(player, state, now);
        }

        for (FullTornado tornado : state.fullTornadoes) {
            moveFullTornado(player.serverLevel(), tornado, now);
        }
        if (now - state.lastScanTick >= ENTITY_SCAN_INTERVAL_TICKS) {
            state.lastScanTick = now;
            affectFullTornadoTargets(player, state, now);
        }
        for (FullTornado tornado : state.fullTornadoes) {
            if (now - tornado.lastVfxTick < FULL_TORNADO_VFX_INTERVAL_TICKS) {
                continue;
            }
            tornado.lastVfxTick = now;
            VFXServerEffects.spawn(player.serverLevel(),
                    "typemoonworld:storm_full_tornado", tornado.center,
                    MAX_VFX_OBSERVER_RADIUS);
            AddonSpellVisualPayload.showAt(
                    player.serverLevel(), AddonSpellVisualPayload.STORM,
                    AddonSpellVisualPayload.TORNADO, player.getUUID(), tornado.center,
                    (float) (FULL_TORNADO_WIDTH * 0.5D), (float) FULL_TORNADO_HEIGHT,
                    FULL_TORNADO_VFX_INTERVAL_TICKS + 8, tornado.visualIndex,
                    MAX_VFX_OBSERVER_RADIUS);
            player.serverLevel().sendParticles(ParticleTypes.GUST,
                    tornado.center.x, tornado.center.y + 5.0D, tornado.center.z,
                    12, 1.75D, 4.6D, 1.75D, 0.13D);
        }
    }

    private static void createFullTornadoes(
            ServerPlayer player,
            ChannelState state,
            long now
    ) {
        ServerLevel level = player.serverLevel();
        ChunkPos chunk = player.chunkPosition();
        double halfWidth = FULL_TORNADO_WIDTH * 0.5D;
        double minX = chunk.getMinBlockX() + halfWidth;
        double maxX = chunk.getMaxBlockX() + 1.0D - halfWidth;
        double minZ = chunk.getMinBlockZ() + halfWidth;
        double maxZ = chunk.getMaxBlockZ() + 1.0D - halfWidth;
        AABB chunkBounds = new AABB(
                chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                chunk.getMaxBlockX() + 1.0D, level.getMaxBuildHeight(),
                chunk.getMaxBlockZ() + 1.0D
        );
        List<LivingEntity> availableTargets = new ArrayList<>(level.getEntitiesOfClass(
                LivingEntity.class,
                chunkBounds,
                target -> isFullTornadoTarget(player, target)
        ));

        Vec3 lookDirection = normalizedOrDeterministic(
                horizontal(player.getLookAngle()), player.getUUID());
        double chunkCenterX = chunk.getMinBlockX() + 8.0D;
        double chunkCenterZ = chunk.getMinBlockZ() + 8.0D;
        double formationRadius = Math.max(2.0D,
                Math.min(4.75D, (maxX - minX) * 0.43D));

        for (int index = 0; index < FULL_TORNADO_COUNT; index++) {
            double angle = state.phaseOffset + Math.PI * 2.0D * index / FULL_TORNADO_COUNT;
            Vec3 desired = new Vec3(
                    chunkCenterX + Math.cos(angle) * formationRadius,
                    player.getY(),
                    chunkCenterZ + Math.sin(angle) * formationRadius
            );
            LivingEntity lockedTarget = availableTargets.stream()
                    .min(Comparator
                            .comparingInt((LivingEntity target) -> target instanceof Mob ? 0 : 1)
                            .thenComparingDouble(target -> target.position().distanceToSqr(desired)))
                    .orElse(null);
            Vec3 formation = lockedTarget == null ? desired : lockedTarget.position();
            if (lockedTarget != null) {
                availableTargets.remove(lockedTarget);
            }

            double x = Mth.clamp(formation.x, minX, maxX);
            double z = Mth.clamp(formation.z, minZ, maxZ);
            double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Mth.floor(x), Mth.floor(z));
            Vec3 direction = horizontal(formation.subtract(player.position()));
            if (direction.lengthSqr() <= 1.0E-8D) {
                direction = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            } else {
                direction = direction.normalize();
            }

            state.fullTornadoSequence++;
            state.fullTornadoes.add(new FullTornado(
                    new Vec3(x, y, z),
                    normalizedOrDeterministic(direction, player.getUUID()),
                    chunk,
                    now,
                    state.phaseOffset + state.fullTornadoSequence * 1.913D,
                    index
            ));
        }
    }

    private static void moveFullTornado(
            ServerLevel level,
            FullTornado tornado,
            long now
    ) {
        double age = now - tornado.spawnTick;
        double turn = Math.sin(age * 0.173D + tornado.wanderPhase) * 0.052D
                + Math.sin(age * 0.071D + tornado.wanderPhase * 1.7D) * 0.024D;
        double cos = Math.cos(turn);
        double sin = Math.sin(turn);
        Vec3 direction = new Vec3(
                tornado.direction.x * cos - tornado.direction.z * sin,
                0.0D,
                tornado.direction.x * sin + tornado.direction.z * cos
        ).normalize();

        double halfWidth = FULL_TORNADO_WIDTH * 0.5D;
        double minX = tornado.chunk.getMinBlockX() + halfWidth;
        double maxX = tornado.chunk.getMaxBlockX() + 1.0D - halfWidth;
        double minZ = tornado.chunk.getMinBlockZ() + halfWidth;
        double maxZ = tornado.chunk.getMaxBlockZ() + 1.0D - halfWidth;
        double nextX = tornado.center.x + direction.x * FULL_TORNADO_MOVE_SPEED;
        double nextZ = tornado.center.z + direction.z * FULL_TORNADO_MOVE_SPEED;
        if (nextX < minX || nextX > maxX) {
            direction = new Vec3(-direction.x, 0.0D, direction.z);
        }
        if (nextZ < minZ || nextZ > maxZ) {
            direction = new Vec3(direction.x, 0.0D, -direction.z);
        }
        direction = normalizedOrDeterministic(direction, new UUID(
                Double.doubleToLongBits(tornado.wanderPhase), tornado.spawnTick));
        nextX = Mth.clamp(tornado.center.x
                + direction.x * FULL_TORNADO_MOVE_SPEED, minX, maxX);
        nextZ = Mth.clamp(tornado.center.z
                + direction.z * FULL_TORNADO_MOVE_SPEED, minZ, maxZ);
        double groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mth.floor(nextX), Mth.floor(nextZ));
        tornado.center = new Vec3(nextX,
                Mth.lerp(0.28D, tornado.center.y, groundY), nextZ);
        tornado.direction = direction;
    }

    private static void affectFullTornadoTargets(
            ServerPlayer player,
            ChannelState state,
            long now
    ) {
        if (state.fullTornadoes.isEmpty()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        double halfArea = FULL_TORNADO_PULL_AREA_SIZE * 0.5D;
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (FullTornado tornado : state.fullTornadoes) {
            minX = Math.min(minX, tornado.center.x - halfArea);
            minY = Math.min(minY, tornado.center.y - 4.0D);
            minZ = Math.min(minZ, tornado.center.z - halfArea);
            maxX = Math.max(maxX, tornado.center.x + halfArea);
            maxY = Math.max(maxY, tornado.center.y + FULL_TORNADO_HEIGHT + 4.0D);
            maxZ = Math.max(maxZ, tornado.center.z + halfArea);
        }
        AABB pullArea = new AABB(
                minX, minY, minZ,
                maxX, maxY, maxZ
        );
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                pullArea,
                target -> isFullTornadoTarget(player, target)
        );
        DamageSource damageSource = player.damageSources().source(DamageTypes.MAGIC, player);
        pruneCooldownMap(state.hitCooldowns, now);

        for (LivingEntity target : targets) {
            Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            FullTornado tornado = nearestEffectiveTornado(
                    level, player, state.fullTornadoes, targetCenter, halfArea);
            if (tornado == null) {
                continue;
            }
            if (now >= state.hitCooldowns.getOrDefault(
                    target.getUUID(), Long.MIN_VALUE)) {
                if (target.hurt(damageSource, FULL_TORNADO_DAMAGE)) {
                    state.hitCooldowns.put(target.getUUID(),
                            now + FULL_TORNADO_DAMAGE_INTERVAL_TICKS);
                }
            }

            double horizontalDistance = horizontal(
                    targetCenter.subtract(tornado.center)).length();
            boolean insideCore = horizontalDistance <= FULL_TORNADO_WIDTH * 0.5D
                    && targetCenter.y >= tornado.center.y - 1.0D
                    && targetCenter.y <= tornado.center.y + FULL_TORNADO_HEIGHT;
            if (insideCore) {
                tornado.capturedTargets.add(target.getUUID());
            }
            if (!isForceImmune(target)) {
                applyFullTornadoPull(target, targetCenter, tornado, halfArea, insideCore);
            }
        }
    }

    private static FullTornado nearestEffectiveTornado(
            ServerLevel level,
            ServerPlayer player,
            List<FullTornado> tornadoes,
            Vec3 targetCenter,
            double halfArea
    ) {
        FullTornado nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (FullTornado tornado : tornadoes) {
            if (Math.abs(targetCenter.x - tornado.center.x) > halfArea
                    || Math.abs(targetCenter.z - tornado.center.z) > halfArea
                    || targetCenter.y < tornado.center.y - 4.0D
                    || targetCenter.y > tornado.center.y + FULL_TORNADO_HEIGHT + 4.0D
                    || !hasClearPathFromTornado(level, player, tornado.center, targetCenter)) {
                continue;
            }
            double distance = targetCenter.distanceToSqr(tornado.center);
            if (distance < nearestDistance) {
                nearest = tornado;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static void applyFullTornadoPull(
            LivingEntity target,
            Vec3 targetCenter,
            FullTornado tornado,
            double halfArea,
            boolean insideCore
    ) {
        Vec3 horizontalToCenter = horizontal(tornado.center.subtract(targetCenter));
        Vec3 inward = normalizedOrDeterministic(horizontalToCenter, target.getUUID());
        double squareDistance = Math.max(
                Math.abs(targetCenter.x - tornado.center.x),
                Math.abs(targetCenter.z - tornado.center.z));
        double distanceFactor = Mth.clamp(1.0D - squareDistance / halfArea, 0.0D, 1.0D);
        double pull = Mth.lerp(distanceFactor,
                FULL_TORNADO_MIN_PULL, FULL_TORNADO_MAX_PULL);
        Vec3 tangent = new Vec3(-inward.z, 0.0D, inward.x);
        Vec3 horizontalForce = inward.scale(pull)
                .add(tangent.scale((insideCore ? 0.52D : 0.08D + distanceFactor * 0.18D)));
        double targetHeight = tornado.center.y + FULL_TORNADO_HEIGHT * 0.55D;
        double verticalForce = Mth.clamp(
                (targetHeight - targetCenter.y) * (insideCore ? 0.085D : 0.035D),
                -0.24D,
                insideCore ? 0.55D : 0.28D
        );

        double resistance = Mth.clamp(
                target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0D, 1.0D);
        double targetMultiplier = target instanceof Player
                ? PLAYER_KNOCKBACK_MULTIPLIER : 1.0D;
        double effectiveMultiplier = (1.0D - resistance) * targetMultiplier;
        if (effectiveMultiplier <= 0.0001D) {
            return;
        }
        horizontalForce = horizontalForce.scale(effectiveMultiplier);
        verticalForce *= effectiveMultiplier;

        Vec3 movement = target.getDeltaMovement();
        Vec3 horizontalMovement = new Vec3(
                movement.x + horizontalForce.x, 0.0D,
                movement.z + horizontalForce.z);
        if (horizontalMovement.lengthSqr()
                > FULL_TORNADO_MAX_HORIZONTAL_SPEED * FULL_TORNADO_MAX_HORIZONTAL_SPEED) {
            horizontalMovement = horizontalMovement.normalize()
                    .scale(FULL_TORNADO_MAX_HORIZONTAL_SPEED);
        }
        target.setDeltaMovement(horizontalMovement.x,
                movement.y + verticalForce, horizontalMovement.z);
        target.hasImpulse = true;
        target.hurtMarked = true;
    }

    private static boolean hasClearPathFromTornado(
            ServerLevel level,
            ServerPlayer caster,
            Vec3 tornadoCenter,
            Vec3 targetCenter
    ) {
        Vec3 eye = tornadoCenter.add(0.0D, FULL_TORNADO_HEIGHT * 0.5D, 0.0D);
        return level.clip(new ClipContext(
                eye,
                targetCenter,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                caster
        )).getType() == HitResult.Type.MISS;
    }

    private static boolean isFullTornadoTarget(
            ServerPlayer caster,
            LivingEntity target
    ) {
        return target != caster
                && target.isAlive()
                && !target.isRemoved()
                && !(target instanceof ArmorStand)
                && (!(target instanceof Player player) || !player.isSpectator());
    }

    private static void dissipateFullTornadoes(
            ServerPlayer player,
            ChannelState state,
            boolean throwCaptured
    ) {
        if (state.fullTornadoes.isEmpty()) {
            return;
        }
        List<FullTornado> tornadoes = List.copyOf(state.fullTornadoes);
        state.fullTornadoes.clear();
        ServerLevel level = player.getServer() == null
                ? null : player.getServer().getLevel(state.dimension);
        if (!throwCaptured || level == null) {
            tornadoes.forEach(tornado -> tornado.capturedTargets.clear());
            return;
        }

        Set<UUID> thrownTargets = new HashSet<>();
        for (FullTornado tornado : tornadoes) {
            for (UUID targetId : tornado.capturedTargets) {
                if (!thrownTargets.add(targetId)
                        || !(level.getEntity(targetId) instanceof LivingEntity target)
                        || !target.isAlive()
                        || isForceImmune(target)) {
                    continue;
                }
                Vec3 outward = horizontal(target.position().subtract(tornado.center));
                if (outward.lengthSqr() <= 1.0E-8D) {
                    outward = tornado.direction;
                } else {
                    outward = outward.normalize();
                }
                Vec3 throwDirection = outward.scale(0.58D)
                        .add(tornado.direction.scale(0.82D));
                throwDirection = normalizedOrDeterministic(throwDirection, targetId);
                double resistance = Mth.clamp(
                        target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0D, 1.0D);
                double targetMultiplier = target instanceof Player
                        ? PLAYER_KNOCKBACK_MULTIPLIER : 1.0D;
                double multiplier = (1.0D - resistance) * targetMultiplier;
                if (multiplier <= 0.0001D) {
                    continue;
                }
                Vec3 movement = target.getDeltaMovement();
                target.setDeltaMovement(
                        movement.x + throwDirection.x * FULL_TORNADO_THROW_HORIZONTAL * multiplier,
                        Math.max(movement.y,
                                FULL_TORNADO_THROW_UPWARD * multiplier),
                        movement.z + throwDirection.z * FULL_TORNADO_THROW_HORIZONTAL * multiplier
                );
                target.hasImpulse = true;
                target.hurtMarked = true;
            }
            tornado.capturedTargets.clear();
        }
    }

    private static void broadcastVfx(
            ServerPlayer player,
            ChannelState state,
            StormParameters parameters,
            long now
    ) {
        ServerLevel level = player.serverLevel();
        double observerRadius = Mth.lerp(parameters.power,
                MIN_VFX_OBSERVER_RADIUS, MAX_VFX_OBSERVER_RADIUS);
        String tier = parameters.power < 0.25D ? "low"
                : parameters.power < 0.7D ? "medium" : "high";
        String cycloneEffectId = "typemoonworld:storm_cyclone_" + tier;
        VFXServerEffects.spawn(level, "typemoonworld:storm_gale_" + tier,
                player, observerRadius);
        VFXServerEffects.spawn(level, cycloneEffectId, player, observerRadius);
        AddonSpellVisualPayload.showAttached(
                level, AddonSpellVisualPayload.STORM, AddonSpellVisualPayload.TORNADO,
                player.getUUID(), player, (float) parameters.cycloneRadius,
                (float) parameters.cycloneHeight, VFX_BROADCAST_INTERVAL_TICKS + 5,
                -1, observerRadius);

        List<Cyclone> cyclones = cyclonePositions(player, state, parameters, now);
        for (int cycloneIndex = 0; cycloneIndex < cyclones.size(); cycloneIndex++) {
            Cyclone cyclone = cyclones.get(cycloneIndex);
            VFXServerEffects.spawn(level, cycloneEffectId, cyclone.center, observerRadius);
            AddonSpellVisualPayload.showAt(
                    level, AddonSpellVisualPayload.STORM, AddonSpellVisualPayload.TORNADO,
                    player.getUUID(), cyclone.center, (float) cyclone.radius,
                    (float) parameters.cycloneHeight, VFX_BROADCAST_INTERVAL_TICKS + 5,
                    100 + cycloneIndex, observerRadius);
        }

        List<Cyclone> allCyclones = new ArrayList<>(cyclones.size() + 1);
        allCyclones.add(new Cyclone(player.position().add(0.0D, 0.2D, 0.0D),
                parameters.cycloneRadius));
        allCyclones.addAll(cyclones);
        int cloudCount = Mth.clamp(2 + Mth.floor(parameters.power * 4.0D), 2, 6);
        int gustCount = Mth.clamp(1 + Mth.floor(parameters.power * 2.0D), 1, 3);
        for (Cyclone cyclone : allCyclones) {
            level.sendParticles(ParticleTypes.CLOUD,
                    cyclone.center.x, cyclone.center.y + 1.1D, cyclone.center.z,
                    cloudCount, cyclone.radius * 0.28D, 0.9D,
                    cyclone.radius * 0.28D, 0.055D);
            level.sendParticles(ParticleTypes.GUST,
                    cyclone.center.x, cyclone.center.y + 0.35D, cyclone.center.z,
                    gustCount, cyclone.radius * 0.32D, 0.45D,
                    cyclone.radius * 0.32D, 0.085D);
        }
    }

    private static boolean isForceImmune(LivingEntity target) {
        if (target instanceof ArmorStand
                || target instanceof EnderDragon
                || target instanceof WitherBoss
                || target instanceof Warden
                || target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 0.999D) {
            return true;
        }
        return target instanceof Mob mob && mob.isNoAi();
    }

    private static boolean isValidCaster(ServerPlayer player) {
        if (!StormMagic.isRegistered()
                || player == null
                || !player.isAlive()
                || player.isRemoved()
                || player.isSpectator()
                || player.hasDisconnected()
                || player.containerMenu != player.inventoryMenu
                || EntityUtils.isPetrified(player)
                || OriginBulletHelper.isSealed(player)
                || player.hasEffect(ModMobEffects.STAGGER)
                || player.hasEffect(ModMobEffects.OFF_BALANCE)
                || BajiquanCombatService.isSparring(player)) {
            return false;
        }

        TypeMoonWorldModVariables.PlayerVariables vars =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        vars.ensureMagicSystemInitialized();
        vars.rebuildSelectedMagicsFromActiveWheel();
        PlayerMagicSelectionService.prepareCurrentSelection(player, vars);
        return vars.is_magus
                && vars.is_magic_circuit_open
                && vars.magic_cooldown <= 0.0D
                && PlayerMagicSelectionService.isCurrentSelection(vars, StormMagic.MAGIC_ID);
    }

    private static void syncManaIfDirty(ServerPlayer player, ChannelState state) {
        if (!state.manaDirty) {
            return;
        }
        player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).syncMana(player);
        state.manaDirty = false;
    }

    private static void pruneCooldowns(ChannelState state, long now) {
        pruneCooldownMap(state.hitCooldowns, now);
    }

    private static void pruneCooldownMap(Map<UUID, Long> cooldowns, long now) {
        Iterator<Map.Entry<UUID, Long>> iterator = cooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() <= now) {
                iterator.remove();
            }
        }
    }

    private static Vec3 horizontal(Vec3 vector) {
        return new Vec3(vector.x, 0.0D, vector.z);
    }

    private static Vec3 normalizedOrDeterministic(Vec3 vector, UUID targetId) {
        if (vector.lengthSqr() > 1.0E-8D) {
            return vector.normalize();
        }
        double angle = Math.floorMod(targetId.hashCode(), 360) * Math.PI / 180.0D;
        return new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
    }

    private static double finiteNonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }

    private static double sanitizeMana(TypeMoonWorldModVariables.PlayerVariables vars) {
        double sanitized = finiteNonNegative(vars.player_mana);
        if (vars.player_mana != sanitized) {
            vars.player_mana = sanitized;
        }
        return sanitized;
    }

    private enum EndReason {
        RELEASED,
        MANA_DEPLETED,
        MAX_DURATION,
        INVALID
    }

    private static final class ChannelState {
        private final ResourceKey<Level> dimension;
        private final double phaseOffset;
        private final Map<UUID, Long> hitCooldowns = new HashMap<>();
        private final List<FullTornado> fullTornadoes = new ArrayList<>(FULL_TORNADO_COUNT);
        private int fullTornadoSequence;
        private long fullChargeTick = -1L;
        private long lastScanTick = Long.MIN_VALUE / 2L;
        private long lastVfxTick = Long.MIN_VALUE / 2L;
        private long lastSigilVfxTick = Long.MIN_VALUE / 2L;
        private int activeTicks;
        private double injectedMana;
        private boolean manaDirty;

        private ChannelState(ResourceKey<Level> dimension, double phaseOffset) {
            this.dimension = dimension;
            this.phaseOffset = phaseOffset;
        }
    }

    private static final class FullTornado {
        private Vec3 center;
        private Vec3 direction;
        private final ChunkPos chunk;
        private final long spawnTick;
        private final double wanderPhase;
        private final int visualIndex;
        private final Set<UUID> capturedTargets = new HashSet<>();
        private long lastVfxTick;

        private FullTornado(
                Vec3 center,
                Vec3 direction,
                ChunkPos chunk,
                long spawnTick,
                double wanderPhase,
                int vfxDelayTicks
        ) {
            this.center = center;
            this.direction = direction;
            this.chunk = chunk;
            this.spawnTick = spawnTick;
            this.wanderPhase = wanderPhase;
            this.visualIndex = vfxDelayTicks;
            this.lastVfxTick = spawnTick - FULL_TORNADO_VFX_INTERVAL_TICKS
                    + Mth.clamp(vfxDelayTicks, 0, FULL_TORNADO_VFX_INTERVAL_TICKS - 1);
        }
    }

    private record Cyclone(Vec3 center, double radius) {
    }

    private record StormParameters(
            double charge,
            double power,
            double effectRadius,
            float damage,
            double horizontalKnockback,
            double upwardForce,
            int cycloneCount,
            double cycloneRadius,
            double cycloneHeight
    ) {
        private static StormParameters fromMana(double injectedMana) {
            double charge = Mth.clamp(
                    finiteNonNegative(injectedMana) / MAX_INJECTED_MANA, 0.0D, 1.0D);
            double power = charge * charge;
            return new StormParameters(
                    charge,
                    power,
                    Mth.clamp(Mth.lerp(power, MIN_EFFECT_RADIUS, MAX_EFFECT_RADIUS),
                            MIN_EFFECT_RADIUS, MAX_EFFECT_RADIUS),
                    Mth.clamp(Mth.lerp((float) power, MIN_DAMAGE, MAX_DAMAGE),
                            MIN_DAMAGE, MAX_DAMAGE),
                    Mth.clamp(Mth.lerp(power, MIN_HORIZONTAL_KNOCKBACK, MAX_HORIZONTAL_KNOCKBACK),
                            MIN_HORIZONTAL_KNOCKBACK, MAX_HORIZONTAL_KNOCKBACK),
                    Mth.clamp(Mth.lerp(power, MIN_UPWARD_FORCE, MAX_UPWARD_FORCE),
                            MIN_UPWARD_FORCE, MAX_UPWARD_FORCE),
                    Mth.clamp(Mth.floor(Mth.lerp(power,
                                    (double) MIN_CYCLONE_COUNT, (double) MAX_CYCLONE_COUNT)),
                            MIN_CYCLONE_COUNT, MAX_CYCLONE_COUNT),
                    Mth.clamp(Mth.lerp(power, MIN_CYCLONE_RADIUS, MAX_CYCLONE_RADIUS),
                            MIN_CYCLONE_RADIUS, MAX_CYCLONE_RADIUS),
                    Mth.clamp(Mth.lerp(power, MIN_CYCLONE_HEIGHT, MAX_CYCLONE_HEIGHT),
                            MIN_CYCLONE_HEIGHT, MAX_CYCLONE_HEIGHT)
            );
        }
    }
}
