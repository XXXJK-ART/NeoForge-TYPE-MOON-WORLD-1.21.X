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
import net.xxxjk.TYPE_MOON_WORLD.network.CastMagicMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.Lose_health_regain_mana_Message;

import net.neoforged.neoforge.client.event.InputEvent;
import net.xxxjk.TYPE_MOON_WORLD.network.CycleMagicMessage;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.network.MysticEyesToggleMessage;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class TypeMoonWorldModKeyMappings {
    public static final String KEY_CATEGORY = "key.categories.typemoonworld";
    
    public static final KeyMapping MAGIC_CIRCUIT_SWITCH = new KeyMapping("key.typemoonworld.magic_circuit_switch", GLFW.GLFW_KEY_B, KEY_CATEGORY);
    public static final KeyMapping CAST_MAGIC = new KeyMapping("key.typemoonworld.cast_magic", GLFW.GLFW_KEY_C, KEY_CATEGORY);
    public static final KeyMapping LOSE_HEALTH_REGAIN_MANA = new KeyMapping("key.typemoonworld.lose_health_regain_mana", GLFW.GLFW_KEY_X, KEY_CATEGORY);
    public static final KeyMapping BASIC_INFORMATION_GUI = new KeyMapping("key.typemoonworld.basic_information_gui", GLFW.GLFW_KEY_R, KEY_CATEGORY);
    public static final KeyMapping MYSTIC_EYES_ACTIVATE = new KeyMapping("key.typemoonworld.mystic_eyes_activate", GLFW.GLFW_KEY_V, KEY_CATEGORY);
    public static final KeyMapping OPEN_PROJECTION_PRESET = new KeyMapping("key.typemoonworld.open_projection_preset", GLFW.GLFW_KEY_TAB, KEY_CATEGORY);
    public static final KeyMapping CYCLE_MAGIC = new KeyMapping("key.typemoonworld.cycle_magic", GLFW.GLFW_KEY_Z, KEY_CATEGORY);

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(MAGIC_CIRCUIT_SWITCH);
        event.register(CAST_MAGIC);
        event.register(LOSE_HEALTH_REGAIN_MANA);
        event.register(BASIC_INFORMATION_GUI);
        event.register(MYSTIC_EYES_ACTIVATE);
        event.register(OPEN_PROJECTION_PRESET);
        event.register(CYCLE_MAGIC);
    }

    @EventBusSubscriber({Dist.CLIENT})
    public static class KeyEventListener {
        private static boolean isTabDown = false;

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            if (Minecraft.getInstance().screen == null) {
                Player player = Minecraft.getInstance().player;
                if (player == null) return;
                
                TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                
                if (MAGIC_CIRCUIT_SWITCH.consumeClick()) {
                    if (vars.is_magus) {
                        PacketDistributor.sendToServer(new MagicCircuitSwitchMessage(0, 0));
                        MagicCircuitSwitchMessage.pressAction(player, 0, 0);
                    }
                }
                if (CAST_MAGIC.consumeClick()) {
                    if (vars.is_magus) {
                        PacketDistributor.sendToServer(new CastMagicMessage());
                        CastMagicMessage.pressAction(player);
                    }
                }
                if (LOSE_HEALTH_REGAIN_MANA.consumeClick()) {
                    // Always allow X key to trigger unlock logic
                    PacketDistributor.sendToServer(new Lose_health_regain_mana_Message(0, 0));
                    Lose_health_regain_mana_Message.pressAction(player, 0, 0);
                }
                if (BASIC_INFORMATION_GUI.consumeClick()) {
                    if (vars.is_magus) {
                        PacketDistributor.sendToServer(new Basic_information_gui_Message(0, 0));
                        Basic_information_gui_Message.pressAction(player, 0, 0);
                    }
                }
                if (MYSTIC_EYES_ACTIVATE.consumeClick()) {
                    if (vars.is_magus) {
                        PacketDistributor.sendToServer(new MysticEyesToggleMessage(0));
                        MysticEyesToggleMessage.pressAction(player, 0);
                    }
                }

                // Tab Key for Projection GUI
                if (OPEN_PROJECTION_PRESET.isDown()) {
                    if (!isTabDown) {
                        isTabDown = true;
                        if (vars.is_magus) { // Check magus status
                            if (vars.is_magic_circuit_open && !vars.selected_magics.isEmpty()) {
                                int index = vars.current_magic_index;
                                if (index >= 0 && index < vars.selected_magics.size()) {
                                    String magicId = vars.selected_magics.get(index);
                                    if ("projection".equals(magicId) || "structural_analysis".equals(magicId) || "unlimited_blade_works".equals(magicId)) {
                                        Minecraft.getInstance().setScreen(new net.xxxjk.TYPE_MOON_WORLD.client.gui.ProjectionPresetScreen(player));
                                    }
                                }
                            }
                        }
                    }
                } else {
                    isTabDown = false;
                }
            }
        }
        
        @SubscribeEvent
        public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
            if (Minecraft.getInstance().screen == null) {
                if (TypeMoonWorldModKeyMappings.CYCLE_MAGIC.isDown()) {
                    Player player = Minecraft.getInstance().player;
                    if (player != null) {
                         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                         if (vars.is_magus) {
                            double scrollDelta = event.getScrollDeltaY();
                            if (scrollDelta != 0) {
                                PacketDistributor.sendToServer(new CycleMagicMessage(scrollDelta > 0));
                                event.setCanceled(true);
                            }
                         }
                    }
                }
            }
        }
    }
}
