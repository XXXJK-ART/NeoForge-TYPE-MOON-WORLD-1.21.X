package io.github.typemoonaddon.magic;

import io.github.typemoonaddon.config.GameplayConfig;
import io.github.typemoonaddon.data.ImaginarySpaceData.BlackMudSummonMode;
import io.github.typemoonaddon.entity.BlackShadowEntity;
import io.github.typemoonaddon.registry.ModAttachments;
import io.github.typemoonaddon.registry.ModBlocks;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import javax.annotation.Nullable;

public final class SummonBlackMudService {
    public static final double MANA_COST = 100.0D;
    public static final int COOLDOWN_TICKS = 5 * 20;
    public static final double CAST_RANGE = 20.0D;
    private static final int POOL_RADIUS = 2;
    public static final int BLACK_SHADOW_INITIAL_RADIUS = 4;
    public static final int BLACK_SHADOW_MAX_RADIUS = GameplayConfig.BLACK_SHADOW_MUD_RADIUS;
    private static final int BLACK_SHADOW_SOURCE_SPACING = 2;
    private static final int MAX_CONNECTED_MUD_BLOCKS = 16_384;
    private static final int REMOVAL_BLOCKS_PER_TICK = 64;
    private static final Map<UUID, List<Deployment>> ACTIVE_DEPLOYMENTS = new HashMap<>();
    private static final Map<UUID, Long> NEXT_CAST_TICK = new HashMap<>();
    private static final List<PendingRemoval> PENDING_REMOVALS = new ArrayList<>();

    public static boolean cast(ServerPlayer player) {
        if (!validCaster(player)) {
            return false;
        }
        if (player.getData(ModAttachments.IMAGINARY_SPACE.get()).blackMudSummonMode()
            == BlackMudSummonMode.DISMISS) {
            return dismissAll(player, true);
        }

        long now = player.serverLevel().getGameTime();
        long nextCast = NEXT_CAST_TICK.getOrDefault(player.getUUID(), 0L);
        if (now < nextCast) {
            long seconds = Math.max(1L, (nextCast - now + 19L) / 20L);
            player.displayClientMessage(Component.translatable(
                "message.typemoonaddon.summon_black_mud.cooldown",
                seconds
            ), true);
            return false;
        }

        HitResult hit = player.pick(CAST_RANGE, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            player.displayClientMessage(Component.translatable("message.typemoonaddon.summon_black_mud.no_target"), true);
            return false;
        }

        ServerLevel level = player.serverLevel();
        BlockPos center = blockHit.getBlockPos().relative(blockHit.getDirection());
        if (blockHit.getDirection() != Direction.UP) {
            center = findSurface(level, blockHit.getBlockPos());
        }

        Set<BlockPos> availableSources = new HashSet<>();
        for (int x = -POOL_RADIUS; x <= POOL_RADIUS; x++) {
            for (int z = -POOL_RADIUS; z <= POOL_RADIUS; z++) {
                if (x * x + z * z > POOL_RADIUS * POOL_RADIUS) {
                    continue;
                }
                BlockPos target = findSurface(level, center.offset(x, 0, z));
                BlockState state = level.getBlockState(target);
                if (state.is(ModBlocks.BLACK_MUD.get())) {
                    continue;
                }
                if (!state.canBeReplaced() || level.getBlockState(target.below()).canBeReplaced()) {
                    continue;
                }
                availableSources.add(target.immutable());
            }
        }

        if (availableSources.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.typemoonaddon.summon_black_mud.no_space"), true);
            return false;
        }
        if (!TypeMoonIntegration.tryConsumeMana(player, MANA_COST)) {
            player.displayClientMessage(Component.translatable("message.typemoonaddon.not_enough_mana"), true);
            return false;
        }

        Set<BlockPos> placedSources = new HashSet<>();
        for (BlockPos target : availableSources) {
            if (level.setBlock(target, ModBlocks.BLACK_MUD.get().defaultBlockState(), 11)) {
                placedSources.add(target);
            }
        }
        if (placedSources.isEmpty()) {
            TypeMoonIntegration.refundMana(player, MANA_COST);
            player.displayClientMessage(Component.translatable("message.typemoonaddon.summon_black_mud.no_space"), true);
            return false;
        }

        ACTIVE_DEPLOYMENTS.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>())
            .add(new Deployment(
                level.dimension(), Set.copyOf(placedSources), null, GrailParticleService.palette(player)
            ));
        NEXT_CAST_TICK.put(player.getUUID(), now + COOLDOWN_TICKS);

