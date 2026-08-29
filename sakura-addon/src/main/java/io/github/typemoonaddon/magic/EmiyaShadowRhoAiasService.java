package io.github.typemoonaddon.magic;

import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.config.GameplayConfig;
import io.github.typemoonaddon.entity.BlackShadowEntity;
import io.github.typemoonaddon.entity.ShadowPiercingRhoAiasEntity;
import io.github.typemoonaddon.registry.ModEntities;
import io.github.typemoonaddon.shadowlogic.entity.ShadowArtRibbonEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Runs the pinned Archer chant and the imported functional Rho Aias sequence. */
public final class EmiyaShadowRhoAiasService {
    private static final ResourceLocation EMIYA_ARCHER = ResourceLocation.fromNamespaceAndPath(
        "typemoonworld",
        "emiya_archer"
    );
    private static final String SESSION = "TypeMoonAddonShadowRhoAiasSession";
    private static final String THREAT = "TypeMoonAddonShadowRhoAiasThreat";
    private static final String CHANT_STARTED = "TypeMoonAddonShadowRhoAiasChantStarted";
    private static final String SECOND_LINE = "TypeMoonAddonShadowRhoAiasSecondLine";
    private static final String SOUND_ACTIVE = "TypeMoonAddonShadowRhoAiasSoundActive";
    private static final String CHANT_INTERRUPTED = "TypeMoonAddonShadowRhoAiasChantInterrupted";
    private static final String SHIELD = "TypeMoonAddonShadowRhoAiasShield";
    private static final String SHIELD_SPAWNED = "TypeMoonAddonShadowRhoAiasShieldSpawned";
    private static final String SHIELD_VOICE_PLAYED = "TypeMoonAddonShadowRhoAiasShieldVoicePlayed";
    private static final String COMPLETED = "TypeMoonAddonShadowRhoAiasCompleted";
    private static final double SEARCH_PADDING = 4.0D;
    private static final double AUDIENCE_RANGE_SQR = 64.0D * 64.0D;

