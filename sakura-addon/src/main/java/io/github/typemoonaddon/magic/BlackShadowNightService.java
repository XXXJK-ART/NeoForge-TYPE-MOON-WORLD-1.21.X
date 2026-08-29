package io.github.typemoonaddon.magic;

import io.github.typemoonaddon.data.PollutionData;
import io.github.typemoonaddon.entity.BlackShadowEntity;
import io.github.typemoonaddon.network.NightShadowCameraPayload;
import io.github.typemoonaddon.registry.ModAttachments;
import io.github.typemoonaddon.registry.ModEntities;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Runs the grail user's autonomous black-shadow hunt while the owner observes through it. */
public final class BlackShadowNightService {
    public static final double INITIAL_TARGET_SCAN_RADIUS = 1000.0D;
    public static final double TARGET_TELEPORT_MIN_RADIUS = 30.0D;
    public static final double TARGET_TELEPORT_MAX_RADIUS = 50.0D;
    private static final double RETRY_TELEPORT_MIN_RADIUS = 20.0D;
    private static final double RETRY_TELEPORT_MAX_RADIUS = 30.0D;

    private static final int SAFE_POSITION_ATTEMPTS = 64;
    private static final double RELOCATION_DISTANCE = 65.0D;
    private static final int RELOCATION_RETRY_TICKS = 40;
    private static final int MAX_RELOCATION_FAILURES = 5;
    private static final int MAX_MISSION_DURATION_TICKS = 20 * 60 * 5;
    private static final int RETURN_DELAY_TICKS = 20;
    private static final int PENDING_SLEEP_TIMEOUT_TICKS = 40;
    private static final int CAMERA_SYNC_INTERVAL_TICKS = 20;

    private static final Map<UUID, PendingSleep> PENDING_SLEEPS = new HashMap<>();
    private static final Map<UUID, NightMission> MISSIONS_BY_OWNER = new HashMap<>();
    private static final Map<UUID, UUID> OWNER_BY_SHADOW = new HashMap<>();
    private static final Set<UUID> INTERNAL_SLEEP_TRANSITIONS = new HashSet<>();

    public static void playerAttemptingSleep(ServerPlayer player, BlockPos bedPos) {
        UUID ownerId = player.getUUID();
        var imaginarySpace = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        if (INTERNAL_SLEEP_TRANSITIONS.contains(ownerId)
            || !imaginarySpace.grailWormAscended()
            || imaginarySpace.grailErosionFull()
            || imaginarySpace.grailErosionPending()
            || imaginarySpace.completedNightMissionOn(dayIndex(player.serverLevel()))
            || MISSIONS_BY_OWNER.containsKey(ownerId)) {
            return;
        }
        PENDING_SLEEPS.put(ownerId, new PendingSleep(
            player.serverLevel().dimension(),
            bedPos.immutable(),
            player.serverLevel().getGameTime()
        ));
    }

    public static void playerWokeUp(ServerPlayer player) {
        UUID ownerId = player.getUUID();
        if (INTERNAL_SLEEP_TRANSITIONS.contains(ownerId)) {
            return;
        }
        PENDING_SLEEPS.remove(ownerId);
        NightMission mission = MISSIONS_BY_OWNER.get(ownerId);
        if (mission != null) {
            finishMission(player.server, mission, false);
        }
    }

    public static boolean blocksSleepSkip(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();
        for (PendingSleep pending : PENDING_SLEEPS.values()) {
            if (pending.dimension.equals(dimension)) {
                return true;
            }
        }
        for (NightMission mission : MISSIONS_BY_OWNER.values()) {
            if (mission.dimension.equals(dimension)) {
                return true;
            }
        }
        return false;
    }

    public static void tick(MinecraftServer server) {
        SummonBlackMudService.tick(server);
        clearPreviousNightCompletions(server);
        tickPendingSleeps(server);
        for (UUID ownerId : List.copyOf(MISSIONS_BY_OWNER.keySet())) {
            NightMission mission = MISSIONS_BY_OWNER.get(ownerId);
            if (mission != null) {
                tickMission(server, mission);
            }
        }
    }

