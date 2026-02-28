package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;
import net.minecraft.network.chat.Component;

public record MagicModeSwitchMessage(int actionType, int value) implements CustomPacketPayload {
    public static final Type<MagicModeSwitchMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "magic_mode_switch"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MagicModeSwitchMessage> STREAM_CODEC = StreamCodec.of(
            (RegistryFriendlyByteBuf buffer, MagicModeSwitchMessage message) -> {
                buffer.writeInt(message.actionType);
                buffer.writeInt(message.value);
            },
            (RegistryFriendlyByteBuf buffer) -> new MagicModeSwitchMessage(buffer.readInt(), buffer.readInt())
    );

    @Override
    public Type<MagicModeSwitchMessage> type() {
        return TYPE;
    }

    public static void handleData(final MagicModeSwitchMessage message, final IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND) {
            context.enqueueWork(() -> {
                net.minecraft.world.entity.player.Player player = context.player();
                TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                
                // actionType:
                // 0: Set Mode (Default/Legacy) - value is mode index
                // 1: Cycle Mode (Default/Legacy) - value is direction (1 or -1)
                // 2: Set Reinforcement Target - value is target index
                // 3: Set Reinforcement Mode - value is mode index
                // 6: Toggle Gravity Target (Self/Other)
                
                if (message.actionType == 2) {
                    vars.reinforcement_target = message.value;
                    
                    Component targetComp = Component.literal("");
                    switch (vars.reinforcement_target) {
                        case 0: targetComp = Component.translatable("gui.typemoonworld.mode.self"); break;
                        case 1: targetComp = Component.translatable("gui.typemoonworld.mode.other"); break;
                        case 2: targetComp = Component.translatable("gui.typemoonworld.mode.item"); break;
                        case 3: targetComp = Component.translatable("gui.typemoonworld.mode.cancel"); break;
                    }
                    player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.target_selected", targetComp), true);
                    
                    vars.syncPlayerVariables(player);
                    return;
                } else if (message.actionType == 3) {
                    vars.reinforcement_mode = message.value;
                    
                    Component feedbackComp = Component.literal("");
                    if (vars.reinforcement_target == 3) {
                        // Cancel Target Feedback
                        String key = "";
                        switch (vars.reinforcement_mode) {
                            case 0: key = "message.typemoonworld.magic.reinforcement.cancel_self"; break;
                            case 1: key = "message.typemoonworld.magic.reinforcement.cancel_other"; break;
                            case 2: key = "message.typemoonworld.magic.reinforcement.cancel_item"; break;
                        }
                        if (!key.isEmpty()) {
                            player.displayClientMessage(Component.translatable(key), true);
                        }
                    } else {
                        // Body Part Feedback
                        switch (vars.reinforcement_mode) {
                            case 0: feedbackComp = Component.translatable("gui.typemoonworld.mode.body"); break;
                            case 1: feedbackComp = Component.translatable("gui.typemoonworld.mode.hand"); break;
                            case 2: feedbackComp = Component.translatable("gui.typemoonworld.mode.leg"); break;
                            case 3: feedbackComp = Component.translatable("gui.typemoonworld.mode.eye"); break;
                        }
                        player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.part_selected", feedbackComp), true);
                    }
                    
                    vars.syncPlayerVariables(player);
                    return;
                } else if (message.actionType == 4) {
                    vars.reinforcement_level = message.value;
                    player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.level_selected", vars.reinforcement_level), true);
                    vars.syncPlayerVariables(player);
                    return;
                } else if (message.actionType == 5) {
                    // This is now handled in CastMagic when reinforcement_target == 3
                    return;
                } else if (message.actionType == 6) {
                    if (vars.selected_magics.isEmpty() || vars.current_magic_index < 0 || vars.current_magic_index >= vars.selected_magics.size()) {
                        return;
                    }
                    String currentMagic = vars.selected_magics.get(vars.current_magic_index);
                    if (!"gravity_magic".equals(currentMagic)) {
                        return;
                    }

                    vars.gravity_magic_target = vars.gravity_magic_target == 0 ? 1 : 0;
                    Component targetComp = Component.translatable(
                            vars.gravity_magic_target == 0
                                    ? "gui.typemoonworld.mode.self"
                                    : "gui.typemoonworld.mode.other"
                    );
                    player.displayClientMessage(Component.translatable("message.typemoonworld.magic.gravity.target_changed", targetComp), true);
                    vars.syncPlayerVariables(player);
                    return;
                }

                if (!vars.selected_magics.isEmpty() && vars.current_magic_index >= 0 && vars.current_magic_index < vars.selected_magics.size()) {
                    String currentMagic = vars.selected_magics.get(vars.current_magic_index);
                    
                    if ("sword_barrel_full_open".equals(currentMagic)) {
                        // Switch between 5 modes (0, 1, 2, 3, 4)
                        int maxModes = 5;
                        
                        if (message.actionType == 0) {
                            if (message.value == 3) {
                                // Toggle Broken Phantasm (Explosion)
                                vars.ubw_broken_phantasm_enabled = !vars.ubw_broken_phantasm_enabled;
                                player.getPersistentData().putBoolean("UBWBrokenPhantasmEnabled", vars.ubw_broken_phantasm_enabled);
                                
                                if (vars.ubw_broken_phantasm_enabled) {
                                    player.displayClientMessage(Component.literal("Broken Phantasm Mode: ON"), true);
                                } else {
                                    player.displayClientMessage(Component.literal("Broken Phantasm Mode: OFF"), true);
                                }
                                vars.syncPlayerVariables(player);
                            } else {
                                vars.sword_barrel_mode = message.value;
                                
                                // Send feedback
                                String modeStr = String.valueOf(vars.sword_barrel_mode);
                                if (vars.sword_barrel_mode == 0) modeStr = "0 (AOE)";
                                else if (vars.sword_barrel_mode == 1) modeStr = "1 (Aiming)";
                                else if (vars.sword_barrel_mode == 2) modeStr = "2 (Focus)";
                                else if (vars.sword_barrel_mode == 4) modeStr = "4 (Clear)";

                                player.displayClientMessage(Component.translatable(MagicConstants.MSG_MAGIC_SWORD_BARREL_MODE_CHANGE, modeStr), true);
                                vars.syncPlayerVariables(player);
                            }
                        } else if (message.actionType == 1) {
                            if (message.value > 0) {
                                vars.sword_barrel_mode = (vars.sword_barrel_mode + 1) % maxModes;
                            } else {
                                vars.sword_barrel_mode = (vars.sword_barrel_mode - 1 + maxModes) % maxModes;
                            }
                            
                            // Send feedback
                            String modeStr = String.valueOf(vars.sword_barrel_mode);
                            if (vars.sword_barrel_mode == 0) modeStr = "0 (AOE)";
                            else if (vars.sword_barrel_mode == 1) modeStr = "1 (Aiming)";
                            else if (vars.sword_barrel_mode == 2) modeStr = "2 (Focus)";
                            else if (vars.sword_barrel_mode == 3) modeStr = "3 (Broken Phantasm)";
                            else if (vars.sword_barrel_mode == 4) modeStr = "4 (Clear)";

                            player.displayClientMessage(Component.translatable(MagicConstants.MSG_MAGIC_SWORD_BARREL_MODE_CHANGE, modeStr), true);
                            vars.syncPlayerVariables(player);
                        }
                    } else if ("jewel_magic_shoot".equals(currentMagic) || "jewel_magic_release".equals(currentMagic)) {
                        // Switch between 6 modes (0: Ruby, 1: Sapphire, 2: Emerald, 3: Topaz, 4: Cyan, 5: Random)
                        int maxModes = 6;
                        if ("jewel_magic_release".equals(currentMagic)) {
                            maxModes = 4; // Release has no Random and no Ruby mode
                        }
                        
                        if (message.actionType == 0) {
                            // Validate if learned
                            if (isJewelModeUnlocked(vars, message.value, currentMagic)) {
                                vars.jewel_magic_mode = message.value;
                            }
                        } else if (message.actionType == 1) {

                            // Cycle logic: find next unlocked mode
                            int current = vars.jewel_magic_mode;
                            int nextMode = current;
                            int attempts = 0;
                            do {
                                if (message.value > 0) {
                                    nextMode = (nextMode + 1) % maxModes;
                                } else {
                                    nextMode = (nextMode - 1 + maxModes) % maxModes;
                                }
                                attempts++;
                            } while (!isJewelModeUnlocked(vars, nextMode, currentMagic) && attempts < maxModes);
                            
                            if (isJewelModeUnlocked(vars, nextMode, currentMagic)) {
                                vars.jewel_magic_mode = nextMode;
                            }
                        }
                        
                        // Send feedback
                        String modeStr = "";
                        switch(vars.jewel_magic_mode) {
                            case 0: modeStr = "Ruby"; break;
                            case 1: modeStr = "Sapphire"; break;
                            case 2: modeStr = "Emerald"; break;
                            case 3: modeStr = "Topaz"; break;
                            case 4: modeStr = "Cyan"; break;
                            case 5: modeStr = "Random"; break;
                        }
                        if ("jewel_magic_release".equals(currentMagic)) {
                            switch (vars.jewel_magic_mode) {
                                case 0 -> modeStr = "Sapphire";
                                case 1 -> modeStr = "Emerald";
                                case 2 -> modeStr = "Topaz";
                                case 3 -> modeStr = "Cyan";
                                default -> modeStr = "Sapphire";
                            }
                        }
                        
                        player.displayClientMessage(Component.translatable(MagicConstants.MSG_MAGIC_JEWEL_MODE_CHANGE, modeStr), true);
                        vars.syncPlayerVariables(player);
                    } else if ("reinforcement".equals(currentMagic)) {
                        // Reinforcement logic handled by actionType 2 and 3 at start of method
                    }
                }
            }).exceptionally(e -> {
                context.connection().disconnect(Component.literal(e.getMessage()));
                return null;
            });
        }
    }
    
    private static boolean isJewelModeUnlocked(TypeMoonWorldModVariables.PlayerVariables vars, int mode, String magicType) {
        if ("jewel_magic_shoot".equals(magicType)) {
            switch (mode) {
                case 0: return vars.learned_magics.contains("ruby_throw");
                case 1: return vars.learned_magics.contains("sapphire_throw");
                case 2: return vars.learned_magics.contains("emerald_use");
                case 3: return vars.learned_magics.contains("topaz_throw");
                case 4: return vars.learned_magics.contains("cyan_throw");
                case 5: return true; // Random
                default: return false;
            }
        } else if ("jewel_magic_release".equals(magicType)) {
            switch (mode) {
                case 0: return vars.learned_magics.contains("sapphire_winter_frost");
                case 1: return vars.learned_magics.contains("emerald_winter_river");
                case 2: return vars.learned_magics.contains("topaz_reinforcement");
                case 3: return vars.learned_magics.contains("cyan_wind");
                default: return false;
            }
        }
        return true;
    }
}