    public static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (LivingEntity archer : findArchers(level)) {
                tickArcher(level, archer);
            }
        }
    }

    public static void serverStopping(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (LivingEntity archer : findArchers(level)) {
                if (archer.getPersistentData().contains(SESSION)) {
                    cancel(level, archer);
                }
            }
        }
    }

    private static List<LivingEntity> findArchers(ServerLevel level) {
        List<LivingEntity> archers = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity living
                && EMIYA_ARCHER.equals(BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()))) {
                archers.add(living);
            }
        }
        return archers;
    }

    private static void tickArcher(ServerLevel level, LivingEntity archer) {
        Pinning pinning = findPinning(level, archer);
        CompoundTag data = archer.getPersistentData();
        if (pinning == null) {
            if (!data.contains(SESSION) || data.getBoolean(COMPLETED)) {
                return;
            }
            Entity storedThreat = data.hasUUID(THREAT) ? level.getEntity(data.getUUID(THREAT)) : null;
            if (!(storedThreat instanceof BlackShadowEntity blackShadow) || !blackShadow.isAlive()) {
                cancel(level, archer);
                return;
            }
            pinning = new Pinning(blackShadow, data.getLong(SESSION));
        }

        long session = pinning.startGameTime();
        if (!data.contains(SESSION) || data.getLong(SESSION) != session) {
            clearArtifacts(level, archer);
            clearState(data);
            data.putLong(SESSION, session);
            data.putUUID(THREAT, pinning.owner().getUUID());
        }
        if (data.getBoolean(COMPLETED)) {
            return;
        }

        long performanceTick = level.getGameTime() - session
            - GameplayConfig.EMIYA_SHADOW_PIN_ANIMATION_DELAY_TICKS;
        int totalTicks = GameplayConfig.EMIYA_SHADOW_CHANT_TICKS
            + GameplayConfig.EMIYA_SHADOW_RHO_AIAS_TOTAL_TICKS;
        if (performanceTick < 0L) {
            return;
        }
        if (performanceTick >= totalTicks) {
            SpiritualDamageService.raiseTo(
                archer,
                GameplayConfig.EMIYA_SHADOW_POST_RHO_AIAS_SPIRITUAL_DAMAGE_PERCENT
            );
            clearArtifacts(level, archer);
            data.putBoolean(COMPLETED, true);
            return;
        }

        if (!data.getBoolean(CHANT_STARTED)) {
            data.putBoolean(CHANT_STARTED, true);
            data.putBoolean(SOUND_ACTIVE, true);
            playVoice(level, archer, SoundEvents.BEACON_ACTIVATE);
            showLine(level, archer, "message.typemoonaddon.emiya_shadow_chant_1");
        }
        if (performanceTick >= GameplayConfig.EMIYA_SHADOW_CHANT_LINE_TICKS
            && !data.getBoolean(SECOND_LINE)) {
            data.putBoolean(SECOND_LINE, true);
            showLine(level, archer, "message.typemoonaddon.emiya_shadow_chant_2");
        }
        if (performanceTick >= GameplayConfig.EMIYA_SHADOW_CHANT_TICKS) {
            if (!data.getBoolean(CHANT_INTERRUPTED)) {
                data.putBoolean(CHANT_INTERRUPTED, true);
                stopChant(level, archer);
                showLine(level, archer, "message.typemoonaddon.emiya_shadow_chant_interrupted");
            }
            if (!data.getBoolean(SHIELD_SPAWNED) && !hasActiveShield(level, data)) {
                data.remove(SHIELD);
                spawnShield(level, archer, pinning.owner());
            }
        }
    }

    @Nullable
    private static Pinning findPinning(ServerLevel level, LivingEntity target) {
        Map<UUID, PinAccumulator> byOwner = new HashMap<>();
        for (ShadowArtRibbonEntity ribbon : level.getEntitiesOfClass(
            ShadowArtRibbonEntity.class,
            target.getBoundingBox().inflate(SEARCH_PADDING),
            candidate -> candidate.action() == ShadowArtRibbonEntity.PINNED
                && candidate.targetEntityId() == target.getId()
        )) {
            UUID ownerId = ribbon.ownerId();
            if (ownerId == null || ribbon.actionStartGameTime() < 0L) {
                continue;
            }
            Entity owner = ribbon.ownerEntityId() < 0 ? null : level.getEntity(ribbon.ownerEntityId());
            if (!(owner instanceof BlackShadowEntity) || !ownerId.equals(owner.getUUID())) {
                owner = level.getEntity(ownerId);
            }
            if (!(owner instanceof BlackShadowEntity blackShadow)) {
                continue;
            }
            PinAccumulator accumulator = byOwner.computeIfAbsent(ownerId, ignored -> new PinAccumulator(blackShadow));
            accumulator.count++;
            accumulator.newestStart = Math.max(accumulator.newestStart, ribbon.actionStartGameTime());
        }

        Pinning selected = null;
        for (PinAccumulator accumulator : byOwner.values()) {
            if (accumulator.count < GameplayConfig.SHADOW_ART_HOLD_RIBBON_COUNT) {
                continue;
            }
            if (selected == null || accumulator.newestStart > selected.startGameTime()) {
                selected = new Pinning(accumulator.owner, accumulator.newestStart);
            }
        }
        return selected;
    }

    private static void spawnShield(ServerLevel level, LivingEntity archer, BlackShadowEntity threat) {
        ShadowPiercingRhoAiasEntity shield = ModEntities.SHADOW_PIERCING_RHO_AIAS.get().create(level);
        if (shield == null) {
            TypeMoonAddon.LOGGER.error("Rho Aias sequence could not create its shield entity for Archer {}", archer.getUUID());
            return;
        }
        shield.initialize(archer, threat);
        if (level.addFreshEntity(shield)) {
            TypeMoonAddon.LOGGER.info(
                "Rho Aias sequence spawned shield entity id={} uuid={} at [{}, {}, {}] for Archer {}",
                shield.getId(),
                shield.getUUID(),
                shield.getX(),
                shield.getY(),
                shield.getZ(),
                archer.getUUID()
            );
            CompoundTag data = archer.getPersistentData();
            data.putUUID(SHIELD, shield.getUUID());
            data.putBoolean(SHIELD_SPAWNED, true);
            if (!data.getBoolean(SHIELD_VOICE_PLAYED)) {
                data.putBoolean(SHIELD_VOICE_PLAYED, true);
                playVoice(level, archer, SoundEvents.AMETHYST_BLOCK_RESONATE);
            }
        } else {
            TypeMoonAddon.LOGGER.error("Rho Aias sequence failed to add its shield entity for Archer {}", archer.getUUID());
        }
    }

    private static boolean hasActiveShield(ServerLevel level, CompoundTag data) {
        if (!data.hasUUID(SHIELD)) {
            return false;
        }
        Entity shield = level.getEntity(data.getUUID(SHIELD));
        return shield instanceof ShadowPiercingRhoAiasEntity && !shield.isRemoved();
    }

    private static void cancel(ServerLevel level, LivingEntity archer) {
        clearArtifacts(level, archer);
        archer.getPersistentData().putBoolean(COMPLETED, true);
    }

    private static void clearArtifacts(ServerLevel level, LivingEntity archer) {
        stopChant(level, archer);
        CompoundTag data = archer.getPersistentData();
        if (data.hasUUID(SHIELD)) {
            Entity shield = level.getEntity(data.getUUID(SHIELD));
            if (shield instanceof ShadowPiercingRhoAiasEntity) {
                shield.discard();
            }
            data.remove(SHIELD);
        }
    }

    private static void stopChant(ServerLevel level, LivingEntity archer) {
        CompoundTag data = archer.getPersistentData();
        if (!data.getBoolean(SOUND_ACTIVE)) {
            return;
        }
        data.putBoolean(SOUND_ACTIVE, false);
        SoundEvent sound = SoundEvents.BEACON_ACTIVATE;
        ResourceLocation soundId = BuiltInRegistries.SOUND_EVENT.getKey(sound);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(archer) <= AUDIENCE_RANGE_SQR) {
                player.connection.send(new ClientboundStopSoundPacket(soundId, SoundSource.HOSTILE));
            }
        }
    }

    private static void playVoice(ServerLevel level, LivingEntity archer, SoundEvent sound) {
        level.playSound(null, archer.getX(), archer.getY(), archer.getZ(), sound, SoundSource.HOSTILE, 4.0F, 1.0F);
    }

    private static void showLine(ServerLevel level, LivingEntity archer, String translationKey) {
        Component line = Component.translatable(translationKey);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(archer) <= AUDIENCE_RANGE_SQR) {
                player.displayClientMessage(line, true);
            }
        }
    }

    private static void clearState(CompoundTag data) {
        data.remove(SESSION);
        data.remove(THREAT);
        data.remove(CHANT_STARTED);
        data.remove(SECOND_LINE);
        data.remove(SOUND_ACTIVE);
        data.remove(CHANT_INTERRUPTED);
        data.remove(SHIELD);
        data.remove(SHIELD_SPAWNED);
        data.remove(SHIELD_VOICE_PLAYED);
        data.remove(COMPLETED);
    }

    private static final class PinAccumulator {
        private final BlackShadowEntity owner;
        private int count;
        private long newestStart = -1L;

        private PinAccumulator(BlackShadowEntity owner) {
            this.owner = owner;
        }
    }

    private record Pinning(BlackShadowEntity owner, long startGameTime) {
    }

    private EmiyaShadowRhoAiasService() {
    }
}
