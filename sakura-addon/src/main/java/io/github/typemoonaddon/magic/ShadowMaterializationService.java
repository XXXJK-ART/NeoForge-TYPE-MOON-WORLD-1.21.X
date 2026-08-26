package io.github.typemoonaddon.magic;

import io.github.typemoonaddon.entity.ShadowFamiliarEntity;
import io.github.typemoonaddon.registry.ModAttachments;
import io.github.typemoonaddon.registry.ModEntities;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
import net.minecraft.world.phys.Vec3;

/** Server-authoritative hold-to-grow summoning for Shadow Materialization. */
public final class ShadowMaterializationService {
    private static final float GROWTH_PER_TICK = 0.05F;
    private static final double MANA_PER_GROWTH_TICK = 1.0D;
    private static final long HEARTBEAT_TIMEOUT_TICKS = 6L;
    private static final Map<UUID, ChargeState> CHARGES = new HashMap<>();
    private static final Set<UUID> RELEASE_LOCKED = new HashSet<>();

    public static boolean beginCharging(ServerPlayer player) {
        if (!validCaster(player)) {
            return false;
        }
        UUID playerId = player.getUUID();
        if (RELEASE_LOCKED.contains(playerId)) {
            return true;
        }

        ChargeState active = CHARGES.get(playerId);
        long now = player.serverLevel().getGameTime();
        if (active != null) {
            active.lastHeldTick = now;
            return true;
        }

        ShadowFamiliarEntity familiar = ModEntities.SHADOW_FAMILIAR.get().create(player.serverLevel());
        if (familiar == null) {
            notify(player, "message.typemoonaddon.materialization.spawn_failed");
            return false;
        }

        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 spawn = player.position().add(horizontal.normalize().scale(3.0D)).add(0.0D, 0.2D, 0.0D);
        familiar.moveTo(spawn.x, spawn.y, spawn.z, player.getYRot(), 0.0F);
        familiar.setOwnerId(playerId);
        familiar.setOwnerDismissalGeneration(
            player.getData(ModAttachments.IMAGINARY_SPACE.get()).shadowDismissalGeneration()
        );
        familiar.setSummonSize(ShadowFamiliarEntity.MIN_SUMMON_SIZE);
        familiar.setForming(true);
        familiar.setInvulnerable(true);
        if (!player.serverLevel().addFreshEntity(familiar)) {
            notify(player, "message.typemoonaddon.materialization.spawn_failed");
            return false;
        }

        CHARGES.put(playerId, new ChargeState(familiar.getUUID(), player.level().dimension(), now));
        GrailParticleService.send(
            player.serverLevel(),
            player,
            ParticleTypes.SQUID_INK,
            familiar.getX(),
            familiar.getY() + 0.5D,
            familiar.getZ(),
            24,
            0.45D,
            0.45D,
            0.45D,
            0.02D
        );
        player.serverLevel().playSound(
            null,
            familiar.blockPosition(),
            SoundEvents.WARDEN_HEARTBEAT,
            SoundSource.PLAYERS,
            0.8F,
            0.7F
        );
        notify(player, "message.typemoonaddon.materialization.started");
        return true;
    }

