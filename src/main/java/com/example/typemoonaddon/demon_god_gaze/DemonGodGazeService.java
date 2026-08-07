package com.example.typemoonaddon.demon_god_gaze;

import com.example.typemoonaddon.network.DemonGodGazeVisualPayload;
import com.example.typemoonaddon.network.DemonGodGazeVisualPayload.TargetLayout;
import com.example.typemoonaddon.registry.AddonSounds;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

/** Server-owned lock, indexed attack sequence, and bounded terrain removal. */
public final class DemonGodGazeService {
    public static final double LOCK_RADIUS = 50.0D;
    public static final double LOCK_RADIUS_SQR = LOCK_RADIUS * LOCK_RADIUS;
    public static final int MAX_LOCKED_TARGETS = 11;
    public static final int MAGIC_CIRCLE_COUNT = 11;
    public static final float DAMAGE_PER_CIRCLE = 10.0F;
    public static final float THEORETICAL_TOTAL_DAMAGE = MAGIC_CIRCLE_COUNT * DAMAGE_PER_CIRCLE;
    public static final int CIRCLE_DEPLOY_TICKS = 12;
    /** The bundled streamed cue is 2.707 seconds long (about 54 server ticks). */
    public static final double ATTACK_CUE_DURATION_SECONDS = 2.707D;
    public static final int ATTACK_CUE_DURATION_TICKS = (int) Math.round(ATTACK_CUE_DURATION_SECONDS * 20.0D);
    /** Spread the eleven shots across the complete replacement cue. */
    public static final int ATTACK_INTERVAL_TICKS = 10;
    public static final double SHOT_HIT_RADIUS = 2.0D;
    /** Large final bloom radius used by both the visual envelope and its bounded terrain queue. */
    public static final double FINAL_VISUAL_RADIUS = 18.0D;
    /** Vanilla TNT uses power 4.0; the final convergence gets one independent explosion. */
    public static final float FINAL_TNT_EXPLOSION_POWER = 4.0F;
    public static final int MAX_TERRAIN_BLOCKS = 300;
    public static final int MAX_BLOCKS_PER_TICK = 30;

    private static final double CIRCLE_DISTANCE = 4.8D;
    private static final double MINOR_TERRAIN_RADIUS = 1.8D;
    private static final double FINAL_TERRAIN_RADIUS = FINAL_VISUAL_RADIUS;
    private static final int MINOR_TERRAIN_BLOCKS_PER_SHOT = 5;
    private static final int CAST_DEBOUNCE_TICKS = 2;
    private static final double VFX_OBSERVER_RADIUS = 192.0D;

    private static final String LOCK_EFFECT = "typemoonworld:demon_god_gaze_lock";
    private static final String SHOT_EFFECT = "typemoonworld:demon_god_gaze_shot";
    private static final String HIT_EFFECT = "typemoonworld:demon_god_gaze_hit";
    private static final String FINAL_EFFECT = "typemoonworld:demon_god_gaze_final_explosion";

    private static final Map<UUID, CastState> ACTIVE_CASTS = new HashMap<>();
    private static final Map<UUID, Long> LAST_SUCCESSFUL_CAST_TICK = new HashMap<>();

    private DemonGodGazeService() {
    }

