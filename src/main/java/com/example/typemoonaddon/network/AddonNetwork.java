package com.example.typemoonaddon.network;

import com.example.typemoonaddon.client.ClientEffects;
import com.example.typemoonaddon.client.ClientNightShadowCamera;
import com.example.typemoonaddon.magic.SakuraShadowMaterializationService;
import com.example.typemoonaddon.shadowlogic.network.ShadowBindingEffectPayload;
import com.example.typemoonaddon.storage.StorageService;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class AddonNetwork {
    private AddonNetwork() {
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (player == null || player instanceof FakePlayer || payload == null
                || !player.connection.hasChannel(payload.type())) {
            return;
        }
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendNear(ServerLevel level, double x, double y, double z,
                                double radius, CustomPacketPayload payload) {
        if (level == null || payload == null || radius <= 0.0D) {
            return;
        }
        double radiusSqr = radius * radius;
        for (ServerPlayer player : level.players()) {
            double dx = player.getX() - x;
            double dy = player.getY() - y;
            double dz = player.getZ() - z;
            if (dx * dx + dy * dy + dz * dz <= radiusSqr) {
                sendToPlayer(player, payload);
            }
        }
    }

    public static void registerPayloads(PayloadRegistrar registrar) {
        registrar.playToServer(
                OpenSpacePayload.TYPE,
                OpenSpacePayload.STREAM_CODEC,
                AddonNetwork::handleOpenSpace
        );
        registrar.playToServer(
                BlockAbsorptionHoldPayload.TYPE,
                BlockAbsorptionHoldPayload.STREAM_CODEC,
                AddonNetwork::handleBlockAbsorptionHold
        );
        registrar.playToServer(
                ShadowMaterializationHoldPayload.TYPE,
                ShadowMaterializationHoldPayload.STREAM_CODEC,
                AddonNetwork::handleShadowMaterializationHold
        );
        registrar.playToServer(
                StorageCastInputPayload.TYPE,
                StorageCastInputPayload.STREAM_CODEC,
                StorageCastInputPayload::handle
        );
        registrar.playToServer(
                StorageModeSwitchPayload.TYPE,
                StorageModeSwitchPayload.STREAM_CODEC,
                StorageModeSwitchPayload::handle
        );
        registrar.playToServer(
                OriasCastInputPayload.TYPE,
                OriasCastInputPayload.STREAM_CODEC,
                OriasCastInputPayload::handle
        );
        registrar.playToServer(
                StormCastInputPayload.TYPE,
                StormCastInputPayload.STREAM_CODEC,
                StormCastInputPayload::handle
        );
        registrar.playToServer(
                AndrephiusCastInputPayload.TYPE,
                AndrephiusCastInputPayload.STREAM_CODEC,
                AndrephiusCastInputPayload::handle
        );
        registrar.playToServer(
                AndrasiasCastInputPayload.TYPE,
                AndrasiasCastInputPayload.STREAM_CODEC,
                AndrasiasCastInputPayload::handle
        );
        registrar.playToServer(
                ZaganCastInputPayload.TYPE,
                ZaganCastInputPayload.STREAM_CODEC,
                ZaganCastInputPayload::handle
        );
        registrar.playToServer(
                ImaginarySpaceModeSwitchPayload.TYPE,
                ImaginarySpaceModeSwitchPayload.STREAM_CODEC,
                ImaginarySpaceModeSwitchPayload::handle
        );
        registrar.playToServer(
                ImaginarySpaceMovementPayload.TYPE,
                ImaginarySpaceMovementPayload.STREAM_CODEC,
                ImaginarySpaceMovementPayload::handle
        );
        registrar.playToServer(
                ImaginaryDiveDepthPayload.TYPE,
                ImaginaryDiveDepthPayload.STREAM_CODEC,
                ImaginaryDiveDepthPayload::handle
        );
        registrar.playToServer(
                AirflowBladeCastInputPayload.TYPE,
                AirflowBladeCastInputPayload.STREAM_CODEC,
                AirflowBladeCastInputPayload::handle
        );
        registrar.playToServer(
                AirflowBladeModeSwitchPayload.TYPE,
                AirflowBladeModeSwitchPayload.STREAM_CODEC,
                AirflowBladeModeSwitchPayload::handle
        );
        registrar.playToServer(
                SetImaginaryModePayload.TYPE,
                SetImaginaryModePayload.STREAM_CODEC,
                SetImaginaryModePayload::handle
        );
        registrar.playToServer(
                SetShadowCommandModePayload.TYPE,
                SetShadowCommandModePayload.STREAM_CODEC,
                SetShadowCommandModePayload::handle
        );
        registrar.playToServer(
                SetShadowAttackPayload.TYPE,
                SetShadowAttackPayload.STREAM_CODEC,
                SetShadowAttackPayload::handle
        );
        registrar.playToServer(
                SetBlackMudSummonModePayload.TYPE,
                SetBlackMudSummonModePayload.STREAM_CODEC,
                SetBlackMudSummonModePayload::handle
        );
        registrar.playToServer(
                SetShadowArtModePayload.TYPE,
                SetShadowArtModePayload.STREAM_CODEC,
                SetShadowArtModePayload::handle
        );
        registrar.playToServer(
                RequestDevourerSelectionPayload.TYPE,
                RequestDevourerSelectionPayload.STREAM_CODEC,
                RequestDevourerSelectionPayload::handle
        );
        registrar.playToServer(
                SetDevourerSelectionPayload.TYPE,
                SetDevourerSelectionPayload.STREAM_CODEC,
                SetDevourerSelectionPayload::handle
        );
        registrar.playToServer(
                SelectShadowTransferPayload.TYPE,
                SelectShadowTransferPayload.STREAM_CODEC,
                SelectShadowTransferPayload::handle
        );
        registrar.playToClient(
                EffectPayload.TYPE,
                EffectPayload.STREAM_CODEC,
                AddonNetwork::handleEffect
        );
        registrar.playToClient(
                DeathFadePayload.TYPE,
                DeathFadePayload.STREAM_CODEC,
                AddonNetwork::handleDeathFade
        );
        registrar.playToClient(
                VoidAbsorptionLinkPayload.TYPE,
                VoidAbsorptionLinkPayload.STREAM_CODEC,
                AddonNetwork::handleVoidAbsorptionLink
        );
        registrar.playToClient(
                ShadowBindingEffectPayload.TYPE,
                ShadowBindingEffectPayload.STREAM_CODEC,
                AddonNetwork::handleShadowBindingEffect
        );
        registrar.playToClient(
                MagicOutputShockwavePayload.TYPE,
                MagicOutputShockwavePayload.STREAM_CODEC,
                AddonNetwork::handleMagicOutputShockwave
        );
        registrar.playToClient(
                NightShadowCameraPayload.TYPE,
                NightShadowCameraPayload.STREAM_CODEC,
                AddonNetwork::handleNightShadowCamera
        );
        registrar.playToClient(
                StorageDataSyncPayload.TYPE,
                StorageDataSyncPayload.STREAM_CODEC,
                StorageDataSyncPayload::handle
        );
        registrar.playToClient(
                AntoresBeamVisualPayload.TYPE,
                AntoresBeamVisualPayload.STREAM_CODEC,
                AntoresBeamVisualPayload::handle
        );
        registrar.playToClient(
                DemonGodGazeVisualPayload.TYPE,
                DemonGodGazeVisualPayload.STREAM_CODEC,
                DemonGodGazeVisualPayload::handle
        );
        registrar.playToClient(
                AddonSpellVisualPayload.TYPE,
                AddonSpellVisualPayload.STREAM_CODEC,
                AddonSpellVisualPayload::handle
        );
        registrar.playToClient(
                DetectionTargetSyncPayload.TYPE,
                DetectionTargetSyncPayload.STREAM_CODEC,
                DetectionTargetSyncPayload::handle
        );
        registrar.playToClient(
                DetectionEyeStatePayload.TYPE,
                DetectionEyeStatePayload.STREAM_CODEC,
                DetectionEyeStatePayload::handle
        );
        registrar.playToClient(
                EntityDisplacementTargetPayload.TYPE,
                EntityDisplacementTargetPayload.STREAM_CODEC,
                EntityDisplacementTargetPayload::handle
        );
        registrar.playToClient(
                ImaginarySpaceStatePayload.TYPE,
                ImaginarySpaceStatePayload.STREAM_CODEC,
                ImaginarySpaceStatePayload::handle
        );
        registrar.playToClient(
                OpenDevourerSelectionPayload.TYPE,
                OpenDevourerSelectionPayload.STREAM_CODEC,
                OpenDevourerSelectionPayload::handle
        );
        registrar.playToClient(
                OpenShadowTransferPayload.TYPE,
                OpenShadowTransferPayload.STREAM_CODEC,
                OpenShadowTransferPayload::handle
        );
    }

    private static void handleOpenSpace(OpenSpacePayload message, IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND && context.player() instanceof ServerPlayer player) {
            context.enqueueWork(() -> StorageService.openStorageMenu(player));
        }
    }

    private static void handleBlockAbsorptionHold(BlockAbsorptionHoldPayload message, IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND) {
            context.enqueueWork(() -> {
            });
        }
    }

    private static void handleShadowMaterializationHold(ShadowMaterializationHoldPayload message, IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND && context.player() instanceof ServerPlayer player) {
            context.enqueueWork(() -> SakuraShadowMaterializationService.updateHeld(player, message.held()));
        }
    }

    private static void handleEffect(EffectPayload message, IPayloadContext context) {
        if (context.flow() == PacketFlow.CLIENTBOUND) {
            context.enqueueWork(() -> ClientEffects.start(message));
        }
    }

    private static void handleDeathFade(DeathFadePayload message, IPayloadContext context) {
        if (context.flow() == PacketFlow.CLIENTBOUND) {
            context.enqueueWork(() -> ClientEffects.startDeathFade(message));
        }
    }

    private static void handleVoidAbsorptionLink(VoidAbsorptionLinkPayload message, IPayloadContext context) {
        if (context.flow() == PacketFlow.CLIENTBOUND) {
            context.enqueueWork(() -> ClientEffects.maintainVoidAbsorptionLink(message));
        }
    }

    private static void handleShadowBindingEffect(ShadowBindingEffectPayload message, IPayloadContext context) {
        if (context.flow() == PacketFlow.CLIENTBOUND) {
            context.enqueueWork(() -> ClientEffects.maintainShadowBinding(message));
        }
    }

    private static void handleMagicOutputShockwave(MagicOutputShockwavePayload message, IPayloadContext context) {
        if (context.flow() == PacketFlow.CLIENTBOUND) {
            context.enqueueWork(() -> ClientEffects.startMagicOutputShockwave(message));
        }
    }

    private static void handleNightShadowCamera(NightShadowCameraPayload message, IPayloadContext context) {
        if (context.flow() == PacketFlow.CLIENTBOUND) {
            context.enqueueWork(() -> ClientNightShadowCamera.accept(message));
        }
    }
}
