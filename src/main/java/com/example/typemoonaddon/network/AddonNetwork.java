package com.example.typemoonaddon.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
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
    }
}