    public static CastOutcome cast(ServerPlayer caster) {
        if (!isValidCaster(caster) || ACTIVE_CASTS.containsKey(caster.getUUID())) {
            return CastOutcome.rejected();
        }

        ServerLevel level = caster.serverLevel();
        long now = level.getGameTime();
        Long lastCast = LAST_SUCCESSFUL_CAST_TICK.get(caster.getUUID());
        if (lastCast != null && now >= lastCast && now - lastCast < CAST_DEBOUNCE_TICKS) {
            return CastOutcome.rejected();
        }

        List<LivingEntity> targets = findTargets(caster);
        if (targets.isEmpty()) {
            return CastOutcome.noTarget();
        }

        UUID castId = UUID.randomUUID();
        List<TargetState> targetStates = new ArrayList<>(targets.size());
        for (LivingEntity target : targets) {
            targetStates.add(new TargetState(
                    target.getUUID(),
                    createCircleOffsets(caster, target, castId),
                    now + CIRCLE_DEPLOY_TICKS
            ));
        }
        CastState state = new CastState(
                castId,
                caster.getUUID(),
                level.dimension(),
                caster.getBoundingBox().getCenter(),
                targetStates
        );
        ACTIVE_CASTS.put(caster.getUUID(), state);
        LAST_SUCCESSFUL_CAST_TICK.put(caster.getUUID(), now);

        // One streamed cue spans circle deployment and the complete multi-shot sequence.
        level.playSound(null, caster.blockPosition(), AddonSounds.DEMON_GOD_GAZE.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);

        List<TargetLayout> visualTargets = new ArrayList<>(targets.size());
        for (int targetIndex = 0; targetIndex < targets.size(); targetIndex++) {
            LivingEntity target = targets.get(targetIndex);
            TargetState targetState = targetStates.get(targetIndex);
            Vec3 targetCenter = target.getBoundingBox().getCenter();
            VFXServerEffects.spawn(level, LOCK_EFFECT, targetCenter, VFX_OBSERVER_RADIUS);
            visualTargets.add(new TargetLayout(target.getId(), targetCenter, targetState.circleOffsets));
        }
        broadcastVisual(level, caster.getBoundingBox().getCenter(),
                DemonGodGazeVisualPayload.start(castId, visualTargets));
        return CastOutcome.success();
    }