        GrailParticleService.send(
            level,
            player,
            ParticleTypes.SQUID_INK,
            center.getX() + 0.5D,
            center.getY() + 0.35D,
            center.getZ() + 0.5D,
            48,
            1.8D,
            0.25D,
            1.8D,
            0.04D
        );
        level.playSound(
            null,
            center,
            SoundEvents.SCULK_CATALYST_BLOOM,
            SoundSource.PLAYERS,
            1.0F,
            0.45F
        );
        player.displayClientMessage(Component.translatable("message.typemoonaddon.summon_black_mud.summoned"), true);
        return true;
    }

    /** Immediately deploys several owner-attributed sources around a black shadow without occupying another cast. */
    public static boolean deployAround(BlackShadowEntity shadow, ServerPlayer owner, int requestedRadius) {
        if (!shadow.isAlive()
            || !owner.isAlive()
            || shadow.level() != owner.level()
            || !(shadow.level() instanceof ServerLevel level)) {
            return false;
        }

        int radius = Math.clamp(
            requestedRadius,
            BLACK_SHADOW_INITIAL_RADIUS,
            BLACK_SHADOW_MAX_RADIUS
        );
        BlockPos center = findSurface(level, shadow.blockPosition());
        Set<BlockPos> placedSources = new HashSet<>();
        boolean alreadyCovered = false;
        for (int x = -radius; x <= radius;
            x += BLACK_SHADOW_SOURCE_SPACING) {
            for (int z = -radius; z <= radius;
                z += BLACK_SHADOW_SOURCE_SPACING) {
                BlockPos target = findSurface(level, center.offset(x, 0, z));
                double deltaX = target.getX() + 0.5D - shadow.getX();
                double deltaY = target.getY() + 0.5D - shadow.getY();
                double deltaZ = target.getZ() + 0.5D - shadow.getZ();
                if (deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ > radius * radius) {
                    continue;
                }
                BlockState state = level.getBlockState(target);
                if (state.is(ModBlocks.BLACK_MUD.get())) {
                    alreadyCovered = true;
                    continue;
                }
                if (!state.canBeReplaced() || level.getBlockState(target.below()).canBeReplaced()) {
                    continue;
                }
                if (level.setBlock(target, ModBlocks.BLACK_MUD.get().defaultBlockState(), 11)) {
                    placedSources.add(target.immutable());
                }
            }
        }

        if (placedSources.isEmpty()) {
            return alreadyCovered;
        }
        ACTIVE_DEPLOYMENTS.computeIfAbsent(owner.getUUID(), ignored -> new ArrayList<>())
            .add(new Deployment(
                level.dimension(), Set.copyOf(placedSources), shadow.getUUID(),
                GrailParticleService.palette(owner)
            ));
        GrailParticleService.send(
            level,
            owner,
            ParticleTypes.SQUID_INK,
            shadow.getX(),
            shadow.getY() + 0.2D,
            shadow.getZ(),
            36,
            radius * 0.35D,
            0.2D,
            radius * 0.35D,
            0.025D
        );
        level.playSound(
            null,
            shadow.blockPosition(),
            SoundEvents.SCULK_CATALYST_BLOOM,
            SoundSource.HOSTILE,
            0.8F,
            0.4F
        );
        return true;
    }

    public static void blackShadowRemoved(BlackShadowEntity shadow) {
        MinecraftServer server = shadow.getServer();
        if (server == null) {
            return;
        }
        List<Deployment> removed = new ArrayList<>();
        for (List<Deployment> deployments : ACTIVE_DEPLOYMENTS.values()) {
            for (int index = deployments.size() - 1; index >= 0; index--) {
                Deployment deployment = deployments.get(index);
                if (shadow.getUUID().equals(deployment.sourceShadowId())) {
                    removed.add(deployment);
                    deployments.remove(index);
                }
            }
        }
        ACTIVE_DEPLOYMENTS.values().removeIf(List::isEmpty);
        removeDeployments(server, removed);
    }

    public static void tick(MinecraftServer server) {
        var iterator = PENDING_REMOVALS.iterator();
        while (iterator.hasNext()) {
            PendingRemoval removal = iterator.next();
            ServerLevel level = server.getLevel(removal.dimension());
            if (level == null) {
                iterator.remove();
                continue;
            }

            double x = 0.0D;
            double y = 0.0D;
            double z = 0.0D;
            int removed = 0;
            while (removed < REMOVAL_BLOCKS_PER_TICK && !removal.positions().isEmpty()) {
                BlockPos pos = removal.positions().removeFirst();
                if (!level.getBlockState(pos).is(ModBlocks.BLACK_MUD.get())) {
                    continue;
                }
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
                x += pos.getX() + 0.5D;
                y += pos.getY() + 0.3D;
                z += pos.getZ() + 0.5D;
                removed++;
            }
            if (removed > 0) {
                GrailParticleService.send(
                    level,
                    removal.palette(),
                    ParticleTypes.SQUID_INK,
                    x / removed,
                    y / removed,
                    z / removed,
                    Math.min(24, 4 + removed / 4),
                    0.8D,
                    0.25D,
                    0.8D,
                    0.02D
                );
            }
            if (removal.positions().isEmpty()) {
                iterator.remove();
            }
        }
    }

    public static boolean selectMode(ServerPlayer player, BlackMudSummonMode requestedMode) {
        if (!validCaster(player)) {
            return false;
        }
        BlackMudSummonMode mode = requestedMode == null ? BlackMudSummonMode.RELEASE : requestedMode;
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        if (data.setBlackMudSummonMode(mode)) {
            player.syncData(ModAttachments.IMAGINARY_SPACE.get());
        }
        player.displayClientMessage(Component.translatable(
            "message.typemoonaddon.summon_black_mud.mode." + mode.serializedName()
        ), true);
        return true;
    }

    @Nullable
    public static ServerPlayer ownerAt(ServerLevel level, BlockPos mudPosition) {
        MinecraftServer server = level.getServer();
        UUID closestOwner = null;
        double closestDistance = Double.MAX_VALUE;
        for (var entry : ACTIVE_DEPLOYMENTS.entrySet()) {
            for (Deployment deployment : entry.getValue()) {
                if (deployment.dimension() != level.dimension()) {
                    continue;
                }
                for (BlockPos source : deployment.sources()) {
                    double distance = source.distSqr(mudPosition);
                    if (distance <= 12.0D * 12.0D && distance < closestDistance) {
                        closestOwner = entry.getKey();
                        closestDistance = distance;
                    }
                }
            }
        }
        return closestOwner == null ? null : server.getPlayerList().getPlayer(closestOwner);
    }

    public static void playerDied(ServerPlayer player) {
        dismissAll(player, false);
        NEXT_CAST_TICK.remove(player.getUUID());
    }

    public static void playerLoggedOut(ServerPlayer player) {
        dismissAll(player, false);
        NEXT_CAST_TICK.remove(player.getUUID());
    }

    public static void serverStopping(MinecraftServer server) {
        var deployments = ACTIVE_DEPLOYMENTS.values().stream().flatMap(List::stream).toList();
        ACTIVE_DEPLOYMENTS.clear();
        NEXT_CAST_TICK.clear();
        removeDeploymentsImmediately(server, deployments);
        for (PendingRemoval removal : PENDING_REMOVALS) {
            removePositionsImmediately(server, removal.dimension(), removal.positions());
        }
        PENDING_REMOVALS.clear();
    }

    private static boolean dismissAll(ServerPlayer player, boolean showMessage) {
        List<Deployment> deployments = ACTIVE_DEPLOYMENTS.remove(player.getUUID());
        if (deployments == null || deployments.isEmpty()) {
            if (showMessage) {
                player.displayClientMessage(Component.translatable(
                    "message.typemoonaddon.summon_black_mud.no_active"
                ), true);
            }
            return false;
        }
        removeDeployments(player.getServer(), deployments);
        if (showMessage) {
            player.displayClientMessage(Component.translatable(
                "message.typemoonaddon.summon_black_mud.dismissed",
                deployments.size()
            ), true);
        }
        return true;
    }

    private static void removeDeployments(MinecraftServer server, List<Deployment> deployments) {
        if (server == null) {
            return;
        }
        Map<ResourceKey<Level>, Set<BlockPos>> sourcesByDimension = new HashMap<>();
        Map<ResourceKey<Level>, Byte> palettesByDimension = new HashMap<>();
        for (Deployment deployment : deployments) {
            sourcesByDimension.computeIfAbsent(deployment.dimension(), ignored -> new HashSet<>())
                .addAll(deployment.sources());
            palettesByDimension.merge(
                deployment.dimension(), deployment.palette(), GrailParticleService::highest
            );
        }
        for (var entry : sourcesByDimension.entrySet()) {
            scheduleSourcesRemoval(
                server,
                entry.getKey(),
                Set.copyOf(entry.getValue()),
                palettesByDimension.getOrDefault(entry.getKey(), GrailParticleService.BLACK)
            );
        }
    }

    private static void scheduleSourcesRemoval(
        MinecraftServer server,
        ResourceKey<Level> dimension,
        Set<BlockPos> ownedSources,
        byte palette
    ) {
        ServerLevel level = server.getLevel(dimension);
        if (level == null) {
            return;
        }

        MudComponent component = collectConnectedMud(level, ownedSources);
        boolean connectedToForeignSource = component.positions().stream().anyMatch(pos ->
            !ownedSources.contains(pos) && level.getFluidState(pos).isSource()
        );
        Set<BlockPos> positionsToRemove = !component.truncated() && !connectedToForeignSource
            ? component.positions()
            : ownedSources;

        double centerX = ownedSources.stream().mapToDouble(BlockPos::getX).average().orElse(0.0D);
        double centerY = ownedSources.stream().mapToDouble(BlockPos::getY).average().orElse(0.0D);
        double centerZ = ownedSources.stream().mapToDouble(BlockPos::getZ).average().orElse(0.0D);
        List<BlockPos> ordered = new ArrayList<>(positionsToRemove);
        ordered.sort(
            Comparator.<BlockPos, Boolean>comparing(pos -> !ownedSources.contains(pos))
                .thenComparingDouble(pos -> -distanceSquared(pos, centerX, centerY, centerZ))
        );
        if (!ordered.isEmpty()) {
            PENDING_REMOVALS.add(new PendingRemoval(dimension, new ArrayDeque<>(ordered), palette));
            level.playSound(
                null,
                ownedSources.iterator().next(),
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS,
                0.9F,
                0.55F
            );
        }
    }

    private static void removeDeploymentsImmediately(MinecraftServer server, List<Deployment> deployments) {
        Map<ResourceKey<Level>, Set<BlockPos>> sourcesByDimension = new HashMap<>();
        for (Deployment deployment : deployments) {
            sourcesByDimension.computeIfAbsent(deployment.dimension(), ignored -> new HashSet<>())
                .addAll(deployment.sources());
        }
        for (var entry : sourcesByDimension.entrySet()) {
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null) {
                continue;
            }
            MudComponent component = collectConnectedMud(level, entry.getValue());
            removePositionsImmediately(server, entry.getKey(), component.positions());
        }
    }

    private static void removePositionsImmediately(
        MinecraftServer server,
        ResourceKey<Level> dimension,
        Iterable<BlockPos> positions
    ) {
        ServerLevel level = server.getLevel(dimension);
        if (level == null) {
            return;
        }
        for (BlockPos pos : positions) {
            if (level.getBlockState(pos).is(ModBlocks.BLACK_MUD.get())) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
            }
        }
    }

    private static double distanceSquared(BlockPos pos, double x, double y, double z) {
        double dx = pos.getX() - x;
        double dy = pos.getY() - y;
        double dz = pos.getZ() - z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static MudComponent collectConnectedMud(ServerLevel level, Set<BlockPos> sources) {
        Set<BlockPos> positions = new HashSet<>();
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        for (BlockPos source : sources) {
            if (level.getBlockState(source).is(ModBlocks.BLACK_MUD.get()) && positions.add(source)) {
                pending.add(source);
            }
        }

        while (!pending.isEmpty() && positions.size() < MAX_CONNECTED_MUD_BLOCKS) {
            BlockPos current = pending.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos adjacent = current.relative(direction);
                if (positions.contains(adjacent)
                    || !level.getBlockState(adjacent).is(ModBlocks.BLACK_MUD.get())) {
                    continue;
                }
                BlockPos immutable = adjacent.immutable();
                positions.add(immutable);
                pending.addLast(immutable);
            }
        }
        return new MudComponent(Set.copyOf(positions), !pending.isEmpty());
    }

    private static boolean validCaster(ServerPlayer player) {
        return player.isAlive()
            && !player.isSpectator()
            && !HolyGrailService.blocksAction(player)
            && TypeMoonIntegration.isSummonBlackMudLearned(player);
    }

    private static BlockPos findSurface(ServerLevel level, BlockPos start) {
        BlockPos.MutableBlockPos cursor = start.mutable();
        for (int offset = 2; offset >= -3; offset--) {
            cursor.set(start.getX(), start.getY() + offset, start.getZ());
            if (level.getBlockState(cursor).canBeReplaced()
                && !level.getBlockState(cursor.below()).canBeReplaced()) {
                return cursor.immutable();
            }
        }
        return start;
    }

    private record Deployment(
        ResourceKey<Level> dimension,
        Set<BlockPos> sources,
        @Nullable UUID sourceShadowId,
        byte palette
    ) {
    }

    private record MudComponent(Set<BlockPos> positions, boolean truncated) {
    }

    private record PendingRemoval(ResourceKey<Level> dimension, ArrayDeque<BlockPos> positions, byte palette) {
    }

    private SummonBlackMudService() {
    }
}
