package net.xxxjk.TYPE_MOON_WORLD.init;

/*
 *	MCreator note: This file will be REGENERATED on each build.
 */

import org.lwjgl.glfw.GLFW;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

import net.xxxjk.TYPE_MOON_WORLD.network.Lose_health_regain_mana_Message;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class TypeMoonWorldModKeyMappings {
    public static final KeyMapping LOSE_HEALTH_REGAIN_MANA
            = new KeyMapping("key.typemoonworld.losehealthregainmana", GLFW.GLFW_KEY_X, "key.categories.gameplay") {
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

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(LOSE_HEALTH_REGAIN_MANA);
    }

    @EventBusSubscriber({Dist.CLIENT})
    public static class KeyEventListener {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            if (Minecraft.getInstance().screen == null) {
                LOSE_HEALTH_REGAIN_MANA.consumeClick();
            }
        }
    }
}
