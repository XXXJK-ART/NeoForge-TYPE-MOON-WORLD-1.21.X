package com.example.typemoonaddon.zagan;

import com.example.typemoonaddon.magic.ZaganMagic;
import com.example.typemoonaddon.network.AddonSpellVisualPayload;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

/** Server-authoritative state for Zagan's anchored flat water vortex. */
public final class ZaganService {
    public static final double TARGET_LOCK_RANGE = 64.0D;
    public static final double EFFECT_RADIUS = 20.0D;
    public static final double TOTAL_DAMAGE_CAP = 300.0D;
    public static final float DAMAGE_PER_PULSE = 50.0F;
    public static final int PULSE_INTERVAL_TICKS = 10;
    public static final int EFFECT_DURATION_TICKS = 60;
    public static final int MAX_TARGETS = 32;
    public static final double WATER_BUBBLE_RADIUS = 5.0D;
    public static final int MAX_WATER_PRISON_BLOCKS = 600;
    public static final int WATER_PRISON_BLOCKS_PER_TICK = 80;

    private static final double EFFECT_RADIUS_SQUARED = EFFECT_RADIUS * EFFECT_RADIUS;
    private static final double WATER_BUBBLE_RADIUS_SQUARED = WATER_BUBBLE_RADIUS * WATER_BUBBLE_RADIUS;
    private static final double VFX_OBSERVER_RADIUS = 128.0D;
    private static final int VFX_INTERVAL_TICKS = 5;
    private static final double MAX_HORIZONTAL_SPEED = 0.52D;
    private static final double PLAYER_FORCE_MULTIPLIER = 0.72D;
    private static final double CONTROL_VERTICAL_MIN = -0.12D;
    private static final double CONTROL_VERTICAL_MAX = 0.2D;
    private static final double MAX_RESTORED_SPEED = 2.5D;

    private static final String LOCK_EFFECT = "typemoonworld:zagan_lock";
    private static final String SIGIL_EFFECT = "typemoonworld:zagan_sigil";
    private static final String FLAT_EFFECT = "typemoonworld:zagan_flat_torrent";
    private static final String PULSE_EFFECT = "typemoonworld:zagan_pulse";
    private static final String WATER_BUBBLE_EFFECT = "typemoonworld:zagan_water_bubble";
    private static final String END_EFFECT = "typemoonworld:zagan_end";

    private static final Map<UUID, CastState> ACTIVE = new HashMap<>();

    private ZaganService() {
    }

