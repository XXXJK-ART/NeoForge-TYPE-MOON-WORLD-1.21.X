package com.captain.servant.client;

import com.captain.servant.voice.VoiceCommunicationService;
import com.captain.servant.network.payload.TelepathyTogglePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class TelepathyClient {
    public static final KeyMapping TELEPATHY_KEY = new KeyMapping(
            "key.fatesupport.telepathy",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_Y,
            net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings.KEY_CATEGORY
    );

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        if (!VoiceCommunicationService.isAvailable()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.screen == null && TELEPATHY_KEY.consumeClick()) {
            PacketDistributor.sendToServer(new TelepathyTogglePayload());
        }
    }

    @EventBusSubscriber(modid = "typemoonworld", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class KeyRegistration {
        private KeyRegistration() {
        }

        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            TELEPATHY_KEY.setKeyConflictContext(net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME);
            event.register(TELEPATHY_KEY);
        }
    }

    public static void drawHud(GuiGraphics gui, Minecraft minecraft) {
        if (!VoiceCommunicationService.isAvailable()) {
            return;
        }
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }
        TypeMoonWorldModVariables.PlayerVariables variables =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        if (!variables.servant_card_transformed) {
            return;
        }

        Component label = TELEPATHY_KEY.getTranslatedKeyMessage().copy()
                .append(":")
                .append(Component.translatable("hud.fatesupport.telepathy"));
        float scale = 0.54F;
        int x = 5;
        int y = 178;
        int width = Math.min(142, Math.max(18, (int) (minecraft.font.width(label) * scale) + 5));
        gui.fill(x - 1, y - 1, x + width, y + 7, 0x44000000);
        gui.pose().pushPose();
        gui.pose().translate(x + 1, y - 1, 0.0F);
        gui.pose().scale(scale, scale, 1.0F);
        gui.drawString(minecraft.font, label, 0, 0, 0xFF8DEBFF, false);
        gui.pose().popPose();
    }
}