    public static void tick(MinecraftServer server) {
        if (server == null || ACTIVE_CASTS.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, CastState>> iterator = ACTIVE_CASTS.entrySet().iterator();
        while (iterator.hasNext()) {
            CastState state = iterator.next().getValue();
            if (tickState(server, state)) {
                iterator.remove();
            }
        }
    }

    public static void stop(LivingEntity entity) {
        if (entity != null) {
            CastState removed = ACTIVE_CASTS.remove(entity.getUUID());
            LAST_SUCCESSFUL_CAST_TICK.remove(entity.getUUID());
            if (removed != null && entity instanceof ServerPlayer player && player.getServer() != null) {
                ServerLevel level = player.getServer().getLevel(removed.dimension);
                if (level != null) {
                    broadcastVisual(level, removed.visualOrigin,
                            DemonGodGazeVisualPayload.clear(removed.castId));
                }
            }
        }
    }

    public static void clearAll() {
        ACTIVE_CASTS.clear();
        LAST_SUCCESSFUL_CAST_TICK.clear();
    }

    private static boolean tickState(MinecraftServer server, CastState state) {
        ServerLevel level = server.getLevel(state.dimension);
        ServerPlayer caster = server.getPlayerList().getPlayer(state.casterId);
        if (level == null
                || !isValidCaster(caster)
                || caster.serverLevel() != level) {
            if (level != null) {
                broadcastVisual(level, state.visualOrigin,
                        DemonGodGazeVisualPayload.clear(state.castId));
            }
            return true;
        }

        long now = level.getGameTime();
        boolean anyActiveTargets = false;
        for (TargetState targetState : state.targets) {
            if (targetState.attacksFinished) {
                continue;
            }
            LivingEntity target = resolveTarget(level, caster, targetState.targetId);
            if (target == null) {
                targetState.attacksFinished = true;
                continue;
            }
            anyActiveTargets = true;
            if (now >= targetState.nextAttackTick) {
                fireNextCircle(caster, level, target, state, targetState);
            }
        }
        processTerrain(level, state);

        if (!anyActiveTargets && state.terrainQueue.isEmpty()) {
            caster.displayClientMessage(Component.translatable(
                    "message.typemoonworld.demon_god_gaze.ended"), true);
            return true;
        }
        return false;
    }

    private static void fireNextCircle(
            ServerPlayer caster,
            ServerLevel level,
            LivingEntity target,
            CastState state,
            TargetState targetState
    ) {
        int circleIndex = targetState.nextCircleIndex;
        if (circleIndex < 0 || circleIndex >= MAGIC_CIRCLE_COUNT) {
            targetState.attacksFinished = true;
            return;
        }
        int bit = 1 << circleIndex;
        targetState.nextCircleIndex++;
        if ((targetState.firedMask & bit) != 0) {
            return;
        }
        targetState.firedMask |= bit;

        if (targetState.nextCircleIndex < MAGIC_CIRCLE_COUNT) {
            targetState.nextAttackTick = scheduledAttackTick(
                    targetState.attackStartTick,
                    targetState.nextCircleIndex
            );
        }

        Vec3 targetCenter = target.getBoundingBox().getCenter();
        Vec3 circlePosition = targetCenter.add(targetState.circleOffsets.get(circleIndex));
        Vec3 shotDirection = targetCenter.subtract(circlePosition);
        VFXServerEffects.spawnOriented(
                level, SHOT_EFFECT, circlePosition, shotDirection, VFX_OBSERVER_RADIUS);
        VFXServerEffects.spawn(level, HIT_EFFECT, targetCenter, VFX_OBSERVER_RADIUS);
        broadcastVisual(level, targetCenter, DemonGodGazeVisualPayload.fire(
                state.castId, target.getId(), circleIndex, circlePosition, targetCenter));

        AABB hitArea = new AABB(targetCenter, targetCenter).inflate(SHOT_HIT_RADIUS);
        if (target.getBoundingBox().intersects(hitArea)
                && (targetState.damageAppliedMask & bit) == 0) {
            DamageSource source = caster.damageSources().source(DamageTypes.MAGIC, caster);
            int previousInvulnerability = target.invulnerableTime;
            target.invulnerableTime = 0;
            boolean hurt = target.hurt(source, DAMAGE_PER_CIRCLE);
            if (hurt) {
                targetState.damageAppliedMask |= bit;
                target.invulnerableTime = 0;
                EntityUtils.triggerSwarmAnger(level, caster, target);
            } else {
                target.invulnerableTime = previousInvulnerability;
            }
        }

        if (circleIndex < MAGIC_CIRCLE_COUNT - 1) {
            queueTerrainSphere(
                    level,
                    state,
                    target.position(),
                    MINOR_TERRAIN_RADIUS,
                    MINOR_TERRAIN_BLOCKS_PER_SHOT
            );
            return;
        }

        targetState.attacksFinished = true;
        VFXServerEffects.spawn(level, FINAL_EFFECT, targetCenter, VFX_OBSERVER_RADIUS);
        VFXServerEffects.screenFlash(level, targetCenter, 128.0D, 8, 0.62F);
        createFinalTntExplosion(level, caster, targetCenter);
        queueTerrainSphere(
                level,
                state,
                target.position(),
                FINAL_TERRAIN_RADIUS,
                MAX_TERRAIN_BLOCKS - state.terrainSeen.size()
        );
    }

    /**
     * Resolve the final hit through the normal TNT explosion path.  This keeps
     * explosion damage separate from the eleven magic-circle damage entries and
     * gives the blast vanilla TNT block interaction/drop-decay semantics.
     */
    private static void createFinalTntExplosion(
            ServerLevel level,
            ServerPlayer caster,
            Vec3 center
    ) {
        DamageSource source = Explosion.getDefaultDamageSource(level, caster);
        level.explode(
                caster,
                source,
                null,
                center.x,
                center.y,
                center.z,
                FINAL_TNT_EXPLOSION_POWER,
                false,
                Level.ExplosionInteraction.TNT
        );
    }

    private static long scheduledAttackTick(long attackStartTick, int circleIndex) {
        if (MAGIC_CIRCLE_COUNT <= 1) {
            return attackStartTick;
        }
        long sequenceWindow = Math.max(0L, ATTACK_CUE_DURATION_TICKS - CIRCLE_DEPLOY_TICKS);
        double progress = circleIndex / (double) (MAGIC_CIRCLE_COUNT - 1);
        return attackStartTick + Math.round(sequenceWindow * progress);
    }

    private static void broadcastVisual(
            ServerLevel level,
            Vec3 center,
            DemonGodGazeVisualPayload payload
    ) {
        PacketDistributor.sendToPlayersNear(
                level,
                null,
                center.x,
                center.y,
                center.z,
                VFX_OBSERVER_RADIUS,
                payload
        );
    }

    private static List<LivingEntity> findTargets(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        Vec3 center = caster.getBoundingBox().getCenter();
        AABB query = new AABB(center, center).inflate(LOCK_RADIUS);
        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                query,
                candidate -> isEligibleTarget(caster, candidate, center));
        candidates.sort(Comparator
                .comparingDouble((LivingEntity target) ->
                        target.getBoundingBox().getCenter().distanceToSqr(center))
                .thenComparingInt(LivingEntity::getId));
        if (candidates.size() > MAX_LOCKED_TARGETS) {
            return new ArrayList<>(candidates.subList(0, MAX_LOCKED_TARGETS));
        }
        return candidates;
    }