    private static void clearPreviousNightCompletions(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.serverLevel().isDay()) {
                player.getData(ModAttachments.IMAGINARY_SPACE.get()).clearNightMissionCompletion();
            }
        }
    }

    public static void playerUnavailable(ServerPlayer player) {
        PENDING_SLEEPS.remove(player.getUUID());
        NightMission mission = MISSIONS_BY_OWNER.get(player.getUUID());
        if (mission != null) {
            finishMission(player.server, mission, false);
        }
    }

    public static void entityLeavingLevel(Entity entity) {
        UUID ownerId = OWNER_BY_SHADOW.get(entity.getUUID());
        if (ownerId == null) {
            return;
        }
        MinecraftServer server = entity.getServer();
        NightMission mission = MISSIONS_BY_OWNER.get(ownerId);
        if (server != null && mission != null && !mission.finishing) {
            finishMission(server, mission, true);
        }
    }

    public static void shadowKilled(BlackShadowEntity shadow) {
        UUID ownerId = OWNER_BY_SHADOW.get(shadow.getUUID());
        MinecraftServer server = shadow.getServer();
        NightMission mission = ownerId == null ? null : MISSIONS_BY_OWNER.get(ownerId);
        if (server != null && mission != null && !mission.finishing) {
            finishMission(server, mission, true);
        }
    }

    public static void beginRetreat(BlackShadowEntity shadow) {
        UUID ownerId = OWNER_BY_SHADOW.get(shadow.getUUID());
        NightMission mission = ownerId == null ? null : MISSIONS_BY_OWNER.get(ownerId);
        if (mission != null && shadow.level() instanceof ServerLevel level) {
            beginReturn(level, shadow, mission);
        }
    }

    public static void serverStopping(MinecraftServer server) {
        for (NightMission mission : List.copyOf(MISSIONS_BY_OWNER.values())) {
            finishMission(server, mission, false);
        }
        PENDING_SLEEPS.clear();
        MISSIONS_BY_OWNER.clear();
        OWNER_BY_SHADOW.clear();
        INTERNAL_SLEEP_TRANSITIONS.clear();
    }

    private static void tickPendingSleeps(MinecraftServer server) {
        for (UUID ownerId : List.copyOf(PENDING_SLEEPS.keySet())) {
            PendingSleep pending = PENDING_SLEEPS.get(ownerId);
            ServerPlayer player = server.getPlayerList().getPlayer(ownerId);
            if (pending == null || player == null
                || !player.isAlive()
                || !player.level().dimension().equals(pending.dimension)) {
                PENDING_SLEEPS.remove(ownerId);
                continue;
            }
            if (player.isSleeping()) {
                PENDING_SLEEPS.remove(ownerId);
                startMission(player, pending);
                continue;
            }
            if (player.serverLevel().getGameTime() - pending.createdAt > PENDING_SLEEP_TIMEOUT_TICKS) {
                PENDING_SLEEPS.remove(ownerId);
            }
        }
    }

    private static void startMission(ServerPlayer player, PendingSleep pending) {
        ServerLevel level = player.serverLevel();
        BlackShadowEntity shadow = ModEntities.BLACK_SHADOW.get().create(level);
        if (shadow == null) {
            return;
        }

        shadow.setOwnerId(player.getUUID());
        shadow.setOwnerDismissalGeneration(player.getData(
            ModAttachments.IMAGINARY_SPACE.get()
        ).shadowDismissalGeneration());
        shadow.finalizeSpawn(
            level,
            level.getCurrentDifficultyAt(player.blockPosition()),
            MobSpawnType.MOB_SUMMONED,
            null
        );
        if (!shadow.teleportAround(player, 2.0D, 6.0D, SAFE_POSITION_ATTEMPTS)) {
            shadow.moveTo(
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot()
            );
        }
        if (!level.addFreshEntity(shadow)) {
            shadow.discard();
            return;
        }

        Vec3 homePosition = shadow.position();
        LivingEntity target = findNearestTarget(shadow);
        if (target == null) {
            shadow.dismiss(player);
            return;
        }
        if (!shadow.teleportAround(
            target,
            TARGET_TELEPORT_MIN_RADIUS,
            TARGET_TELEPORT_MAX_RADIUS,
            SAFE_POSITION_ATTEMPTS
        )) {
            shadow.dismiss(player);
            return;
        }

        NightMission mission = new NightMission(
            player.getUUID(),
            shadow.getUUID(),
            shadow.getId(),
            target.getUUID(),
            level.dimension(),
            homePosition,
            level.getGameTime(),
            dayIndex(level),
            PollutionService.canBecomeFullyCorrupted(target)
        );
        mission.nextRelocationTick = level.getGameTime() + RELOCATION_RETRY_TICKS;
        MISSIONS_BY_OWNER.put(player.getUUID(), mission);
        OWNER_BY_SHADOW.put(shadow.getUUID(), player.getUUID());

        shadow.beginNightMission(target);
        syncCamera(player, mission, true);
    }

    @Nullable
    private static LivingEntity findNearestTarget(BlackShadowEntity shadow) {
        double radiusSqr = INITIAL_TARGET_SCAN_RADIUS * INITIAL_TARGET_SCAN_RADIUS;
        LivingEntity nearestCorruptible = null;
        LivingEntity nearestFallback = null;
        double nearestCorruptibleDistance = Double.MAX_VALUE;
        double nearestFallbackDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : shadow.level().getEntitiesOfClass(
            LivingEntity.class,
            shadow.getBoundingBox().inflate(INITIAL_TARGET_SCAN_RADIUS),
            shadow::canStartNightMissionAgainst
        )) {
            double distance = shadow.getBoundingBox().getCenter().distanceToSqr(
                candidate.getBoundingBox().getCenter()
            );
            if (distance > radiusSqr) {
                continue;
            }
            if (PollutionService.canBecomeFullyCorrupted(candidate)) {
                if (distance < nearestCorruptibleDistance) {
                    nearestCorruptible = candidate;
                    nearestCorruptibleDistance = distance;
                }
            } else if (PollutionService.isUnpollutableServantOrCardUser(candidate)
                && distance < nearestFallbackDistance) {
                nearestFallback = candidate;
                nearestFallbackDistance = distance;
            }
        }
        return nearestCorruptible != null ? nearestCorruptible : nearestFallback;
    }

    private static void tickMission(MinecraftServer server, NightMission mission) {
        ServerLevel level = server.getLevel(mission.dimension);
        ServerPlayer owner = server.getPlayerList().getPlayer(mission.ownerId);
        BlackShadowEntity shadow = blackShadow(level, mission.shadowId);
        if (level == null || owner == null || shadow == null
            || !owner.isAlive()
            || !owner.level().dimension().equals(mission.dimension)) {
            finishMission(server, mission, owner != null);
            return;
        }
        if (!owner.isSleeping()) {
            finishMission(server, mission, false);
            return;
        }
        if (level.isDay()) {
            finishMission(server, mission, true);
            return;
        }
        if (level.getGameTime() - mission.startedAt >= MAX_MISSION_DURATION_TICKS) {
            finishMission(server, mission, false);
            return;
        }

        maintainObservation(level, owner, mission);
        if (mission.returning) {
            if (level.getGameTime() >= mission.finishAt) {
                finishMission(server, mission, false, true);
            }
            return;
        }

        LivingEntity target = living(level, mission.targetId);
        if (target == null || !target.isAlive() || target.isRemoved()) {
            beginReturn(level, shadow, mission);
            return;
        }
        if (mission.corruptionTarget) {
            PollutionData pollution = target.getExistingDataOrNull(ModAttachments.POLLUTION.get());
            if ((pollution != null && pollution.fullyCorrupted())
                || !PollutionService.canBecomeFullyCorrupted(target)) {
                beginReturn(level, shadow, mission);
                return;
            }
        }
        if (!shadow.canAttack(target)) {
            beginReturn(level, shadow, mission);
            return;
        }

        double relocationDistanceSqr = RELOCATION_DISTANCE * RELOCATION_DISTANCE;
        if (shadow.getTarget() != target || shadow.distanceToSqr(target) > relocationDistanceSqr) {
            shadow.beginNightMission(target);
            if (level.getGameTime() >= mission.nextRelocationTick) {
                boolean relocated = shadow.teleportAround(
                    target,
                    RETRY_TELEPORT_MIN_RADIUS,
                    RETRY_TELEPORT_MAX_RADIUS,
                    SAFE_POSITION_ATTEMPTS
                );
                mission.nextRelocationTick = level.getGameTime() + RELOCATION_RETRY_TICKS;
                if (relocated) {
                    mission.relocationFailures = 0;
                } else if (++mission.relocationFailures >= MAX_RELOCATION_FAILURES) {
                    finishMission(server, mission, false);
                }
            }
        } else {
            mission.relocationFailures = 0;
        }
    }

    private static void beginReturn(
        ServerLevel level,
        BlackShadowEntity shadow,
        NightMission mission
    ) {
        if (mission.returning) {
            return;
        }
        mission.returning = true;
        mission.finishAt = level.getGameTime() + RETURN_DELAY_TICKS;
        shadow.endNightMission();
        shadow.teleportTo(mission.homePosition.x, mission.homePosition.y, mission.homePosition.z);
        shadow.setDeltaMovement(Vec3.ZERO);
    }

    private static void maintainObservation(ServerLevel level, ServerPlayer owner, NightMission mission) {
        if (level.getGameTime() >= mission.nextCameraSyncTick) {
            syncCamera(owner, mission, true);
        }
    }

    private static void syncCamera(ServerPlayer owner, NightMission mission, boolean active) {
        PacketDistributor.sendToPlayer(owner, new NightShadowCameraPayload(mission.shadowEntityId, active));
        if (active) {
            mission.nextCameraSyncTick = owner.serverLevel().getGameTime() + CAMERA_SYNC_INTERVAL_TICKS;
        }
    }

    private static void finishMission(
        MinecraftServer server,
        NightMission mission,
        boolean wakeOwner
    ) {
        finishMission(server, mission, wakeOwner, false);
    }

    private static void finishMission(
        MinecraftServer server,
        NightMission mission,
        boolean wakeOwner,
        boolean completedNightTask
    ) {
        if (mission.finishing) {
            return;
        }
        mission.finishing = true;
        MISSIONS_BY_OWNER.remove(mission.ownerId, mission);
        OWNER_BY_SHADOW.remove(mission.shadowId, mission.ownerId);

        ServerLevel level = server.getLevel(mission.dimension);
        BlackShadowEntity shadow = blackShadow(level, mission.shadowId);
        ServerPlayer owner = server.getPlayerList().getPlayer(mission.ownerId);
        if (owner != null) {
            if (completedNightTask) {
                owner.getData(ModAttachments.IMAGINARY_SPACE.get())
                    .markNightMissionCompleted(mission.missionDay);
            }
            syncCamera(owner, mission, false);
        }
        if (shadow != null) {
            shadow.endNightMission();
            shadow.dismiss(owner == null ? shadow : owner);
        }
        if (!wakeOwner || owner == null || !owner.isSleeping()) {
            return;
        }
        INTERNAL_SLEEP_TRANSITIONS.add(owner.getUUID());
        try {
            owner.stopSleepInBed(true, true);
        } finally {
            INTERNAL_SLEEP_TRANSITIONS.remove(owner.getUUID());
        }
    }

    @Nullable
    private static BlackShadowEntity blackShadow(@Nullable ServerLevel level, UUID entityId) {
        Entity entity = level == null ? null : level.getEntity(entityId);
        return entity instanceof BlackShadowEntity shadow && shadow.isAlive() ? shadow : null;
    }

    @Nullable
    private static LivingEntity living(@Nullable ServerLevel level, UUID entityId) {
        Entity entity = level == null ? null : level.getEntity(entityId);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static long dayIndex(ServerLevel level) {
        return Math.floorDiv(level.getDayTime(), 24000L);
    }

    private record PendingSleep(ResourceKey<Level> dimension, BlockPos bedPos, long createdAt) {
    }

    private static final class NightMission {
        private final UUID ownerId;
        private final UUID shadowId;
        private final int shadowEntityId;
        private final UUID targetId;
        private final ResourceKey<Level> dimension;
        private final Vec3 homePosition;
        private final long startedAt;
        private final long missionDay;
        private final boolean corruptionTarget;
        private long nextRelocationTick;
        private long nextCameraSyncTick;
        private long finishAt;
        private int relocationFailures;
        private boolean returning;
        private boolean finishing;

        private NightMission(
            UUID ownerId,
            UUID shadowId,
            int shadowEntityId,
            UUID targetId,
            ResourceKey<Level> dimension,
            Vec3 homePosition,
            long startedAt,
            long missionDay,
            boolean corruptionTarget
        ) {
            this.ownerId = ownerId;
            this.shadowId = shadowId;
            this.shadowEntityId = shadowEntityId;
            this.targetId = targetId;
            this.dimension = dimension;
            this.homePosition = homePosition;
            this.startedAt = startedAt;
            this.missionDay = missionDay;
            this.corruptionTarget = corruptionTarget;
        }
    }

    private BlackShadowNightService() {
    }
}
