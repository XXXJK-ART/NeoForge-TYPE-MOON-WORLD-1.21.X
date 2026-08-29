package io.github.typemoonaddon.magic;

import io.github.typemoonaddon.data.ImaginarySpaceData.ShadowCommandMode;
import io.github.typemoonaddon.entity.BlackShadowEntity;
import io.github.typemoonaddon.entity.ShadowFamiliarEntity;
import io.github.typemoonaddon.registry.ModAttachments;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Applies player-selected familiar commands on the authoritative server. */
public final class BlackMudControlService {
    private static final double COMMAND_UPDATE_RADIUS = 128.0D;

    public static boolean selectMode(ServerPlayer player, ShadowCommandMode requestedMode) {
        if (!validCaster(player)) {
            return false;
        }
        ShadowCommandMode mode = requestedMode == null ? ShadowCommandMode.FREE : requestedMode;
        if (mode == ShadowCommandMode.ATTACK_AROUND) {
            return false;
        }
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        if (data.setSelectedShadowCommandMode(mode)) {
            player.syncData(ModAttachments.IMAGINARY_SPACE.get());
        }
        return true;
    }

    public static boolean cast(ServerPlayer player) {
        if (!validCaster(player)) {
            return false;
        }

        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        ShadowCommandMode selectedMode = data.selectedShadowCommandMode();
        if (selectedMode == ShadowCommandMode.ATTACK_AROUND) {
            selectedMode = ShadowCommandMode.FREE;
            data.setSelectedShadowCommandMode(selectedMode);
        }
        if (selectedMode == ShadowCommandMode.DISMISS) {
            BlackMudHuntService.end(player);
            data.advanceShadowDismissalGeneration();
            player.syncData(ModAttachments.IMAGINARY_SPACE.get());
            int dismissed = dismissLoadedSummons(player) + PollutionService.dismissCorruptedServants(player);
            player.displayClientMessage(Component.translatable(
                "message.typemoonaddon.black_mud_control.dismissed",
                dismissed
            ), true);
            playCommandSound(player, selectedMode);
            return true;
        }

        if (selectedMode == ShadowCommandMode.HUNT) {
            BlackMudHuntService.StartResult result = BlackMudHuntService.start(player);
            String messageKey = switch (result) {
                case STARTED -> "message.typemoonaddon.black_mud_control.hunt";
                case NO_TARGET -> "message.typemoonaddon.black_mud_control.hunt_no_target";
                case NO_FAMILIARS -> "message.typemoonaddon.black_mud_control.hunt_no_familiars";
            };
            player.displayClientMessage(Component.translatable(messageKey), true);
            if (result == BlackMudHuntService.StartResult.STARTED) {
                playCommandSound(player, selectedMode);
            }
            return true;
        }

        BlackMudHuntService.end(player);

        data.applySelectedShadowCommandMode();
        player.syncData(ModAttachments.IMAGINARY_SPACE.get());

        player.serverLevel().getEntitiesOfClass(
            ShadowFamiliarEntity.class,
            player.getBoundingBox().inflate(COMMAND_UPDATE_RADIUS),
            familiar -> player.getUUID().equals(familiar.getOwnerId())
        ).forEach(ShadowFamiliarEntity::onCommandModeApplied);
        player.serverLevel().getEntitiesOfClass(
            BlackShadowEntity.class,
            player.getBoundingBox().inflate(COMMAND_UPDATE_RADIUS),
            shadow -> player.getUUID().equals(shadow.getOwnerId())
        ).forEach(BlackShadowEntity::onCommandModeApplied);

        String messageKey = "message.typemoonaddon.black_mud_control." + selectedMode.serializedName();
        player.displayClientMessage(Component.translatable(messageKey), true);
        playCommandSound(player, selectedMode);
        return true;
    }

    public static boolean setAttackAround(ServerPlayer player, boolean enabled) {
        if (!validCaster(player)) {
            return false;
        }
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        if (data.setShadowAttackAround(enabled)) {
            player.syncData(ModAttachments.IMAGINARY_SPACE.get());
            player.displayClientMessage(Component.translatable(
                "message.typemoonaddon.black_mud_control.attack_around_" + (enabled ? "enabled" : "disabled")
            ), true);
            playCommandSound(player, ShadowCommandMode.ATTACK_AROUND);
        }
        return true;
    }

    public static void dismissForDispel(ServerPlayer player) {
        BlackMudHuntService.end(player);
        dismissLoadedSummons(player);
        PollutionService.killCorruptedServantsForDispel(player);
    }

    private static int dismissLoadedSummons(ServerPlayer player) {
        List<ShadowFamiliarEntity> familiars = new ArrayList<>();
        List<BlackShadowEntity> blackShadows = new ArrayList<>();
        for (ServerLevel level : player.server.getAllLevels()) {
            for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
                if (entity instanceof ShadowFamiliarEntity familiar
                    && player.getUUID().equals(familiar.getOwnerId())) {
                    familiars.add(familiar);
                } else if (entity instanceof BlackShadowEntity shadow
                    && player.getUUID().equals(shadow.getOwnerId())) {
                    blackShadows.add(shadow);
                }
            }
        }

        for (ShadowFamiliarEntity familiar : familiars) {
            if (familiar.level() instanceof ServerLevel level) {
                GrailParticleService.send(
                    level,
                    player,
                    ParticleTypes.SQUID_INK,
                    familiar.getX(),
                    familiar.getY() + familiar.getBbHeight() * 0.5D,
                    familiar.getZ(),
                    10,
                    familiar.getBbWidth() * 0.2D,
                    familiar.getBbHeight() * 0.2D,
                    familiar.getBbWidth() * 0.2D,
                    0.02D
                );
            }
            familiar.discard();
        }
        for (BlackShadowEntity shadow : blackShadows) {
            shadow.dismiss(player);
        }
        return familiars.size() + blackShadows.size();
    }

    private static void playCommandSound(ServerPlayer player, ShadowCommandMode mode) {
        player.serverLevel().playSound(
            null,
            player.blockPosition(),
            SoundEvents.SCULK_CATALYST_BLOOM,
            SoundSource.PLAYERS,
            0.65F,
            0.75F + mode.ordinal() * 0.08F
        );
    }

    private static boolean validCaster(ServerPlayer player) {
        return player.isAlive()
            && !player.isSpectator()
            && !HolyGrailService.blocksAction(player)
            && TypeMoonIntegration.isBlackMudControlLearned(player);
    }

    private BlackMudControlService() {
    }
}