    private static boolean isEligibleTarget(
            ServerPlayer caster,
            LivingEntity target,
            Vec3 sphereCenter
    ) {
        return target != caster
                && target.isAlive()
                && !target.isRemoved()
                && !(target instanceof ArmorStand)
                && target.level() == caster.level()
                && target.getBoundingBox().getCenter().distanceToSqr(sphereCenter) <= LOCK_RADIUS_SQR;
    }

    private static LivingEntity resolveTarget(
            ServerLevel level,
            ServerPlayer caster,
            UUID targetId
    ) {
        if (level.getEntity(targetId) instanceof LivingEntity target
                && target != caster
                && target.isAlive()
                && !target.isRemoved()
                && !(target instanceof ArmorStand)
                && target.level() == level) {
            return target;
        }
        return null;
    }

    private static List<Vec3> createCircleOffsets(
            ServerPlayer caster,
            LivingEntity target,
            UUID castId
    ) {
        List<Vec3> offsets = new ArrayList<>(MAGIC_CIRCLE_COUNT);
        double seedAngle = Math.floorMod(castId.getLeastSignificantBits(), 4096L)
                / 4096.0D * Math.PI * 2.0D;
        for (int index = 0; index < 8; index++) {
            double angle = seedAngle + index * Math.PI * 2.0D / 8.0D;
            double vertical = Math.sin(angle * 2.0D) * 0.72D;
            double horizontal = Math.sqrt(Math.max(0.0D,
                    CIRCLE_DISTANCE * CIRCLE_DISTANCE - vertical * vertical));
            offsets.add(new Vec3(
                    Math.cos(angle) * horizontal,
                    vertical,
                    Math.sin(angle) * horizontal
            ));
        }

        double upperVertical = 3.25D;
        double upperHorizontal = Math.sqrt(
                CIRCLE_DISTANCE * CIRCLE_DISTANCE - upperVertical * upperVertical);
        for (int index = 0; index < 2; index++) {
            double angle = seedAngle + Math.PI * (0.36D + index);
            offsets.add(new Vec3(
                    Math.cos(angle) * upperHorizontal,
                    upperVertical,
                    Math.sin(angle) * upperHorizontal
            ));
        }

        Vec3 toCaster = caster.getBoundingBox().getCenter()
                .subtract(target.getBoundingBox().getCenter());
        Vec3 horizontalToCaster = new Vec3(toCaster.x, 0.0D, toCaster.z);
        if (horizontalToCaster.lengthSqr() < 1.0E-6D) {
            horizontalToCaster = new Vec3(Math.cos(seedAngle), 0.0D, Math.sin(seedAngle));
        } else {
            horizontalToCaster = horizontalToCaster.normalize();
        }
        double coreVertical = -2.35D;
        double coreHorizontal = Math.sqrt(
                CIRCLE_DISTANCE * CIRCLE_DISTANCE - coreVertical * coreVertical);
        offsets.add(horizontalToCaster.scale(coreHorizontal).add(0.0D, coreVertical, 0.0D));
        return List.copyOf(offsets);
    }

