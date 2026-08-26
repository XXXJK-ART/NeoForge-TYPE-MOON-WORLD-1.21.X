package io.github.typemoonaddon.magic;

import io.github.typemoonaddon.entity.ShadowFamiliarEntity;
import io.github.typemoonaddon.network.OpenShadowTransferPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-authoritative three-second transfer to an owned Shadow Familiar. */
public final class ShadowTransferService {
    public static final int CHANNEL_TICKS = 60;
    public static final double BASE_MANA_COST = 20.0D;
    public static final double MANA_PER_BLOCK = 2.0D;
    private static final int MAX_TARGETS = 256;
    private static final int PARTICLE_INTERVAL_TICKS = 2;
    private static final Map<UUID, TransferState> TRANSFERS = new HashMap<>();

    public static boolean openSelection(ServerPlayer player) {
        if (!validCaster(player)) {
            return false;
        }
        if (TRANSFERS.containsKey(player.getUUID())) {
            notify(player, "message.typemoonaddon.shadow_transfer.already_channeling");
            return true;
        }

        List<ShadowFamiliarEntity> familiars = ownedFamiliars(player);
        List<OpenShadowTransferPayload.Target> targets = new ArrayList<>(familiars.size());
        for (ShadowFamiliarEntity familiar : familiars) {
            double distance = player.distanceTo(familiar);
            targets.add(new OpenShadowTransferPayload.Target(
                familiar.getUUID(),
                familiar.getBlockX(),
                familiar.getBlockY(),
                familiar.getBlockZ(),
                (int)Math.ceil(distance),
                manaCost(distance)
            ));
        }
        PacketDistributor.sendToPlayer(player, new OpenShadowTransferPayload(
            player.getBlockX(),
            player.getBlockZ(),
            targets
        ));
        return true;
    }

