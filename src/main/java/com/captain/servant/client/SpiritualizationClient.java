package com.captain.servant.client;

import com.captain.servant.network.SpiritualizationClientState;
import com.captain.servant.network.payload.SpiritualizationTogglePayload;
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

public final class SpiritualizationClient {
    public static final KeyMapping SPIRITUALIZATION_KEY = new KeyMapping(
            "key.fatesupport.spiritualization",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_LCONTROL,
            net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings.KEY_CATEGORY
    );
    private static boolean hadLevel;
    private boolean controlledNoPhysics;

    public SpiritualizationClient() {
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        SpiritualizationClientState.clientTick();
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            controlledNoPhysics = false;
            if (hadLevel) {
                SpiritualizationClientState.clear();
                hadLevel = false;
            }
            return;
        }
        hadLevel = true;

        if (SPIRITUALIZATION_KEY.consumeClick() && minecraft.screen == null
                && player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servant_card_transformed) {
            PacketDistributor.sendToServer(new SpiritualizationTogglePayload());
        }

        if (SpiritualizationClientState.isRestricted(player.getUUID())) {
            if (player.isShiftKeyDown()
                    && player.onGround()
                    && player.getDeltaMovement().y <= 0.0D
                    && !player.isPassenger()) {
                player.noPhysics = true;
                controlledNoPhysics = true;
                player.setDeltaMovement(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
            } else {
                player.noPhysics = false;
                controlledNoPhysics = false;
            }
        } else if (controlledNoPhysics) {
            player.noPhysics = false;
            controlledNoPhysics = false;
        }
    }

    @EventBusSubscriber(modid = "typemoonworld", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class KeyRegistration {
        private KeyRegistration() {
        }

        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            SPIRITUALIZATION_KEY.setKeyConflictContext(net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings.SERVANT_CONTEXT);
            event.register(SPIRITUALIZATION_KEY);
        }
    }

    public static void drawHud(GuiGraphics gui, Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }
        TypeMoonWorldModVariables.PlayerVariables variables =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        if (!variables.servant_card_transformed) {
            return;
        }

        int phase = SpiritualizationClientState.phase(player.getUUID());
        int cooldown = SpiritualizationClientState.cooldown(player.getUUID());
        Component state;
        int color;
        if (phase == 1) {
            state = Component.translatable("hud.fatesupport.spiritualization.entering");
            color = 0xFFFFFF55;
        } else if (phase == 2) {
            state = Component.translatable("hud.fatesupport.spiritualization.active");
            color = 0xFF8DEBFF;
        } else if (phase == 3) {
            state = Component.translatable("hud.fatesupport.spiritualization.exiting");
            color = 0xFFFFFF55;
        } else if (cooldown > 0) {
            state = Component.literal(String.format(java.util.Locale.ROOT, "%.1f", cooldown / 20.0D));
            color = 0xFFFFAA00;
        } else {
            state = Component.translatable("hud.fatesupport.spiritualization.ready");
            color = 0xFFFF7777;
        }

        Component label = SPIRITUALIZATION_KEY.getTranslatedKeyMessage().copy()
                .append(":")
                .append(state);
        float scale = 0.54F;
        int x = 5;
        int y = 89;
        int width = Math.min(142, Math.max(18, (int) (minecraft.font.width(label) * scale) + 5));
        gui.fill(x - 1, y - 1, x + width, y + 7, 0x44000000);
        gui.pose().pushPose();
        gui.pose().translate(x + 1, y - 1, 0.0F);
        gui.pose().scale(scale, scale, 1.0F);
        gui.drawString(minecraft.font, label, 0, 0, color, false);
        gui.pose().popPose();
    }
}