    /** Updates only an executor-created charge; packets cannot initiate the skill. */
    public static void updateHeld(ServerPlayer player, boolean held) {
        UUID playerId = player.getUUID();
        if (!held) {
            RELEASE_LOCKED.remove(playerId);
            ChargeState state = CHARGES.remove(playerId);
            if (state != null) {
                finish(player.server, player, state, false, null);
            }
            return;
        }

        ChargeState state = CHARGES.get(playerId);
        if (state != null) {
            state.lastHeldTick = player.serverLevel().getGameTime();
        }
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, ChargeState>> iterator = CHARGES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ChargeState> entry = iterator.next();
            UUID playerId = entry.getKey();
            ChargeState state = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            ServerLevel level = server.getLevel(state.dimension);
            Entity rawEntity = level == null ? null : level.getEntity(state.familiarId);
            if (!(rawEntity instanceof ShadowFamiliarEntity familiar)) {
                iterator.remove();
                RELEASE_LOCKED.add(playerId);
                continue;
            }

            long now = level.getGameTime();
            if (!validCaster(player)
                || player.level() != level
                || now - state.lastHeldTick > HEARTBEAT_TIMEOUT_TICKS) {
                iterator.remove();
                RELEASE_LOCKED.add(playerId);
                finish(server, player, state, true, null);
                continue;
            }

            float currentSize = familiar.getSummonSize();
            if (currentSize >= ShadowFamiliarEntity.MAX_SUMMON_SIZE) {
                iterator.remove();
                RELEASE_LOCKED.add(playerId);
                finish(server, player, state, true, null);
                continue;
            }
            if (!TypeMoonIntegration.tryConsumeMana(player, MANA_PER_GROWTH_TICK)) {
                iterator.remove();
                RELEASE_LOCKED.add(playerId);
                finish(
                    server,
                    player,
                    state,
                    true,
                    "message.typemoonaddon.materialization.not_enough_mana"
                );
                continue;
            }

            float nextSize = Math.min(ShadowFamiliarEntity.MAX_SUMMON_SIZE, currentSize + GROWTH_PER_TICK);
            familiar.setSummonSize(nextSize);
            if (familiar.tickCount % 4 == 0) {
                GrailParticleService.send(
                    level,
                    player,
                    ParticleTypes.PORTAL,
                    familiar.getX(),
                    familiar.getY() + nextSize * 0.5D,
                    familiar.getZ(),
                    4,
                    nextSize * 0.28D,
                    nextSize * 0.28D,
                    nextSize * 0.28D,
                    0.02D
                );
            }
        }
    }

    public static void playerLoggedOut(ServerPlayer player) {
        UUID playerId = player.getUUID();
        RELEASE_LOCKED.remove(playerId);
        ChargeState state = CHARGES.remove(playerId);
        if (state != null) {
            finish(player.server, player, state, false, null);
        }
    }

    public static void entityLeavingLevel(Entity entity) {
        if (!(entity instanceof ShadowFamiliarEntity familiar)) {
            return;
        }
        CHARGES.entrySet().removeIf(entry -> {
            if (!entry.getValue().familiarId.equals(familiar.getUUID())) {
                return false;
            }
            RELEASE_LOCKED.add(entry.getKey());
            return true;
        });
    }

    public static void serverStopping(MinecraftServer server) {
        for (Map.Entry<UUID, ChargeState> entry : CHARGES.entrySet()) {
            ServerLevel level = server.getLevel(entry.getValue().dimension);
            Entity entity = level == null ? null : level.getEntity(entry.getValue().familiarId);
            if (entity instanceof ShadowFamiliarEntity familiar) {
                familiar.setForming(false);
                familiar.setInvulnerable(false);
            }
        }
        CHARGES.clear();
        RELEASE_LOCKED.clear();
    }

    private static void finish(
        MinecraftServer server,
        ServerPlayer player,
        ChargeState state,
        boolean keepReleaseLock,
        String messageKey
    ) {
        ServerLevel level = server.getLevel(state.dimension);
        Entity entity = level == null ? null : level.getEntity(state.familiarId);
        if (!(entity instanceof ShadowFamiliarEntity familiar)) {
            return;
        }
        familiar.setForming(false);
        familiar.setInvulnerable(false);
        familiar.setHealth(familiar.getMaxHealth());
        level.playSound(
            null,
            familiar.blockPosition(),
            SoundEvents.SCULK_CATALYST_BLOOM,
            SoundSource.PLAYERS,
            0.8F,
            0.8F + familiar.getSummonSize() * 0.04F
        );
        if (player != null) {
            if (!keepReleaseLock) {
                RELEASE_LOCKED.remove(player.getUUID());
            }
            if (messageKey != null) {
                notify(player, messageKey);
            }
            player.displayClientMessage(Component.translatable(
                "message.typemoonaddon.materialization.finished",
                String.format(java.util.Locale.ROOT, "%.1f", familiar.getSummonSize())
            ), true);
        }
    }

    private static boolean validCaster(ServerPlayer player) {
        return player != null
            && player.isAlive()
            && !player.isSpectator()
            && !HolyGrailService.blocksAction(player)
            && TypeMoonIntegration.isShadowMaterializationLearned(player);
    }

    private static void notify(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    private static final class ChargeState {
        private final UUID familiarId;
        private final ResourceKey<Level> dimension;
        private long lastHeldTick;

        private ChargeState(UUID familiarId, ResourceKey<Level> dimension, long lastHeldTick) {
            this.familiarId = familiarId;
            this.dimension = dimension;
            this.lastHeldTick = lastHeldTick;
        }
    }

    private ShadowMaterializationService() {
    }
}