    public static boolean begin(ServerPlayer player, UUID familiarId) {
        if (!validCaster(player) || familiarId == null || TRANSFERS.containsKey(player.getUUID())) {
            return false;
        }
        Entity entity = player.serverLevel().getEntity(familiarId);
        if (!(entity instanceof ShadowFamiliarEntity familiar) || !validTarget(player, familiar)) {
            notify(player, "message.typemoonaddon.shadow_transfer.target_lost");
            return false;
        }

        long now = player.serverLevel().getGameTime();
        TRANSFERS.put(player.getUUID(), new TransferState(familiarId, player.level().dimension(), now));
        player.serverLevel().playSound(
            null,
            player.blockPosition(),
            SoundEvents.WARDEN_HEARTBEAT,
            SoundSource.PLAYERS,
            0.8F,
            0.6F
        );
        notify(player, "message.typemoonaddon.shadow_transfer.started");
        return true;
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, TransferState>> iterator = TRANSFERS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TransferState> entry = iterator.next();
            TransferState state = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            ServerLevel level = server.getLevel(state.dimension);
            Entity entity = level == null ? null : level.getEntity(state.familiarId);
            if (player == null || level == null || player.level() != level || !validCaster(player)
                || !(entity instanceof ShadowFamiliarEntity familiar) || !validTarget(player, familiar)) {
                iterator.remove();
                if (player != null) {
                    notify(player, "message.typemoonaddon.shadow_transfer.target_lost");
                }
                continue;
            }

            long elapsed = level.getGameTime() - state.startedAt;
            if (elapsed % PARTICLE_INTERVAL_TICKS == 0L) {
                spawnChannelParticles(level, player, 10);
            }
            if (elapsed < CHANNEL_TICKS) {
                continue;
            }

            iterator.remove();
            Vec3 destination = findDestination(level, player, familiar);
            if (destination == null) {
                notify(player, "message.typemoonaddon.shadow_transfer.no_safe_destination");
                continue;
            }
            int cost = manaCost(player.distanceTo(familiar));
            if (!TypeMoonIntegration.tryConsumeMana(player, cost)) {
                notify(player, "message.typemoonaddon.not_enough_mana");
                continue;
            }

            spawnChannelParticles(level, player, 40);
            Vec3 origin = player.position();
            player.stopRiding();
            player.teleportTo(destination.x, destination.y, destination.z);
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0.0F;
            spawnChannelParticles(level, player, 40);
            level.playSound(null, BlockPos.containing(origin), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9F, 0.65F);
            level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9F, 0.8F);
            notify(player, "message.typemoonaddon.shadow_transfer.finished", cost);
        }
    }

    public static void interruptOnDamage(ServerPlayer player) {
        if (TRANSFERS.remove(player.getUUID()) != null) {
            notify(player, "message.typemoonaddon.shadow_transfer.interrupted");
        }
    }

    public static void playerChangedDimension(ServerPlayer player) {
        TRANSFERS.remove(player.getUUID());
    }

    public static void playerLoggedOut(ServerPlayer player) {
        TRANSFERS.remove(player.getUUID());
    }

    public static void serverStopping() {
        TRANSFERS.clear();
    }

    public static int manaCost(double distance) {
        return (int)Math.min(Integer.MAX_VALUE, Math.ceil(BASE_MANA_COST + Math.max(0.0D, distance) * MANA_PER_BLOCK));
    }

    private static List<ShadowFamiliarEntity> ownedFamiliars(ServerPlayer player) {
        List<ShadowFamiliarEntity> familiars = new ArrayList<>();
        for (Entity entity : player.serverLevel().getAllEntities()) {
            if (entity instanceof ShadowFamiliarEntity familiar && validTarget(player, familiar)) {
                familiars.add(familiar);
            }
        }
        familiars.sort(Comparator.comparingDouble(player::distanceToSqr));
        if (familiars.size() > MAX_TARGETS) {
            return List.copyOf(familiars.subList(0, MAX_TARGETS));
        }
        return List.copyOf(familiars);
    }

    private static boolean validTarget(ServerPlayer player, ShadowFamiliarEntity familiar) {
        return familiar.isAlive()
            && !familiar.isForming()
            && player.level() == familiar.level()
            && player.getUUID().equals(familiar.getOwnerId());
    }

    private static boolean validCaster(ServerPlayer player) {
        return player != null
            && player.isAlive()
            && !player.isSpectator()
            && !HolyGrailService.blocksAction(player)
            && TypeMoonIntegration.isShadowTransferLearned(player);
    }

    private static Vec3 findDestination(ServerLevel level, ServerPlayer player, ShadowFamiliarEntity familiar) {
        double minimumRadius = familiar.getBbWidth() * 0.5D + player.getBbWidth() * 0.5D + 0.75D;
        int centerY = familiar.getBlockY();
        for (int ring = 0; ring < 3; ring++) {
            double radius = minimumRadius + ring * 1.5D;
            for (int direction = 0; direction < 8; direction++) {
                double angle = Math.PI * 2.0D * direction / 8.0D;
                double x = familiar.getX() + Math.cos(angle) * radius;
                double z = familiar.getZ() + Math.sin(angle) * radius;
                Vec3 candidate = findSurface(level, player, x, z, centerY);
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static Vec3 findSurface(ServerLevel level, ServerPlayer player, double x, double z, int centerY) {
        if (!level.getChunkSource().hasChunk(
            SectionPos.blockToSectionCoord((int)Math.floor(x)),
            SectionPos.blockToSectionCoord((int)Math.floor(z))
        )) {
            return null;
        }
        for (int blockY = centerY + 4; blockY >= centerY - 6; blockY--) {
            if (blockY < level.getMinBuildHeight() || blockY >= level.getMaxBuildHeight()) {
                continue;
            }
            BlockPos supportPos = BlockPos.containing(x, blockY, z);
            if (!level.getWorldBorder().isWithinBounds(supportPos)) {
                continue;
            }
            VoxelShape shape = level.getBlockState(supportPos).getCollisionShape(level, supportPos);
            if (shape.isEmpty()) {
                continue;
            }
            double y = supportPos.getY() + shape.max(Direction.Axis.Y);
            AABB destinationBounds = player.getBoundingBox().move(
                x - player.getX(),
                y - player.getY(),
                z - player.getZ()
            );
            if (level.noCollision(player, destinationBounds) && !level.containsAnyLiquid(destinationBounds)) {
                return new Vec3(x, y, z);
            }
        }
        return null;
    }

    private static void spawnChannelParticles(ServerLevel level, ServerPlayer player, int count) {
        GrailParticleService.send(
            level,
            player,
            ParticleTypes.SQUID_INK,
            player.getX(),
            player.getY() + player.getBbHeight() * 0.5D,
            player.getZ(),
            count,
            player.getBbWidth() * 0.75D,
            player.getBbHeight() * 0.55D,
            player.getBbWidth() * 0.75D,
            0.025D
        );
    }

    private static void notify(ServerPlayer player, String key, Object... arguments) {
        player.displayClientMessage(Component.translatable(key, arguments), true);
    }

    private record TransferState(UUID familiarId, ResourceKey<Level> dimension, long startedAt) {
    }

    private ShadowTransferService() {
    }
}
