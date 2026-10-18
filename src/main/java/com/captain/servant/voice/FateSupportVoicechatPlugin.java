package com.captain.servant.voice;

import com.captain.servant.TypeMoonServant;
import com.captain.servant.voice.VoiceCommunicationService.PrivateVoiceRoute;
import com.captain.servant.voice.VoiceCommunicationService.VoiceRoute;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.packets.LocationalSoundPacket;
import java.util.UUID;

@ForgeVoicechatPlugin
public final class FateSupportVoicechatPlugin implements VoicechatPlugin {
    @Override
    public String getPluginId() {
        return TypeMoonServant.MOD_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        VoiceCommunicationService.setVoiceDistance(api.getVoiceChatDistance());
        TypeMoonServant.LOGGER.info("Fate Support voice chat plugin initialized");
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
    }

    private void onMicrophonePacket(MicrophonePacketEvent event) {
        VoicechatConnection senderConnection = event.getSenderConnection();
        if (senderConnection == null || senderConnection.getPlayer() == null) {
            return;
        }

        UUID senderId = senderConnection.getPlayer().getUuid();
        VoicechatServerApi api = event.getVoicechat();
        PrivateVoiceRoute privateRoute = VoiceCommunicationService.privateVoiceRoute(senderId);
        if (privateRoute != null) {
            event.cancel();
            VoicechatConnection targetConnection = api.getConnectionOf(privateRoute.targetId());
            if (isAvailable(targetConnection)) {
                api.sendStaticSoundPacketTo(targetConnection, event.getPacket().toStaticSoundPacket());
            }

            LocationalSoundPacket spectatorPacket = event.getPacket()
                    .locationalSoundPacketBuilder()
                    .channelId(privateRoute.channelId())
                    .position(api.createPosition(privateRoute.x(), privateRoute.y(), privateRoute.z()))
                    .distance(privateRoute.distance())
                    .build();
            for (UUID listenerId : privateRoute.spectatorListeners()) {
                VoicechatConnection listener = api.getConnectionOf(listenerId);
                if (isAvailable(listener)) {
                    api.sendLocationalSoundPacketTo(listener, spectatorPacket);
                }
            }
            return;
        }

        for (VoiceRoute route : VoiceCommunicationService.voiceRoutes(senderId)) {
            LocationalSoundPacket packet = event.getPacket()
                    .locationalSoundPacketBuilder()
                    .channelId(route.channelId())
                    .position(api.createPosition(route.x(), route.y(), route.z()))
                    .distance(route.distance())
                    .build();
            for (UUID listenerId : route.listeners()) {
                VoicechatConnection listener = api.getConnectionOf(listenerId);
                if (isAvailable(listener)) {
                    api.sendLocationalSoundPacketTo(listener, packet);
                }
            }
        }
    }

    private static boolean isAvailable(VoicechatConnection connection) {
        return connection != null && connection.isInstalled() && connection.isConnected() && !connection.isDisabled();
    }
}
