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
import net.xxxjk.TYPE_MOON_WORLD.network.MagicModeSwitchMessage;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.network.MysticEyesToggleMessage;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.MagicModeSwitcherScreen;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.MagicRadialMenuScreen;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.ProjectionPresetScreen;

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
    public static final KeyMapping MAGIC_MODE_SWITCH = new KeyMapping("key.typemoonworld.magic_mode_switch", GLFW.GLFW_KEY_LEFT_CONTROL, KEY_CATEGORY);

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(MAGIC_CIRCUIT_SWITCH);
        event.register(CAST_MAGIC);
        event.register(LOSE_HEALTH_REGAIN_MANA);
        event.register(BASIC_INFORMATION_GUI);
        event.register(MYSTIC_EYES_ACTIVATE);
        event.register(OPEN_PROJECTION_PRESET);
        event.register(CYCLE_MAGIC);
        event.register(MAGIC_MODE_SWITCH);
    }

    @EventBusSubscriber({Dist.CLIENT})
    public static class KeyEventListener {
        private static boolean isTabDown = false;
        private static boolean isModeSwitchDown = false;

        @SubscribeEvent
        public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
            if (Minecraft.getInstance().screen == null) {
                double scrollDelta = event.getScrollDeltaY();
                if (scrollDelta == 0) return;

                // Priority 1: Mode Switch (Ctrl + Scroll)
                if (MAGIC_MODE_SWITCH.isDown()) {
                     boolean forward = scrollDelta > 0;
                     PacketDistributor.sendToServer(new MagicModeSwitchMessage(forward, -1));
                     event.setCanceled(true);
                     return;
                }

                // Priority 2: Cycle Magic (Z + Scroll)
                if (TypeMoonWorldModKeyMappings.CYCLE_MAGIC.isDown()) {
                    Player player = Minecraft.getInstance().player;
                    if (player != null) {
                         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                         if (vars.is_magus) {
                            PacketDistributor.sendToServer(new CycleMagicMessage(scrollDelta > 0));
                            event.setCanceled(true);
                         }
                    }
                }
            }
        }
        
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            if (Minecraft.getInstance().screen == null) {
                Player player = Minecraft.getInstance().player;
                if (player == null) return;
                
                TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);

                // Open Mode Switcher Menu on Ctrl Key Press (if correct magic selected)
                if (MAGIC_MODE_SWITCH.isDown()) {
                     if (!isModeSwitchDown) {
                         if (vars.is_magus && !vars.selected_magics.isEmpty() && vars.current_magic_index >= 0 && vars.current_magic_index < vars.selected_magics.size()) {
                            String currentMagic = vars.selected_magics.get(vars.current_magic_index);
                            if ("sword_barrel_full_open".equals(currentMagic)) {
                                if (Minecraft.getInstance().screen == null) {
                                    Minecraft.getInstance().setScreen(new MagicModeSwitcherScreen(vars.sword_barrel_mode));
                                    isModeSwitchDown = true;
                                }
                            } else if ("jewel_magic_shoot".equals(currentMagic) || "jewel_magic_release".equals(currentMagic)) {
                                if (Minecraft.getInstance().screen == null) {
                                    Minecraft.getInstance().setScreen(new MagicModeSwitcherScreen(vars.jewel_magic_mode));
                                    isModeSwitchDown = true;
                                }
                            }
                        }
                         // If we didn't open the screen (e.g. wrong magic), we still mark key as down to prevent retry every tick? 
                         // Actually, user might want to switch magic and then press Ctrl. 
                         // But if they hold Ctrl and scroll to switch magic, should it open immediately? 
                         // For safety, let's only set true if we actually TRIED to open or just mark it processed.
                         // But if we mark it processed, user has to release Ctrl to try again. This is safer.
                         isModeSwitchDown = true;
                     }
                } else {
                    isModeSwitchDown = false;
                }

                // Open Radial Menu on Z Key Press (if not scrolling)
                if (CYCLE_MAGIC.isDown()) {
                     // Only open if we have magics and are a magus
                     if (vars.is_magus && !vars.selected_magics.isEmpty()) {
                        // Only open if current screen is NOT already the radial menu
                        if (!(Minecraft.getInstance().screen instanceof MagicRadialMenuScreen)) {
                            Minecraft.getInstance().setScreen(new MagicRadialMenuScreen(vars.selected_magics));
                        }
                    }
                }
                
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
                                    if ("projection".equals(magicId) || "structural_analysis".equals(magicId) || "unlimited_blade_works".equals(magicId) || "broken_phantasm".equals(magicId)) {
                                        Minecraft.getInstance().setScreen(new ProjectionPresetScreen(player));
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
    }
}
