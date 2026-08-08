package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class AddonNetwork {
    private AddonNetwork() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
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
