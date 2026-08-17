package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.data.ImaginarySpaceData;
import com.example.typemoonaddon.entity.SakuraBlackShadowEntity;
import com.example.typemoonaddon.entity.SakuraShadowFamiliarEntity;
import com.example.typemoonaddon.registry.AddonAttachments;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

public final class SakuraBlackMudControlService {
    private static final double COMMAND_UPDATE_RADIUS = 128.0D;

    public static boolean selectMode(ServerPlayer player, ImaginarySpaceData.ShadowCommandMode requestedMode) {
        if (!validCaster(player)) {
            return false;
        }
        ImaginarySpaceData.ShadowCommandMode mode = requestedMode == null ? ImaginarySpaceData.ShadowCommandMode.FREE : requestedMode;
        if (mode == ImaginarySpaceData.ShadowCommandMode.ATTACK_AROUND) {
            return setAttackAround(player, !player.getData(AddonAttachments.IMAGINARY_SPACE.get()).shadowAttackAround());
        }
        var data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        if (data.setSelectedShadowCommandMode(mode)) {
            player.syncData(AddonAttachments.IMAGINARY_SPACE.get());
        }
        return true;
    }

    public static boolean cast(ServerPlayer player) {
        if (!validCaster(player)) {
            return false;
        }
        var data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        ImaginarySpaceData.ShadowCommandMode selectedMode = data.selectedShadowCommandMode();
        if (selectedMode == ImaginarySpaceData.ShadowCommandMode.DISMISS) {
            SakuraBlackMudHuntService.end(player);
            data.advanceShadowDismissalGeneration();
            player.syncData(AddonAttachments.IMAGINARY_SPACE.get());
            int dismissed = dismissLoadedSummons(player);
            player.displayClientMessage(Component.translatable("message.typemoonworld.black_mud_control.dismissed", dismissed), true);
            playCommandSound(player, selectedMode);
            return true;
        }
        if (selectedMode == ImaginarySpaceData.ShadowCommandMode.HUNT) {
            SakuraBlackMudHuntService.StartResult result = SakuraBlackMudHuntService.start(player);
            String messageKey = switch (result) {
                case STARTED -> "message.typemoonworld.black_mud_control.hunt";
                case NO_TARGET -> "message.typemoonworld.black_mud_control.hunt_no_target";
                case NO_FAMILIARS -> "message.typemoonworld.black_mud_control.hunt_no_familiars";
            };
            player.displayClientMessage(Component.translatable(messageKey), true);
            if (result == SakuraBlackMudHuntService.StartResult.STARTED) {
                playCommandSound(player, selectedMode);
            }
            return true;
        }
        SakuraBlackMudHuntService.end(player);
        data.applySelectedShadowCommandMode();
        player.syncData(AddonAttachments.IMAGINARY_SPACE.get());
        player.serverLevel().getEntitiesOfClass(SakuraShadowFamiliarEntity.class, player.getBoundingBox().inflate(COMMAND_UPDATE_RADIUS), familiar -> player.getUUID().equals(familiar.getOwnerId())).forEach(SakuraShadowFamiliarEntity::onCommandModeApplied);
        player.serverLevel().getEntitiesOfClass(SakuraBlackShadowEntity.class, player.getBoundingBox().inflate(COMMAND_UPDATE_RADIUS), shadow -> player.getUUID().equals(shadow.getOwnerId())).forEach(SakuraBlackShadowEntity::onCommandModeApplied);
        player.displayClientMessage(Component.translatable("message.typemoonworld.black_mud_control." + selectedMode.serializedName()), true);
        playCommandSound(player, selectedMode);
        return true;
    }

    public static boolean setAttackAround(ServerPlayer player, boolean enabled) {
        if (!validCaster(player)) {
            return false;
        }
        var data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        if (data.setShadowAttackAround(enabled)) {
            player.syncData(AddonAttachments.IMAGINARY_SPACE.get());
            player.displayClientMessage(Component.translatable("message.typemoonworld.black_mud_control.attack_around_" + (enabled ? "enabled" : "disabled")), true);
            playCommandSound(player, ImaginarySpaceData.ShadowCommandMode.ATTACK_AROUND);
        }
        return true;
    }

    private static int dismissLoadedSummons(ServerPlayer player) {
        List<Entity> summons = new ArrayList<>();
        for (ServerLevel level : player.server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof SakuraShadowFamiliarEntity familiar && player.getUUID().equals(familiar.getOwnerId())) {
                    summons.add(familiar);
                } else if (entity instanceof SakuraBlackShadowEntity shadow && player.getUUID().equals(shadow.getOwnerId())) {
                    summons.add(shadow);
                }
            }
        }
        for (Entity entity : summons) {
            if (entity.level() instanceof ServerLevel level) {
                SakuraParticleService.send(level, ParticleTypes.SQUID_INK, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), 10, entity.getBbWidth() * 0.2D, entity.getBbHeight() * 0.2D, entity.getBbWidth() * 0.2D, 0.02D);
            }
            entity.discard();
        }
        return summons.size();
    }

    private static void playCommandSound(ServerPlayer player, ImaginarySpaceData.ShadowCommandMode mode) {
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 0.65F, 0.75F + mode.ordinal() * 0.08F);
    }

    private static boolean validCaster(ServerPlayer player) {
        return player != null && player.isAlive() && !player.isSpectator() && SakuraTypeMoonIntegration.isBlackMudControlLearned(player);
    }

    private SakuraBlackMudControlService() {
    }
}
