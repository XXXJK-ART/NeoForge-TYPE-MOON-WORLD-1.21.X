package com.captain.servant.smoke;

import com.captain.servant.client.KnightArmoryScreen;
import com.captain.servant.client.SpiritualizationClient;
import com.captain.servant.client.TelepathyClient;
import com.captain.servant.lancelot.KnightArmoryEntry;
import com.captain.servant.network.FateSupportNetwork;
import com.mojang.logging.LogUtils;
import java.util.Arrays;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings;
import org.lwjgl.glfw.GLFW;

/** Test-only source set added by the compatibility init script; never part of the release JAR. */
@Mod(value = "typemoonworld_support_smoke", dist = Dist.CLIENT)
public final class FateSupportClientSmoke {
    private int stage;
    private int screenTicks;

    public FateSupportClientSmoke() {
        LogUtils.getLogger().info("FATESUPPORT_CLIENT_SMOKE_START");
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (stage == 2 || mc.getOverlay() != null) return;
        try {
            if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
                mc.options.onboardingAccessibilityFinished();
                mc.setScreen(new TitleScreen());
            }
            if (stage == 0 && mc.screen instanceof TitleScreen) {
                checkKey(mc, SpiritualizationClient.SPIRITUALIZATION_KEY, GLFW.GLFW_KEY_LEFT_CONTROL);
                checkKey(mc, TypeMoonWorldModKeyMappings.SERVANT_CARD_DODGE, GLFW.GLFW_KEY_LEFT_ALT);
                checkKey(mc, TypeMoonWorldModKeyMappings.MAGIC_MODE_SWITCH, GLFW.GLFW_KEY_LEFT_ALT);
                checkKey(mc, TelepathyClient.TELEPATHY_KEY, GLFW.GLFW_KEY_Y);
                FateSupportNetwork.openArmoryScreen(KnightArmoryEntry.IRON_ROD.bit());
                if (!(mc.screen instanceof KnightArmoryScreen)) throw new AssertionError("armory handler was not initialized");
                stage = 1;
            } else if (stage == 1 && ++screenTicks >= 4) {
                if (!(mc.screen instanceof KnightArmoryScreen) || mc.screen.children().isEmpty()) {
                    throw new AssertionError("armory screen failed to initialize/render");
                }
                LogUtils.getLogger().info("FATESUPPORT_CLIENT_SMOKE_PASS voicechat={} superbwarfare={} spirit=left_ctrl dodge=left_alt",
                        ModList.get().isLoaded("voicechat"), ModList.get().isLoaded("superbwarfare"));
                stage = 2;
                mc.stop();
            }
        } catch (Throwable error) {
            stage = 2;
            LogUtils.getLogger().error("FATESUPPORT_CLIENT_SMOKE_FAIL", error);
            mc.stop();
        }
    }

    private static void checkKey(Minecraft mc, KeyMapping key, int expected) {
        if (key.getDefaultKey().getValue() != expected
                || Arrays.stream(mc.options.keyMappings).noneMatch(registered -> registered == key)) {
            throw new AssertionError("unregistered or incorrect default key: " + key.getName());
        }
    }
}
