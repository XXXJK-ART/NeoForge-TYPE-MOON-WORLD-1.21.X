package com.example.typemoonaddon.client;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.magic.SakuraTypeMoonIntegration;
import com.example.typemoonaddon.network.RequestDevourerSelectionPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID, value = Dist.CLIENT)
public final class SakuraClientInputEvents {
    private static boolean modeSwitchDown;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            modeSwitchDown = TypeMoonWorldModKeyMappings.MAGIC_MODE_SWITCH.isDown();
            return;
        }
        boolean down = TypeMoonWorldModKeyMappings.MAGIC_MODE_SWITCH.isDown();
        if (down && !modeSwitchDown) {
            TypeMoonWorldModVariables.PlayerVariables vars = minecraft.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            String currentMagic = PlayerMagicSelectionService.getCurrentMagicId(vars);
            if (matches(currentMagic, SakuraTypeMoonIntegration.IMAGINARY_STORAGE.toString())
                    || matches(currentMagic, SakuraTypeMoonIntegration.IMAGINARY_ABSORPTION.toString())) {
                minecraft.setScreen(new ImaginaryModeScreen());
            } else if (matches(currentMagic, SakuraTypeMoonIntegration.BLACK_MUD_CONTROL.toString())) {
                minecraft.setScreen(new BlackMudControlScreen());
            } else if (matches(currentMagic, SakuraTypeMoonIntegration.SUMMON_BLACK_MUD.toString())) {
                minecraft.setScreen(new BlackMudSummonModeScreen());
            } else if (matches(currentMagic, SakuraTypeMoonIntegration.SHADOW_ART.toString())) {
                minecraft.setScreen(new ShadowArtModeScreen());
            } else if (matches(currentMagic, SakuraTypeMoonIntegration.HEROIC_SPIRIT_DEVOURER.toString())) {
                PacketDistributor.sendToServer(RequestDevourerSelectionPayload.INSTANCE);
            }
        }
        modeSwitchDown = down;
    }

    private static boolean matches(String actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        String expectedPath = expected.contains(":") ? expected.substring(expected.indexOf(':') + 1) : expected;
        return actual.equals(expected) || actual.equals(expectedPath);
    }

    private SakuraClientInputEvents() {
    }
}