    /** Starts one cast after resolving the target again on the server. */
    public static boolean cast(ServerPlayer caster) {
        if (!isValidCaster(caster) || ACTIVE.containsKey(caster.getUUID())) {
            return false;
        }
        TypeMoonWorldModVariables.PlayerVariables vars =
                caster.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        if (!PlayerMagicSelectionService.isCurrentSelection(vars, ZaganMagic.MAGIC_ID)
                || !vars.is_magus
                || !vars.is_magic_circuit_open
                || !Double.isFinite(vars.magic_cooldown)
                || vars.magic_cooldown > 0.0D) {
            return false;
        }

        LivingEntity target = EntityUtils.findAutoAimTarget(caster, TARGET_LOCK_RANGE, 70.0D);
        if (target == null || !EntityUtils.isValidCombatTarget(caster, target)) {
            caster.displayClientMessage(Component.translatable(
                    "message.typemoonworld.zagan.no_target"), true);
            return false;
        }

        ServerLevel level = caster.serverLevel();
        Vec3 anchor = target.getBoundingBox().getCenter();
        long startTick = level.getGameTime();
        CastState state = new CastState(
                caster.getUUID(),
                target.getUUID(),
                level.dimension(),
                anchor,
                startTick,
                startTick + EFFECT_DURATION_TICKS
        );
        ACTIVE.put(caster.getUUID(), state);
        VFXServerEffects.spawn(level, SIGIL_EFFECT, caster, VFX_OBSERVER_RADIUS);
        VFXServerEffects.spawn(level, LOCK_EFFECT, anchor, VFX_OBSERVER_RADIUS);
        VFXServerEffects.spawn(level, FLAT_EFFECT, anchor, VFX_OBSERVER_RADIUS);
        AddonSpellVisualPayload.showAttached(
                level, AddonSpellVisualPayload.ZAGAN, AddonSpellVisualPayload.SIGIL,
                caster.getUUID(), caster, 1.0F, 1.0F, 26, 0, VFX_OBSERVER_RADIUS);
        AddonSpellVisualPayload.showAt(
                level, AddonSpellVisualPayload.ZAGAN, AddonSpellVisualPayload.WATER_FIELD,
                caster.getUUID(), anchor, (float) EFFECT_RADIUS, 5.0F,
                EFFECT_DURATION_TICKS + 5, 0, VFX_OBSERVER_RADIUS);
        level.sendParticles(ParticleTypes.SPLASH, anchor.x, anchor.y, anchor.z,
                28, 7.0D, 0.7D, 7.0D, 0.16D);
        caster.displayClientMessage(Component.translatable(
                "message.typemoonworld.zagan.cast"), true);
        return true;
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, CastState>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            CastState state = iterator.next().getValue();
            ServerPlayer caster = server.getPlayerList().getPlayer(state.casterId);
            ServerLevel level = server.getLevel(state.dimension);
            if (!isValidCaster(caster)
                    || level == null
                    || caster.serverLevel() != level
                    || !isTargetStillValid(level, state.targetId)) {
                finishState(server, state, level);
                iterator.remove();
                continue;
            }

            long now = level.getGameTime();
            if (now >= state.endTick) {
                finishState(server, state, level);
                iterator.remove();
                caster.displayClientMessage(Component.translatable(
                        "message.typemoonworld.zagan.ended"), true);
                continue;
            }

            if (now >= state.nextPulseTick) {
                pulse(caster, level, state, now);
                state.nextPulseTick = now + PULSE_INTERVAL_TICKS;
            }
            enforceControls(level, caster, state);
            processWaterPrison(level, state);
            enforceWaterBubble(level, state);
            if (now - state.lastVfxTick >= VFX_INTERVAL_TICKS) {
                state.lastVfxTick = now;
                VFXServerEffects.spawn(level, FLAT_EFFECT, state.anchor, VFX_OBSERVER_RADIUS);
                AddonSpellVisualPayload.showAt(
                        level, AddonSpellVisualPayload.ZAGAN,
                        AddonSpellVisualPayload.WATER_FIELD, state.casterId, state.anchor,
                        (float) EFFECT_RADIUS, 5.0F, VFX_INTERVAL_TICKS + 6,
                        0, VFX_OBSERVER_RADIUS);
                spawnWaterBubbleVfx(level, state);
            }
        }
    }

    public static void stop(Entity entity, boolean notify) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        CastState state = ACTIVE.remove(player.getUUID());
        if (state == null) {
            return;
        }
        MinecraftServer server = player.getServer();
        ServerLevel level = server == null ? player.serverLevel() : server.getLevel(state.dimension);
        finishState(server, state, level);
        if (notify) {
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.zagan.ended"), true);
        }
    }

    public static void handleEntityInvalidated(Entity entity) {
        if (entity == null) {
            return;
        }
        Iterator<Map.Entry<UUID, CastState>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            CastState state = iterator.next().getValue();
            if (!state.casterId.equals(entity.getUUID()) && !state.targetId.equals(entity.getUUID())) {
                continue;
            }
            MinecraftServer server = entity.getServer();
            if (server != null) {
                finishState(server, state, server.getLevel(state.dimension));
            }
            iterator.remove();
        }
    }

    public static void clearAll(MinecraftServer server) {
        for (CastState state : List.copyOf(ACTIVE.values())) {
            finishState(server, state, server.getLevel(state.dimension));
        }
        ACTIVE.clear();
    }

    public static void handleChunkUnload(ServerLevel level, ChunkPos chunkPos) {
        if (level == null || chunkPos == null) {
            return;
        }
        for (CastState state : ACTIVE.values()) {
            if (!state.dimension.equals(level.dimension()) || state.waterBubble == null) {
                continue;
            }
            restoreWaterPrisonInChunk(level, state.waterBubble, chunkPos);
        }
    }

    private static void pulse(ServerPlayer caster, ServerLevel level, CastState state, long now) {
        List<LivingEntity> targets = findTargets(caster, level, state.anchor);
        Set<UUID> current = new HashSet<>();
        DamageSource source = level.damageSources().source(DamageTypes.MAGIC, caster);
        for (LivingEntity target : targets) {
            current.add(target.getUUID());
            state.controls.computeIfAbsent(target.getUUID(), ignored ->
                    new ControlSnapshot(clampVelocity(target.getDeltaMovement())));

            float accumulated = state.accumulatedDamage.getOrDefault(target.getUUID(), 0.0F);
            boolean damaged = false;
            if (accumulated < TOTAL_DAMAGE_CAP && target.isAlive() && !target.isRemoved()) {
                float amount = (float) Math.min(
                        DAMAGE_PER_PULSE,
                        TOTAL_DAMAGE_CAP - accumulated
                );
                if (amount > 0.0F && target.hurt(source, amount)) {
                    state.accumulatedDamage.put(target.getUUID(),
                            Math.min((float) TOTAL_DAMAGE_CAP, accumulated + amount));
                    damaged = true;
                }
            }
            if (damaged
                && target.getUUID().equals(state.targetId)
                && state.waterBubble == null) {
                state.waterBubble = new WaterBubbleState(target.getBoundingBox().getCenter());
                queueWaterPrison(level, state, target);
                VFXServerEffects.spawn(level, WATER_BUBBLE_EFFECT, target, VFX_OBSERVER_RADIUS);
                AddonSpellVisualPayload.showAttached(
                        level, AddonSpellVisualPayload.ZAGAN,
                        AddonSpellVisualPayload.WATER_PRISON, state.casterId, target,
                        (float) WATER_BUBBLE_RADIUS, 1.0F, EFFECT_DURATION_TICKS,
                        target.getId(), VFX_OBSERVER_RADIUS);
            }
        }
        state.activeTargets.clear();
        state.activeTargets.addAll(current);
        VFXServerEffects.spawn(level, PULSE_EFFECT, state.anchor, VFX_OBSERVER_RADIUS);
        level.sendParticles(ParticleTypes.SPLASH, state.anchor.x, state.anchor.y,
                state.anchor.z, Math.min(40, 18 + targets.size() / 2),
                EFFECT_RADIUS * 0.65D, 0.55D, EFFECT_RADIUS * 0.65D, 0.2D);
    }

    private static List<LivingEntity> findTargets(
            ServerPlayer caster,
            ServerLevel level,
            Vec3 center
    ) {
        AABB search = new AABB(center, center).inflate(EFFECT_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                search,
                target -> target != caster
                        && target.isAlive()
                        && !target.isRemoved()
                        && EntityUtils.isValidCombatTarget(caster, target)
                        && target.getBoundingBox().getCenter().distanceToSqr(center)
                        <= EFFECT_RADIUS_SQUARED
        );
        targets.sort(Comparator.comparingDouble(
                target -> target.getBoundingBox().getCenter().distanceToSqr(center)));
        if (targets.size() > MAX_TARGETS) {
            return new ArrayList<>(targets.subList(0, MAX_TARGETS));
        }
        return targets;
    }

    private static void enforceControls(ServerLevel level, ServerPlayer caster, CastState state) {
        for (UUID id : List.copyOf(state.controls.keySet())) {
            Entity entity = level.getEntity(id);
            if (!(entity instanceof LivingEntity target)
                    || !target.isAlive()
                    || target.isRemoved()
                    || !state.activeTargets.contains(id)
                    || !EntityUtils.isValidCombatTarget(caster, target)) {
                restoreControl(level, state.controls.remove(id), entity);
                continue;
            }
            applyControl(level, target, state.anchor);
        }
    }

    private static void enforceWaterBubble(ServerLevel level, CastState state) {
        WaterBubbleState bubble = state.waterBubble;
        if (bubble == null) {
            return;
        }
        Entity entity = level.getEntity(state.targetId);
        if (!(entity instanceof LivingEntity target)
                || !target.isAlive()
                || target.isRemoved()) {
            state.waterBubble = null;
            return;
        }
        if (target instanceof EnderDragon || target instanceof WitherBoss) {
            return;
        }

        Vec3 body = target.getBoundingBox().getCenter();
        Vec3 offset = body.subtract(bubble.center);
        double distance = offset.length();
        if (!Double.isFinite(distance)
                || distance * distance <= WATER_BUBBLE_RADIUS_SQUARED) {
            if (target instanceof Mob mob) {
                mob.getNavigation().stop();
            }
            return;
        }

        Vec3 inward = distance > 1.0E-4D
                ? offset.scale(-1.0D / distance)
                : Vec3.ZERO;
        Vec3 current = target.getDeltaMovement();
        double outwardSpeed = current.dot(offset.scale(1.0D / distance));
        Vec3 velocity = current;
        if (outwardSpeed > 0.0D) {
            velocity = velocity.subtract(offset.scale(outwardSpeed / distance));
        }
        velocity = velocity.add(inward.scale(0.22D));

        double allowedDistance = Math.max(0.5D, WATER_BUBBLE_RADIUS - 0.35D);
        Vec3 clampedBody = bubble.center.add(offset.scale(allowedDistance / distance));
        double y = clampedBody.y - target.getBbHeight() * 0.5D;
        target.setPos(clampedBody.x, y, clampedBody.z);
        target.setDeltaMovement(
                Mth.clamp(velocity.x, -0.65D, 0.65D),
                Mth.clamp(velocity.y, -0.35D, 0.35D),
                Mth.clamp(velocity.z, -0.65D, 0.65D));
        target.hasImpulse = true;
        target.hurtMarked = true;
        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
        }
    }

    private static void spawnWaterBubbleVfx(ServerLevel level, CastState state) {
        if (state.waterBubble == null) {
            return;
        }
        Entity target = level.getEntity(state.targetId);
        if (target instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
            VFXServerEffects.spawn(level, WATER_BUBBLE_EFFECT, living, VFX_OBSERVER_RADIUS);
            AddonSpellVisualPayload.showAttached(
                    level, AddonSpellVisualPayload.ZAGAN,
                    AddonSpellVisualPayload.WATER_PRISON, state.casterId, living,
                    (float) WATER_BUBBLE_RADIUS, 1.0F, VFX_INTERVAL_TICKS + 6,
                    living.getId(), VFX_OBSERVER_RADIUS);
        }
    }

    private static void queueWaterPrison(ServerLevel level, CastState state, LivingEntity target) {
        WaterBubbleState bubble = state.waterBubble;
        ServerPlayer caster = level.getServer().getPlayerList().getPlayer(state.casterId);
        if (bubble == null
                || caster == null
                || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        BlockPos center = BlockPos.containing(bubble.center);
        int radius = Mth.ceil(WATER_BUBBLE_RADIUS);
        double radiusSquared = WATER_BUBBLE_RADIUS * WATER_BUBBLE_RADIUS;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * (double) x + y * (double) y + z * (double) z > radiusSquared) {
                        continue;
                    }
                    BlockPos pos = center.offset(x, y, z);
                    if (!level.hasChunkAt(pos)
                            || !level.mayInteract(caster, pos)
                            || level.getBlockEntity(pos) != null
                            || new AABB(pos).intersects(target.getBoundingBox())) {
                        continue;
                    }
                    BlockState current = level.getBlockState(pos);
                    if (!isWaterPrisonReplaceable(current)
                            || !bubble.queuedPositions.add(pos.asLong())) {
                        continue;
                    }
                    bubble.placementQueue.addLast(pos.immutable());
                }
            }
        }
    }

    private static boolean isWaterPrisonReplaceable(BlockState state) {
        return state.isAir()
                || (state.canBeReplaced() && state.getFluidState().isEmpty());
    }

    private static void processWaterPrison(ServerLevel level, CastState state) {
        WaterBubbleState bubble = state.waterBubble;
        ServerPlayer caster = level.getServer().getPlayerList().getPlayer(state.casterId);
        if (bubble == null
                || caster == null
                || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            if (bubble != null) {
                bubble.placementQueue.clear();
            }
            return;
        }
        int processed = 0;
        while (processed < WATER_PRISON_BLOCKS_PER_TICK && !bubble.placementQueue.isEmpty()) {
            BlockPos pos = bubble.placementQueue.removeFirst();
            processed++;
            if (!level.hasChunkAt(pos)
                    || !level.mayInteract(caster, pos)
                    || level.getBlockEntity(pos) != null
                    || bubble.replacedBlocks.size() >= MAX_WATER_PRISON_BLOCKS) {
                continue;
            }
            BlockState current = level.getBlockState(pos);
            if (!isWaterPrisonReplaceable(current)) {
                continue;
            }
            BlockState water = Blocks.WATER.defaultBlockState();
            if (!level.setBlock(pos, water, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)) {
                continue;
            }
            bubble.replacedBlocks.put(pos.immutable(), new WaterPrisonBlock(current, water));
        }
    }

    private static void restoreWaterPrison(ServerLevel level, WaterBubbleState bubble) {
        if (level == null || bubble == null) {
            return;
        }
        for (Map.Entry<BlockPos, WaterPrisonBlock> entry : List.copyOf(bubble.replacedBlocks.entrySet())) {
            BlockPos pos = entry.getKey();
            WaterPrisonBlock block = entry.getValue();
            if (level.hasChunkAt(pos) && level.getBlockState(pos).equals(block.placedState())) {
                level.setBlock(pos, block.originalState(), Block.UPDATE_ALL);
            }
            bubble.replacedBlocks.remove(pos);
        }
        bubble.placementQueue.clear();
        bubble.queuedPositions.clear();
    }

    private static void restoreWaterPrisonInChunk(
            ServerLevel level,
            WaterBubbleState bubble,
            ChunkPos chunkPos
    ) {
        for (Map.Entry<BlockPos, WaterPrisonBlock> entry : List.copyOf(bubble.replacedBlocks.entrySet())) {
            BlockPos pos = entry.getKey();
            if ((pos.getX() >> 4) != chunkPos.x || (pos.getZ() >> 4) != chunkPos.z) {
                continue;
            }
            WaterPrisonBlock block = entry.getValue();
            if (level.hasChunkAt(pos) && level.getBlockState(pos).equals(block.placedState())) {
                level.setBlock(pos, block.originalState(), Block.UPDATE_ALL);
            }
            bubble.replacedBlocks.remove(pos);
        }
        bubble.placementQueue.removeIf(pos ->
                (pos.getX() >> 4) == chunkPos.x && (pos.getZ() >> 4) == chunkPos.z);
    }

    private static void applyControl(ServerLevel level, LivingEntity target, Vec3 center) {
        if (target instanceof EnderDragon || target instanceof WitherBoss) {
            return;
        }
        double knockbackResistance = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        if (!Double.isFinite(knockbackResistance) || knockbackResistance >= 1.0D) {
            return;
        }
        double resistanceMultiplier = Mth.clamp(1.0D - knockbackResistance, 0.0D, 1.0D);
        Vec3 body = target.getBoundingBox().getCenter();
        Vec3 horizontal = new Vec3(center.x - body.x, 0.0D, center.z - body.z);
        double distance = horizontal.length();
        if (distance > EFFECT_RADIUS) {
            return;
        }
        double factor = Mth.clamp(1.0D - distance / EFFECT_RADIUS, 0.0D, 1.0D);
        Vec3 inward;
        if (distance > 1.0E-4D) {
            inward = horizontal.scale(1.0D / distance);
        } else {
            double phase = (target.getId() & 31) * 0.1963495408D;
            inward = new Vec3(Math.cos(phase), 0.0D, Math.sin(phase));
        }
        Vec3 tangent = new Vec3(-inward.z, 0.0D, inward.x);
        double blockedMultiplier = isBlocked(level, target, center) ? 0.28D : 1.0D;
        double force = (0.085D + 0.18D * factor) * blockedMultiplier * resistanceMultiplier;
        double swirl = (0.07D + 0.2D * factor) * blockedMultiplier * resistanceMultiplier;
        double entityMultiplier = target instanceof ServerPlayer ? PLAYER_FORCE_MULTIPLIER : 1.0D;
        Vec3 current = target.getDeltaMovement();
        Vec3 horizontalVelocity = new Vec3(current.x, 0.0D, current.z)
                .scale(0.28D)
                .add(inward.scale(force * entityMultiplier))
                .add(tangent.scale(swirl * entityMultiplier));
        double speed = horizontalVelocity.length();
        if (speed > MAX_HORIZONTAL_SPEED * entityMultiplier) {
            horizontalVelocity = horizontalVelocity.normalize()
                    .scale(MAX_HORIZONTAL_SPEED * entityMultiplier);
        }
        double vertical = Mth.clamp(current.y * 0.25D + 0.045D * factor,
                CONTROL_VERTICAL_MIN, CONTROL_VERTICAL_MAX);
        target.setDeltaMovement(horizontalVelocity.x, vertical, horizontalVelocity.z);
        target.hasImpulse = true;
        target.hurtMarked = true;
        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
        }
    }

    private static boolean isBlocked(ServerLevel level, LivingEntity target, Vec3 center) {
        Vec3 start = target.getEyePosition();
        HitResult hit = level.clip(new ClipContext(
                start,
                center,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                target
        ));
        return hit.getType() == HitResult.Type.BLOCK
                && hit.getLocation().distanceToSqr(start) + 0.25D < start.distanceToSqr(center);
    }

    private static boolean isTargetStillValid(ServerLevel level, UUID targetId) {
        Entity entity = level.getEntity(targetId);
        return entity instanceof LivingEntity living
                && living.isAlive()
                && !living.isRemoved();
    }

    private static boolean isValidCaster(ServerPlayer player) {
        return player != null
                && player.isAlive()
                && !player.isRemoved()
                && !player.isSpectator();
    }

    private static void finishState(MinecraftServer server, CastState state, ServerLevel level) {
        if (level != null) {
            for (Map.Entry<UUID, ControlSnapshot> entry : state.controls.entrySet()) {
                restoreControl(level, entry.getValue(), level.getEntity(entry.getKey()));
            }
            restoreWaterPrison(level, state.waterBubble);
            VFXServerEffects.spawn(level, END_EFFECT, state.anchor, VFX_OBSERVER_RADIUS);
            ServerPlayer caster = server == null
                    ? null : server.getPlayerList().getPlayer(state.casterId);
            if (caster != null) {
                AddonSpellVisualPayload.clear(
                        caster, AddonSpellVisualPayload.ZAGAN, VFX_OBSERVER_RADIUS);
            }
            AddonSpellVisualPayload.showAt(
                    level, AddonSpellVisualPayload.ZAGAN,
                    AddonSpellVisualPayload.AFTERMATH, state.casterId, state.anchor,
                    (float) EFFECT_RADIUS, 1.0F, 28, 0, VFX_OBSERVER_RADIUS);
        }
        state.controls.clear();
        state.activeTargets.clear();
        state.accumulatedDamage.clear();
        state.waterBubble = null;
    }

    private static void restoreControl(ServerLevel level, ControlSnapshot snapshot, Entity entity) {
        if (!(entity instanceof LivingEntity living) || snapshot == null || living.isRemoved()) {
            return;
        }
        living.setDeltaMovement(clampVelocity(snapshot.velocity));
        living.hasImpulse = true;
        living.hurtMarked = true;
        if (living instanceof Mob mob) {
            mob.getNavigation().stop();
        }
    }

    private static Vec3 clampVelocity(Vec3 velocity) {
        if (velocity == null
                || !Double.isFinite(velocity.x)
                || !Double.isFinite(velocity.y)
                || !Double.isFinite(velocity.z)) {
            return Vec3.ZERO;
        }
        if (velocity.lengthSqr() > MAX_RESTORED_SPEED * MAX_RESTORED_SPEED) {
            return velocity.normalize().scale(MAX_RESTORED_SPEED);
        }
        return velocity;
    }

    private static final class CastState {
        private final UUID casterId;
        private final UUID targetId;
        private final ResourceKey<Level> dimension;
        private final Vec3 anchor;
        private final long endTick;
        private final Map<UUID, Float> accumulatedDamage = new HashMap<>();
        private final Map<UUID, ControlSnapshot> controls = new HashMap<>();
        private final Set<UUID> activeTargets = new HashSet<>();
        private WaterBubbleState waterBubble;
        private long nextPulseTick;
        private long lastVfxTick;

        private CastState(
                UUID casterId,
                UUID targetId,
                ResourceKey<Level> dimension,
                Vec3 anchor,
                long startTick,
                long endTick
        ) {
            this.casterId = casterId;
            this.targetId = targetId;
            this.dimension = dimension;
            this.anchor = anchor;
            this.endTick = endTick;
            this.nextPulseTick = startTick;
            this.lastVfxTick = startTick;
        }
    }

    private record ControlSnapshot(Vec3 velocity) {
    }

    private static final class WaterBubbleState {
        private final Vec3 center;
        private final ArrayDeque<BlockPos> placementQueue = new ArrayDeque<>();
        private final Set<Long> queuedPositions = new HashSet<>();
        private final Map<BlockPos, WaterPrisonBlock> replacedBlocks = new LinkedHashMap<>();

        private WaterBubbleState(Vec3 center) {
            this.center = center;
        }
    }

    private record WaterPrisonBlock(BlockState originalState, BlockState placedState) {
    }
}
