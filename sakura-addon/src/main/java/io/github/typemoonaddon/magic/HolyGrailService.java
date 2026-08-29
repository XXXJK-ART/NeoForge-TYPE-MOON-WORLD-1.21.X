package io.github.typemoonaddon.magic;

import io.github.typemoonaddon.config.GameplayConfig;
import io.github.typemoonaddon.entity.ShadowFamiliarEntity;
import io.github.typemoonaddon.registry.ModAttachments;
import io.github.typemoonaddon.registry.ModEntities;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative five-second false-death state for the Grail/Worm ritual. */
public final class HolyGrailService {
    private static final float FALSE_DEATH_HEALTH = 0.01F;
    private static final Map<UUID, FalseDeath> FALSE_DEATHS = new HashMap<>();

    public static boolean preventLethalDamage(ServerPlayer player, DamageSource source, float finalDamage) {
        if (!eligible(player) || SpiritualDamageService.isCollapse(source)
            || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || finalDamage <= 0.0F) {
            return false;
        }
        if (isFalseDead(player)) {
            return true;
        }
        if (finalDamage < player.getHealth()) {
            return false;
        }
        return beginFalseDeath(player);
    }

    public static boolean preventDirectMagicDeath(ServerPlayer player) {
        return eligible(player) && (isFalseDead(player) || beginFalseDeath(player));
    }

    public static boolean blocksDamage(ServerPlayer player, DamageSource source) {
        if (RuleBreakerDispelService.dispelFromDamage(player, source)) {
            return false;
        }
        return isFalseDead(player) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    public static boolean blocksAction(Entity entity) {
        return entity instanceof ServerPlayer player && isFalseDead(player);
    }

    public static boolean isFalseDead(ServerPlayer player) {
        return FALSE_DEATHS.containsKey(player.getUUID());
    }

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        Iterator<FalseDeath> iterator = FALSE_DEATHS.values().iterator();
        while (iterator.hasNext()) {
            FalseDeath state = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(state.playerId());
            if (player == null) {
                iterator.remove();
                continue;
            }
            if (!player.level().dimension().equals(state.dimension()) || now >= state.finishTick()) {
                iterator.remove();
                revive(player, state, true);
                continue;
            }

            player.setInvulnerable(true);
            player.setHealth(FALSE_DEATH_HEALTH);
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0.0F;
            if (player.position().distanceToSqr(state.anchor()) > 0.0025D) {
                player.teleportTo(state.anchor().x, state.anchor().y, state.anchor().z);
            }
            if (now % 10L == 0L) {
                GrailParticleService.send(
                    player.serverLevel(),
                    player,
                    ParticleTypes.SQUID_INK,
                    player.getX(),
                    player.getY() + 0.2D,
                    player.getZ(),
                    2,
                    0.25D,
                    0.1D,
                    0.25D,
                    0.0D
                );
            }
        }
    }

    public static void playerLoggedOut(ServerPlayer player) {
        FalseDeath state = FALSE_DEATHS.remove(player.getUUID());
        if (state != null) {
            revive(player, state, false);
        }
    }

    public static void dispel(ServerPlayer player) {
        FalseDeath state = FALSE_DEATHS.remove(player.getUUID());
        if (state != null) {
            revive(player, state, false);
        }
    }

    /** Clears transient state when bypass-invulnerability damage performs a real death. */
    public static void playerDied(ServerPlayer player) {
        FALSE_DEATHS.remove(player.getUUID());
    }

    public static void serverStopping(MinecraftServer server) {
        for (FalseDeath state : FALSE_DEATHS.values()) {
            ServerPlayer player = server.getPlayerList().getPlayer(state.playerId());
            if (player != null) {
                revive(player, state, false);
            }
        }
        FALSE_DEATHS.clear();
    }

    private static boolean beginFalseDeath(ServerPlayer player) {
        if (isFalseDead(player)) {
            return true;
        }
        long finishTick = player.server.overworld().getGameTime() + GameplayConfig.GRAIL_FAKE_DEATH_TICKS;
        FalseDeath state = new FalseDeath(
            player.getUUID(),
            player.level().dimension(),
            player.position(),
            finishTick,
            player.isInvulnerable()
        );
        FALSE_DEATHS.put(player.getUUID(), state);
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        if (data.protectionCooldownTicks() == 0 && data.protectionShield() > 0.0F) {
            data.activateProtection();
            player.syncData(ModAttachments.IMAGINARY_SPACE.get());
        }
        player.setHealth(FALSE_DEATH_HEALTH);
        player.setInvulnerable(true);
        player.setDeltaMovement(Vec3.ZERO);
        player.serverLevel().playSound(
            null,
            player.blockPosition(),
            SoundEvents.WARDEN_HEARTBEAT,
            SoundSource.PLAYERS,
            0.8F,
            0.5F
        );
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
            "message.typemoonaddon.holy_grail.false_death"
        ), false);
        return true;
    }

    private static void revive(ServerPlayer player, FalseDeath state, boolean notify) {
        player.setInvulnerable(state.wasInvulnerable());
        player.setHealth(player.getMaxHealth());
        player.setDeltaMovement(Vec3.ZERO);
        player.clearFire();
        if (notify) {
            summonRevivalFamiliars(player);
            GrailParticleService.send(
                player.serverLevel(),
                player,
                ParticleTypes.SQUID_INK,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                36,
                0.45D,
                0.8D,
                0.45D,
                0.15D
            );
            player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.TOTEM_USE,
                SoundSource.PLAYERS,
                1.0F,
                0.8F
            );
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "message.typemoonaddon.holy_grail.revived"
            ), false);
        }
    }

    private static void summonRevivalFamiliars(ServerPlayer player) {
        var level = player.serverLevel();
        int generation = player.getData(ModAttachments.IMAGINARY_SPACE.get()).shadowDismissalGeneration();
        for (int index = 0; index < 5; index++) {
            ShadowFamiliarEntity familiar = ModEntities.SHADOW_FAMILIAR.get().create(level);
            if (familiar == null) {
                continue;
            }
            familiar.setOwnerId(player.getUUID());
            familiar.setOwnerDismissalGeneration(generation);
            familiar.finalizeSpawn(
                level,
                level.getCurrentDifficultyAt(player.blockPosition()),
                MobSpawnType.MOB_SUMMONED,
                null
            );
            double angle = Math.PI * 2.0D * index / 5.0D;
            double radius = 3.0D + index * 0.35D;
            familiar.moveTo(
                player.getX() + Math.cos(angle) * radius,
                player.getY(),
                player.getZ() + Math.sin(angle) * radius,
                (float)(angle * 180.0D / Math.PI),
                0.0F
            );
            familiar.configureTemporaryRetaliator(3 * 60 * 20);
            if (!level.noCollision(familiar)) {
                familiar.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
            }
            if (!level.addFreshEntity(familiar)) {
                familiar.discard();
            }
        }
    }

    private static boolean eligible(ServerPlayer player) {
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        return player.isAlive()
            && !player.isSpectator()
            && data.grailWormAscended()
            && !data.grailErosionFull();
    }

    private record FalseDeath(
        UUID playerId,
        ResourceKey<Level> dimension,
        Vec3 anchor,
        long finishTick,
        boolean wasInvulnerable
    ) {
    }

    private HolyGrailService() {
    }
}
