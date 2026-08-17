package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.data.ImaginarySpaceData;
import com.example.typemoonaddon.entity.SakuraBlackShadowEntity;
import com.example.typemoonaddon.registry.AddonAttachments;
import com.example.typemoonaddon.registry.AddonBlocks;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
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

public final class SakuraSummonBlackMudService {
    public static final double MANA_COST = 100.0D;
    public static final int COOLDOWN_TICKS = 5 * 20;
    public static final double CAST_RANGE = 20.0D;
    private static final int POOL_RADIUS = 2;
    private static final int MAX_CONNECTED_MUD_BLOCKS = 16_384;
    private static final int REMOVAL_BLOCKS_PER_TICK = 64;
    private static final Map<UUID, List<Deployment>> ACTIVE_DEPLOYMENTS = new HashMap<>();
    private static final Map<UUID, Long> NEXT_CAST_TICK = new HashMap<>();
    private static final List<PendingRemoval> PENDING_REMOVALS = new ArrayList<>();

    public static boolean cast(ServerPlayer player) {
        if (!validCaster(player)) {
            return false;
        }
        ImaginarySpaceData data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        if (data.blackMudSummonMode() == ImaginarySpaceData.BlackMudSummonMode.DISMISS) {
            return dismissAll(player, true);
        }

        long now = player.serverLevel().getGameTime();
        long nextCast = NEXT_CAST_TICK.getOrDefault(player.getUUID(), 0L);
        if (now < nextCast) {
            long seconds = Math.max(1L, (nextCast - now + 19L) / 20L);
            player.displayClientMessage(Component.translatable("message.typemoonworld.summon_black_mud.cooldown", seconds), true);
            return false;
        }

        HitResult hit = player.pick(CAST_RANGE, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.summon_black_mud.no_target"), true);
            return false;
        }

        ServerLevel level = player.serverLevel();
        BlockPos center = blockHit.getBlockPos().relative(blockHit.getDirection());
        if (blockHit.getDirection() != Direction.UP) {
            center = findSurface(level, blockHit.getBlockPos());
        }

        Set<BlockPos> sources = new HashSet<>();
        for (int x = -POOL_RADIUS; x <= POOL_RADIUS; x++) {
            for (int z = -POOL_RADIUS; z <= POOL_RADIUS; z++) {
                if (x * x + z * z > POOL_RADIUS * POOL_RADIUS) {
                    continue;
                }
                BlockPos target = findSurface(level, center.offset(x, 0, z));
                BlockState state = level.getBlockState(target);
                if (state.is(AddonBlocks.BLACK_MUD.get())) {
                    continue;
                }
                if (!state.canBeReplaced() || level.getBlockState(target.below()).canBeReplaced()) {
                    continue;
                }
                sources.add(target.immutable());
            }
        }

        if (sources.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.summon_black_mud.no_space"), true);
            return false;
        }
        if (!SakuraTypeMoonIntegration.tryConsumeMana(player, MANA_COST)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
            return false;
        }

        Set<BlockPos> placed = new HashSet<>();
        for (BlockPos pos : sources) {
            if (level.setBlock(pos, AddonBlocks.BLACK_MUD.get().defaultBlockState(), 11)) {
                placed.add(pos);
            }
        }
        if (placed.isEmpty()) {
            SakuraTypeMoonIntegration.refundMana(player, MANA_COST);
            player.displayClientMessage(Component.translatable("message.typemoonworld.summon_black_mud.no_space"), true);
            return false;
        }

        ACTIVE_DEPLOYMENTS.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>())
                .add(new Deployment(level.dimension(), Set.copyOf(placed), null));
        NEXT_CAST_TICK.put(player.getUUID(), now + COOLDOWN_TICKS);
        SakuraParticleService.send(level, ParticleTypes.SQUID_INK, center.getX() + 0.5D, center.getY() + 0.35D, center.getZ() + 0.5D, 48, 1.8D, 0.25D, 1.8D, 0.04D);
        level.playSound(null, center, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 1.0F, 0.45F);
        player.displayClientMessage(Component.translatable("message.typemoonworld.summon_black_mud.summoned"), true);
        return true;
    }

    public static boolean selectMode(ServerPlayer player, ImaginarySpaceData.BlackMudSummonMode mode) {
        if (!validCaster(player)) {
            return false;
        }
        ImaginarySpaceData data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        ImaginarySpaceData.BlackMudSummonMode validated = mode == null ? ImaginarySpaceData.BlackMudSummonMode.RELEASE : mode;
        if (data.setBlackMudSummonMode(validated)) {
            AddonAttachments.sync(player, AddonAttachments.IMAGINARY_SPACE);
        }
        player.displayClientMessage(Component.translatable("message.typemoonworld.summon_black_mud.mode." + validated.serializedName()), true);
        return true;
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
            int removed = 0;
            double x = 0.0D;
            double y = 0.0D;
            double z = 0.0D;
            while (removed < REMOVAL_BLOCKS_PER_TICK && !removal.positions().isEmpty()) {
                BlockPos pos = removal.positions().removeFirst();
                if (!level.getBlockState(pos).is(AddonBlocks.BLACK_MUD.get())) {
                    continue;
                }
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
                x += pos.getX() + 0.5D;
                y += pos.getY() + 0.25D;
                z += pos.getZ() + 0.5D;
                removed++;
            }
            if (removed > 0) {
                SakuraParticleService.send(level, ParticleTypes.SQUID_INK, x / removed, y / removed, z / removed, Math.min(24, 4 + removed / 4), 0.8D, 0.25D, 0.8D, 0.02D);
            }
            if (removal.positions().isEmpty()) {
                iterator.remove();
            }
        }
    }

    @Nullable
    public static ServerPlayer ownerAt(ServerLevel level, BlockPos mudPosition) {
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
        return closestOwner == null ? null : level.getServer().getPlayerList().getPlayer(closestOwner);
    }

    public static void blackShadowRemoved(SakuraBlackShadowEntity shadow) {
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

    public static void playerUnavailable(ServerPlayer player) {
        dismissAll(player, false);
        NEXT_CAST_TICK.remove(player.getUUID());
    }

    public static void serverStopping(MinecraftServer server) {
        List<Deployment> deployments = ACTIVE_DEPLOYMENTS.values().stream().flatMap(List::stream).toList();
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
                player.displayClientMessage(Component.translatable("message.typemoonworld.summon_black_mud.no_active"), true);
            }
            return false;
        }
        removeDeployments(player.server, deployments);
        if (showMessage) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.summon_black_mud.dismissed", deployments.size()), true);
        }
        return true;
    }

    private static void removeDeployments(MinecraftServer server, List<Deployment> deployments) {
        if (server == null) {
            return;
        }
        Map<ResourceKey<Level>, Set<BlockPos>> byDimension = new HashMap<>();
        for (Deployment deployment : deployments) {
            byDimension.computeIfAbsent(deployment.dimension(), ignored -> new HashSet<>()).addAll(deployment.sources());
        }
        for (var entry : byDimension.entrySet()) {
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null) {
                continue;
            }
            Set<BlockPos> connected = collectConnectedMud(level, entry.getValue());
            List<BlockPos> ordered = new ArrayList<>(connected);
            double cx = entry.getValue().stream().mapToDouble(BlockPos::getX).average().orElse(0.0D);
            double cy = entry.getValue().stream().mapToDouble(BlockPos::getY).average().orElse(0.0D);
            double cz = entry.getValue().stream().mapToDouble(BlockPos::getZ).average().orElse(0.0D);
            ordered.sort(Comparator.comparingDouble(pos -> -pos.distToCenterSqr(cx, cy, cz)));
            PENDING_REMOVALS.add(new PendingRemoval(entry.getKey(), new ArrayDeque<>(ordered)));
            level.playSound(null, entry.getValue().iterator().next(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.9F, 0.55F);
        }
    }

    private static void removeDeploymentsImmediately(MinecraftServer server, List<Deployment> deployments) {
        for (Deployment deployment : deployments) {
            ServerLevel level = server.getLevel(deployment.dimension());
            if (level != null) {
                removePositionsImmediately(server, deployment.dimension(), collectConnectedMud(level, deployment.sources()));
            }
        }
    }

    private static void removePositionsImmediately(MinecraftServer server, ResourceKey<Level> dimension, Iterable<BlockPos> positions) {
        ServerLevel level = server.getLevel(dimension);
        if (level == null) {
            return;
        }
        for (BlockPos pos : positions) {
            if (level.getBlockState(pos).is(AddonBlocks.BLACK_MUD.get())) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
            }
        }
    }

    private static Set<BlockPos> collectConnectedMud(ServerLevel level, Set<BlockPos> sources) {
        Set<BlockPos> positions = new HashSet<>();
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        for (BlockPos source : sources) {
            if (level.getBlockState(source).is(AddonBlocks.BLACK_MUD.get()) && positions.add(source)) {
                pending.add(source);
            }
        }
        while (!pending.isEmpty() && positions.size() < MAX_CONNECTED_MUD_BLOCKS) {
            BlockPos current = pending.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos adjacent = current.relative(direction);
                if (!positions.contains(adjacent) && level.getBlockState(adjacent).is(AddonBlocks.BLACK_MUD.get())) {
                    BlockPos immutable = adjacent.immutable();
                    positions.add(immutable);
                    pending.addLast(immutable);
                }
            }
        }
        return positions;
    }

    private static boolean validCaster(ServerPlayer player) {
        return player != null && player.isAlive() && !player.isSpectator() && SakuraTypeMoonIntegration.isSummonBlackMudLearned(player);
    }

    private static BlockPos findSurface(ServerLevel level, BlockPos start) {
        BlockPos.MutableBlockPos cursor = start.mutable();
        for (int offset = 2; offset >= -3; offset--) {
            cursor.set(start.getX(), start.getY() + offset, start.getZ());
            if (level.getBlockState(cursor).canBeReplaced() && !level.getBlockState(cursor.below()).canBeReplaced()) {
                return cursor.immutable();
            }
        }
        return start;
    }

    private record Deployment(ResourceKey<Level> dimension, Set<BlockPos> sources, @Nullable UUID sourceShadowId) {
    }

    private record PendingRemoval(ResourceKey<Level> dimension, ArrayDeque<BlockPos> positions) {
    }

    private SakuraSummonBlackMudService() {
    }
}
