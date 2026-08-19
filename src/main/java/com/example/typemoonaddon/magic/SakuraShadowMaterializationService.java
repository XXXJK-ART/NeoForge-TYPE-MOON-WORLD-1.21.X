package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.entity.SakuraShadowFamiliarEntity;
import com.example.typemoonaddon.registry.AddonAttachments;
import com.example.typemoonaddon.registry.AddonEntities;
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

public final class SakuraShadowMaterializationService {
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
        long now = player.serverLevel().getGameTime();
        ChargeState active = CHARGES.get(playerId);
        if (active != null) {
            active.lastHeldTick = now;
            return true;
        }
        SakuraShadowFamiliarEntity familiar = AddonEntities.SHADOW_FAMILIAR.get().create(player.serverLevel());
        if (familiar == null) {
            notify(player, "message.typemoonworld.materialization.spawn_failed");
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
        familiar.setOwnerDismissalGeneration(player.getData(AddonAttachments.IMAGINARY_SPACE.get()).shadowDismissalGeneration());
        familiar.setSummonSize(SakuraShadowFamiliarEntity.MIN_SUMMON_SIZE);
        familiar.setForming(true);
        familiar.setInvulnerable(true);
        if (!player.serverLevel().addFreshEntity(familiar)) {
            notify(player, "message.typemoonworld.materialization.spawn_failed");
            return false;
        }
        CHARGES.put(playerId, new ChargeState(familiar.getUUID(), player.level().dimension(), now));
        SakuraParticleService.send(player.serverLevel(), ParticleTypes.SQUID_INK, familiar.getX(), familiar.getY() + 0.5D, familiar.getZ(), 24, 0.45D, 0.45D, 0.45D, 0.02D);
        player.serverLevel().playSound(null, familiar.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.8F, 0.7F);
        notify(player, "message.typemoonworld.materialization.started");
        return true;
    }

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

    public static void maintainHeld(ServerPlayer player) {
        ChargeState state = CHARGES.get(player.getUUID());
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
            Entity entity = level == null ? null : level.getEntity(state.familiarId);
            if (!(entity instanceof SakuraShadowFamiliarEntity familiar)) {
                iterator.remove();
                RELEASE_LOCKED.add(playerId);
                continue;
            }
            long now = level.getGameTime();
            if (!validCaster(player) || player.level() != level || now - state.lastHeldTick > HEARTBEAT_TIMEOUT_TICKS) {
                iterator.remove();
                RELEASE_LOCKED.add(playerId);
                finish(server, player, state, true, null);
                continue;
            }
            float currentSize = familiar.getSummonSize();
            if (currentSize >= SakuraShadowFamiliarEntity.MAX_SUMMON_SIZE) {
                iterator.remove();
                RELEASE_LOCKED.add(playerId);
                finish(server, player, state, true, null);
                continue;
            }
            if (!SakuraTypeMoonIntegration.tryConsumeMana(player, MANA_PER_GROWTH_TICK)) {
                iterator.remove();
                RELEASE_LOCKED.add(playerId);
                finish(server, player, state, true, "message.typemoonworld.materialization.not_enough_mana");
                continue;
            }
            familiar.setSummonSize(Math.min(SakuraShadowFamiliarEntity.MAX_SUMMON_SIZE, currentSize + GROWTH_PER_TICK));
            if (familiar.tickCount % 4 == 0) {
                SakuraParticleService.send(level, ParticleTypes.PORTAL, familiar.getX(), familiar.getY() + familiar.getSummonSize() * 0.5D, familiar.getZ(), 4, familiar.getSummonSize() * 0.28D, familiar.getSummonSize() * 0.28D, familiar.getSummonSize() * 0.28D, 0.02D);
            }
        }
    }

    public static void playerUnavailable(ServerPlayer player) {
        UUID playerId = player.getUUID();
        RELEASE_LOCKED.remove(playerId);
        ChargeState state = CHARGES.remove(playerId);
        if (state != null) {
            finish(player.server, player, state, false, null);
        }
    }

    public static void entityLeavingLevel(Entity entity) {
        if (!(entity instanceof SakuraShadowFamiliarEntity familiar)) {
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
        for (ChargeState state : CHARGES.values()) {
            ServerLevel level = server.getLevel(state.dimension);
            Entity entity = level == null ? null : level.getEntity(state.familiarId);
            if (entity instanceof SakuraShadowFamiliarEntity familiar) {
                familiar.setForming(false);
                familiar.setInvulnerable(false);
            }
        }
        CHARGES.clear();
        RELEASE_LOCKED.clear();
    }

    private static void finish(MinecraftServer server, ServerPlayer player, ChargeState state, boolean keepReleaseLock, String messageKey) {
        ServerLevel level = server.getLevel(state.dimension);
        Entity entity = level == null ? null : level.getEntity(state.familiarId);
        if (!(entity instanceof SakuraShadowFamiliarEntity familiar)) {
            return;
        }
        familiar.setForming(false);
        familiar.setInvulnerable(false);
        familiar.setHealth(familiar.getMaxHealth());
        level.playSound(null, familiar.blockPosition(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 0.8F, 0.8F + familiar.getSummonSize() * 0.04F);
        if (player != null) {
            if (!keepReleaseLock) {
                RELEASE_LOCKED.remove(player.getUUID());
            }
            if (messageKey != null) {
                notify(player, messageKey);
            }
            player.displayClientMessage(Component.translatable("message.typemoonworld.materialization.finished", String.format(java.util.Locale.ROOT, "%.1f", familiar.getSummonSize())), true);
        }
    }

    private static boolean validCaster(ServerPlayer player) {
        return player != null
                && player.isAlive()
                && !player.isSpectator()
                && !player.getData(AddonAttachments.IMAGINARY_SPACE.get()).grailErosionPending()
                && SakuraTypeMoonIntegration.isShadowMaterializationLearned(player);
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

    private SakuraShadowMaterializationService() {
    }
}