    private static void queueTerrainSphere(
            ServerLevel level,
            CastState state,
            Vec3 center,
            double radius,
            int requestedBudget
    ) {
        if (requestedBudget <= 0
                || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                || state.terrainSeen.size() >= MAX_TERRAIN_BLOCKS) {
            return;
        }

        int radiusCeil = Mth.ceil(Mth.clamp(radius, 0.0D, FINAL_TERRAIN_RADIUS));
        BlockPos origin = BlockPos.containing(center);
        List<BlockCandidate> candidates = new ArrayList<>();
        double radiusSquared = radius * radius;
        for (int x = -radiusCeil; x <= radiusCeil; x++) {
            for (int y = -radiusCeil; y <= radiusCeil; y++) {
                for (int z = -radiusCeil; z <= radiusCeil; z++) {
                    double distanceSquared = x * x + y * y + z * z;
                    if (distanceSquared > radiusSquared) {
                        continue;
                    }
                    BlockPos pos = origin.offset(x, y, z).immutable();
                    if (!state.terrainSeen.contains(pos) && isRemovableTerrain(level, pos)) {
                        candidates.add(new BlockCandidate(pos, distanceSquared));
                    }
                }
            }
        }
        candidates.sort(Comparator.comparingDouble(BlockCandidate::distanceSquared));

        int available = Math.min(
                Math.max(0, requestedBudget),
                MAX_TERRAIN_BLOCKS - state.terrainSeen.size());
        for (BlockCandidate candidate : candidates) {
            if (available <= 0) {
                break;
            }
            if (state.terrainSeen.add(candidate.pos())) {
                state.terrainQueue.addLast(candidate.pos());
                available--;
            }
        }
    }

    private static void processTerrain(ServerLevel level, CastState state) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            state.terrainQueue.clear();
            return;
        }
        int processed = 0;
        while (processed < MAX_BLOCKS_PER_TICK
                && state.removedBlocks < MAX_TERRAIN_BLOCKS
                && !state.terrainQueue.isEmpty()) {
            BlockPos pos = state.terrainQueue.removeFirst();
            processed++;
            if (!level.hasChunkAt(pos)) {
                state.terrainQueue.clear();
                return;
            }
            if (!isRemovableTerrain(level, pos)) {
                continue;
            }
            if (level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)) {
                state.removedBlocks++;
            }
        }
        if (state.removedBlocks >= MAX_TERRAIN_BLOCKS) {
            state.terrainQueue.clear();
        }
    }

    private static boolean isRemovableTerrain(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || level.getBlockEntity(pos) != null) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir()
                || !level.getFluidState(pos).isEmpty()
                || state.is(Blocks.BEDROCK)
                || state.is(BlockTags.PORTALS)
                || state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        return state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(BlockTags.TERRACOTTA)
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.SNOW)
                || state.is(BlockTags.ICE)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.MUD)
                || state.is(Blocks.PACKED_MUD)
                || state.is(Blocks.END_STONE);
    }

    private static boolean isValidCaster(ServerPlayer caster) {
        return caster != null
                && caster.isAlive()
                && !caster.isRemoved()
                && !caster.isSpectator()
                && !caster.hasDisconnected();
    }

    public enum CastStatus {
        SUCCESS,
        NO_TARGET,
        REJECTED
    }

    public record CastOutcome(CastStatus status) {
        private static CastOutcome success() {
            return new CastOutcome(CastStatus.SUCCESS);
        }

        private static CastOutcome noTarget() {
            return new CastOutcome(CastStatus.NO_TARGET);
        }

        private static CastOutcome rejected() {
            return new CastOutcome(CastStatus.REJECTED);
        }
    }

    private record BlockCandidate(BlockPos pos, double distanceSquared) {
    }

    private static final class CastState {
        private final UUID castId;
        private final UUID casterId;
        private final ResourceKey<Level> dimension;
        private final Vec3 visualOrigin;
        private final List<TargetState> targets;
        private final ArrayDeque<BlockPos> terrainQueue = new ArrayDeque<>();
        private final Set<BlockPos> terrainSeen = new HashSet<>();
        private int removedBlocks;

        private CastState(
                UUID castId,
                UUID casterId,
                ResourceKey<Level> dimension,
                Vec3 visualOrigin,
                List<TargetState> targets
        ) {
            this.castId = castId;
            this.casterId = casterId;
            this.dimension = dimension;
            this.visualOrigin = visualOrigin;
            this.targets = List.copyOf(targets);
        }
    }

    private static final class TargetState {
        private final UUID targetId;
        private final List<Vec3> circleOffsets;
        private final long attackStartTick;
        private long nextAttackTick;
        private int nextCircleIndex;
        private int firedMask;
        private int damageAppliedMask;
        private boolean attacksFinished;

        private TargetState(UUID targetId, List<Vec3> circleOffsets, long nextAttackTick) {
            this.targetId = targetId;
            this.circleOffsets = List.copyOf(circleOffsets);
            this.attackStartTick = nextAttackTick;
            this.nextAttackTick = nextAttackTick;
        }
    }
}
