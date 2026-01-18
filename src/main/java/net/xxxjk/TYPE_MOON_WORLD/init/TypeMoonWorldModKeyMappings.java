package net.xxxjk.TYPE_MOON_WORLD.init;

/*
 *	MCreator note: This file will be REGENERATED on each build.
 */

import net.xxxjk.TYPE_MOON_WORLD.network.Basic_information_gui_Message;
import org.lwjgl.glfw.GLFW;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

import net.xxxjk.TYPE_MOON_WORLD.network.MagicCircuitSwitchMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.Lose_health_regain_mana_Message;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class TypeMoonWorldModKeyMappings {
    public static final KeyMapping MAGIC_CIRCUIT_SWITCH
            = new KeyMapping("key.typemoonworld.magic_circuit_switch", GLFW.GLFW_KEY_Z, "key.categories.gameplay") {
        private boolean isDownOld = false;

        @Override
        public void setDown(boolean isDown) {
            super.setDown(isDown);
            if (isDownOld != isDown && isDown) {
                PacketDistributor.sendToServer(new MagicCircuitSwitchMessage(0, 0));
                if (Minecraft.getInstance().player != null) {
                    MagicCircuitSwitchMessage.pressAction(Minecraft.getInstance().player, 0, 0);
                }
            }
            isDownOld = isDown;
        }
    };

    public static final KeyMapping LOSE_HEALTH_REGAIN_MANA
            = new KeyMapping("key.typemoonworld.lose_health_regain_mana", GLFW.GLFW_KEY_X, "key.categories.gameplay") {
        private boolean isDownOld = false;

        @Override
        public void setDown(boolean isDown) {
            super.setDown(isDown);
            if (isDownOld != isDown && isDown) {
                PacketDistributor.sendToServer(new Lose_health_regain_mana_Message(0, 0));
                if (Minecraft.getInstance().player != null) {
                    Lose_health_regain_mana_Message.pressAction(Minecraft.getInstance().player, 0, 0);
                }
            }
            isDownOld = isDown;
        }
    };

    public static final KeyMapping BASIC_INFORMATION_GUI
            = new KeyMapping("key.typemoonworld.basic_information_gui", GLFW.GLFW_KEY_R, "key.categories.gameplay") {
        private boolean isDownOld = false;

        @Override
        public void setDown(boolean isDown) {
            super.setDown(isDown);
            if (isDownOld != isDown && isDown) {
                PacketDistributor.sendToServer(new Basic_information_gui_Message(0, 0));
                if (Minecraft.getInstance().player != null) {
                    Basic_information_gui_Message.pressAction(Minecraft.getInstance().player, 0, 0);
                }
            }
            isDownOld = isDown;
        }
    };

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(MAGIC_CIRCUIT_SWITCH);
        event.register(LOSE_HEALTH_REGAIN_MANA);
        event.register(BASIC_INFORMATION_GUI);
    }

    @EventBusSubscriber({Dist.CLIENT})
    public static class KeyEventListener {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            if (Minecraft.getInstance().screen == null) {
                MAGIC_CIRCUIT_SWITCH.consumeClick();
                LOSE_HEALTH_REGAIN_MANA.consumeClick();
                BASIC_INFORMATION_GUI.consumeClick();
            }
        }
    }
}
