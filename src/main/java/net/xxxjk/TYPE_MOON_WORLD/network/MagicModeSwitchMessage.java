package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

public record MagicModeSwitchMessage(int actionType, int value) implements CustomPacketPayload {
   public static final Type<MagicModeSwitchMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "magic_mode_switch"));
   public static final StreamCodec<RegistryFriendlyByteBuf, MagicModeSwitchMessage> STREAM_CODEC = StreamCodec.of((buffer, message) -> {
      buffer.writeInt(message.actionType);
      buffer.writeInt(message.value);
   }, buffer -> new MagicModeSwitchMessage(buffer.readInt(), buffer.readInt()));

   public Type<MagicModeSwitchMessage> type() {
      return TYPE;
   }

   public static void handleData(MagicModeSwitchMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.SERVERBOUND) {
         context.enqueueWork(
               () -> {
                  Player player = context.player();
                  TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
                     TypeMoonWorldModVariables.PLAYER_VARIABLES
                  );
                  vars.ensureMagicSystemInitialized();
                  vars.rebuildSelectedMagicsFromActiveWheel();
                  String currentMagic = null;
                  if (!vars.selected_magics.isEmpty() && vars.current_magic_index >= 0 && vars.current_magic_index < vars.selected_magics.size()) {
                     currentMagic = vars.selected_magics.get(vars.current_magic_index);
                  }

                  if (currentMagic == null || !vars.isCurrentSelectionFromCrest(currentMagic)) {
                     if (message.actionType == 2) {
                        if (message.value >= 0 && message.value <= 3) {
                           vars.reinforcement_target = message.value;
                           Component targetComp = Component.literal("");
                           switch (vars.reinforcement_target) {
                              case 0:
                                 targetComp = Component.translatable("gui.typemoonworld.mode.self");
                                 break;
                              case 1:
                                 targetComp = Component.translatable("gui.typemoonworld.mode.other");
                                 break;
                              case 2:
                                 targetComp = Component.translatable("gui.typemoonworld.mode.item");
                                 break;
                              case 3:
                                 targetComp = Component.translatable("gui.typemoonworld.mode.cancel");
                           }

                           player.displayClientMessage(
                              Component.translatable("message.typemoonworld.magic.reinforcement.target_selected", new Object[]{targetComp}), true
                           );
                           vars.syncModeState(player);
                        }
                     } else if (message.actionType == 3) {
                        int maxMode = vars.reinforcement_target == 3 ? 2 : 3;
                        if (message.value >= 0 && message.value <= maxMode) {
                           vars.reinforcement_mode = message.value;
                           Component feedbackComp = Component.literal("");
                           if (vars.reinforcement_target == 3) {
                              String key = "";
                              switch (vars.reinforcement_mode) {
                                 case 0:
                                    key = "message.typemoonworld.magic.reinforcement.cancel_self";
                                    break;
                                 case 1:
                                    key = "message.typemoonworld.magic.reinforcement.cancel_other";
                                    break;
                                 case 2:
                                    key = "message.typemoonworld.magic.reinforcement.cancel_item";
                              }

                              if (!key.isEmpty()) {
                                 player.displayClientMessage(Component.translatable(key), true);
                              }
                           } else {
                              switch (vars.reinforcement_mode) {
                                 case 0:
                                    feedbackComp = Component.translatable("gui.typemoonworld.mode.body");
                                    break;
                                 case 1:
                                    feedbackComp = Component.translatable("gui.typemoonworld.mode.hand");
                                    break;
                                 case 2:
                                    feedbackComp = Component.translatable("gui.typemoonworld.mode.leg");
                                    break;
                                 case 3:
                                    feedbackComp = Component.translatable("gui.typemoonworld.mode.eye");
                              }

                              player.displayClientMessage(
                                 Component.translatable("message.typemoonworld.magic.reinforcement.part_selected", new Object[]{feedbackComp}), true
                              );
                           }

                           vars.syncModeState(player);
                        }
                     } else if (message.actionType == 4) {
                        if (message.value >= 1 && message.value <= 5) {
                           vars.reinforcement_level = message.value;
                           player.displayClientMessage(
                              Component.translatable("message.typemoonworld.magic.reinforcement.level_selected", new Object[]{vars.reinforcement_level}), true
                           );
                           vars.syncModeState(player);
                        }
                     } else if (message.actionType != 5) {
                        if (message.actionType == 6) {
                           if (currentMagic != null) {
                              if ("gravity_magic".equals(currentMagic)) {
                                 if (message.value < 0) {
                                    vars.gravity_magic_target = vars.gravity_magic_target == 0 ? 1 : 0;
                                 } else {
                                    if (message.value != 0 && message.value != 1) {
                                       return;
                                    }

                                    vars.gravity_magic_target = message.value;
                                 }

                                 Component targetComp = Component.translatable(
                                    vars.gravity_magic_target == 0 ? "gui.typemoonworld.mode.self" : "gui.typemoonworld.mode.other"
                                 );
                                 player.displayClientMessage(
                                    Component.translatable("message.typemoonworld.magic.gravity.target_changed", new Object[]{targetComp}), true
                                 );
                                 vars.syncModeState(player);
                              }
                           }
                        } else if (message.actionType == 7) {
                           if (currentMagic != null) {
                              if ("gravity_magic".equals(currentMagic)) {
                                 if (message.value >= -2 && message.value <= 2) {
                                    vars.gravity_magic_mode = message.value;

                                    String modeKey = switch (vars.gravity_magic_mode) {
                                       case -2 -> "gui.typemoonworld.mode.gravity.ultra_light";
                                       case -1 -> "gui.typemoonworld.mode.gravity.light";
                                       default -> "gui.typemoonworld.mode.gravity.normal";
                                       case 1 -> "gui.typemoonworld.mode.gravity.heavy";
                                       case 2 -> "gui.typemoonworld.mode.gravity.ultra_heavy";
                                    };
                                    player.displayClientMessage(
                                       Component.translatable("message.typemoonworld.magic.gravity.mode_changed", new Object[]{Component.translatable(modeKey)}),
                                       true
                                    );
                                    vars.syncModeState(player);
                                 }
                              }
                           }
                        } else {
                           if (currentMagic != null) {
                              if ("sword_barrel_full_open".equals(currentMagic)) {
                                 int maxModes = 5;
                                 if (message.actionType == 0) {
                                    if (message.value < 0 || message.value >= maxModes) {
                                       return;
                                    }

                                    if (message.value == 3) {
                                       vars.ubw_broken_phantasm_enabled = !vars.ubw_broken_phantasm_enabled;
                                       player.getPersistentData().putBoolean("UBWBrokenPhantasmEnabled", vars.ubw_broken_phantasm_enabled);
                                       if (vars.ubw_broken_phantasm_enabled) {
                                          player.displayClientMessage(Component.literal("Broken Phantasm Mode: ON"), true);
                                       } else {
                                          player.displayClientMessage(Component.literal("Broken Phantasm Mode: OFF"), true);
                                       }

                                       vars.syncModeState(player);
                                    } else {
                                       vars.sword_barrel_mode = message.value;
                                       String modeStr = String.valueOf(vars.sword_barrel_mode);
                                       if (vars.sword_barrel_mode == 0) {
                                          modeStr = "0 (AOE)";
                                       } else if (vars.sword_barrel_mode == 1) {
                                          modeStr = "1 (Aiming)";
                                       } else if (vars.sword_barrel_mode == 2) {
                                          modeStr = "2 (Focus)";
                                       } else if (vars.sword_barrel_mode == 4) {
                                          modeStr = "4 (Clear)";
                                       }

                                       player.displayClientMessage(
                                          Component.translatable("message.typemoonworld.magic.sword_barrel.mode_change", new Object[]{modeStr}), true
                                       );
                                       vars.syncModeState(player);
                                    }
                                 } else if (message.actionType == 1) {
                                    if (message.value == 0) {
                                       return;
                                    }

                                    if (message.value > 0) {
                                       vars.sword_barrel_mode = (vars.sword_barrel_mode + 1) % maxModes;
                                    } else {
                                       vars.sword_barrel_mode = (vars.sword_barrel_mode - 1 + maxModes) % maxModes;
                                    }

                                    String modeStr = String.valueOf(vars.sword_barrel_mode);
                                    if (vars.sword_barrel_mode == 0) {
                                       modeStr = "0 (AOE)";
                                    } else if (vars.sword_barrel_mode == 1) {
                                       modeStr = "1 (Aiming)";
                                    } else if (vars.sword_barrel_mode == 2) {
                                       modeStr = "2 (Focus)";
                                    } else if (vars.sword_barrel_mode == 3) {
                                       modeStr = "3 (Broken Phantasm)";
                                    } else if (vars.sword_barrel_mode == 4) {
                                       modeStr = "4 (Clear)";
                                    }

                                    player.displayClientMessage(
                                       Component.translatable("message.typemoonworld.magic.sword_barrel.mode_change", new Object[]{modeStr}), true
                                    );
                                    vars.syncModeState(player);
                                 }
                              } else if ("jewel_magic_shoot".equals(currentMagic) || "jewel_magic_release".equals(currentMagic)) {
                                 int maxModes = 6;
                                 if ("jewel_magic_release".equals(currentMagic)) {
                                    maxModes = 4;
                                 }

                                 if (message.actionType == 0) {
                                    if (message.value < 0 || message.value >= maxModes) {
                                       return;
                                    }

                                    if (isJewelModeUnlocked(vars, message.value, currentMagic)) {
                                       vars.jewel_magic_mode = message.value;
                                    }
                                 } else if (message.actionType == 1) {
                                    if (message.value == 0) {
                                       return;
                                    }

                                    int current = vars.jewel_magic_mode;
                                    int nextMode = current;
                                    int attempts = 0;

                                    do {
                                       if (message.value > 0) {
                                          nextMode = (nextMode + 1) % maxModes;
                                       } else {
                                          nextMode = (nextMode - 1 + maxModes) % maxModes;
                                       }
                                    } while (!isJewelModeUnlocked(vars, nextMode, currentMagic) && ++attempts < maxModes);

                                    if (isJewelModeUnlocked(vars, nextMode, currentMagic)) {
                                       vars.jewel_magic_mode = nextMode;
                                    }
                                 }

                                 String modeStr = "";
                                 switch (vars.jewel_magic_mode) {
                                    case 0:
                                       modeStr = "Ruby";
                                       break;
                                    case 1:
                                       modeStr = "Sapphire";
                                       break;
                                    case 2:
                                       modeStr = "Emerald";
                                       break;
                                    case 3:
                                       modeStr = "Topaz";
                                       break;
                                    case 4:
                                       modeStr = "Cyan";
                                       break;
                                    case 5:
                                       modeStr = "Random";
                                 }

                                 if ("jewel_magic_release".equals(currentMagic)) {
                                    switch (vars.jewel_magic_mode) {
                                       case 0:
                                          modeStr = "Sapphire";
                                          break;
                                       case 1:
                                          modeStr = "Emerald";
                                          break;
                                       case 2:
                                          modeStr = "Topaz";
                                          break;
                                       case 3:
                                          modeStr = "Cyan";
                                          break;
                                       default:
                                          modeStr = "Sapphire";
                                    }
                                 }

                                 player.displayClientMessage(
                                    Component.translatable("message.typemoonworld.magic.jewel.mode_change", new Object[]{modeStr}), true
                                 );
                                 vars.syncModeState(player);
                              } else if ("gandr_machine_gun".equals(currentMagic)) {
                                 if (message.actionType == 0) {
                                    vars.gandr_machine_gun_mode = message.value == 1 ? 1 : 0;
                                 } else {
                                    if (message.actionType != 1) {
                                       return;
                                    }

                                    vars.gandr_machine_gun_mode = vars.gandr_machine_gun_mode == 0 ? 1 : 0;
                                 }

                                 String modeKey = vars.gandr_machine_gun_mode == 1
                                    ? "gui.typemoonworld.mode.gandr_barrage"
                                    : "gui.typemoonworld.mode.gandr_rapid";
                                 player.displayClientMessage(
                                    Component.translatable(
                                       "message.typemoonworld.magic.gandr_machine_gun.mode_change", new Object[]{Component.translatable(modeKey)}
                                    ),
                                    true
                                 );
                                 vars.syncModeState(player);
                              } else if ("reinforcement".equals(currentMagic)) {
                              }
                           }
                        }
                     }
                  }
               }
            )
            .exceptionally(e -> {
               TYPE_MOON_WORLD.LOGGER.error("Failed to handle MagicModeSwitchMessage", e);
               return null;
            });
      }
   }

   private static boolean isJewelModeUnlocked(TypeMoonWorldModVariables.PlayerVariables vars, int mode, String magicType) {
      if ("jewel_magic_shoot".equals(magicType)) {
         return vars.learned_magics.contains("jewel_magic_shoot") && mode >= 0 && mode <= 5;
      } else {
         return !"jewel_magic_release".equals(magicType) ? true : vars.learned_magics.contains("jewel_magic_release") && mode >= 0 && mode <= 3;
      }
   }
}
