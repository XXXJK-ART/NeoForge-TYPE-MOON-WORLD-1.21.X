package net.xxxjk.TYPE_MOON_WORLD.init;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.MagicModeSwitcherScreen;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.MagicRadialMenuScreen;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.MagicWheelSwitchScreen;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.ProjectionPresetScreen;
import net.xxxjk.TYPE_MOON_WORLD.client.projection.StructuralAnalysisSelectionClient;
import net.xxxjk.TYPE_MOON_WORLD.client.projection.StructuralProjectionPlacementClient;
import net.xxxjk.TYPE_MOON_WORLD.network.Basic_information_gui_Message;
import net.xxxjk.TYPE_MOON_WORLD.network.CastMagicMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.CycleMagicMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.Lose_health_regain_mana_Message;
import net.xxxjk.TYPE_MOON_WORLD.network.MagicCircuitSwitchMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.MagicModeSwitchMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.MysticEyesToggleMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.SwitchMagicWheelMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public class TypeMoonWorldModKeyMappings {
   public static final String KEY_CATEGORY = "key.categories.typemoonworld";
   public static final KeyMapping MAGIC_CIRCUIT_SWITCH = new KeyMapping("key.typemoonworld.magic_circuit_switch", 66, "key.categories.typemoonworld");
   public static final KeyMapping CAST_MAGIC = new KeyMapping("key.typemoonworld.cast_magic", 67, "key.categories.typemoonworld");
   public static final KeyMapping LOSE_HEALTH_REGAIN_MANA = new KeyMapping("key.typemoonworld.lose_health_regain_mana", 88, "key.categories.typemoonworld");
   public static final KeyMapping BASIC_INFORMATION_GUI = new KeyMapping("key.typemoonworld.basic_information_gui", 82, "key.categories.typemoonworld");
   public static final KeyMapping MYSTIC_EYES_ACTIVATE = new KeyMapping("key.typemoonworld.mystic_eyes_activate", 86, "key.categories.typemoonworld");
   public static final KeyMapping OPEN_PROJECTION_PRESET = new KeyMapping("key.typemoonworld.open_projection_preset", 258, "key.categories.typemoonworld");
   public static final KeyMapping CYCLE_MAGIC = new KeyMapping("key.typemoonworld.cycle_magic", 90, "key.categories.typemoonworld");
   public static final KeyMapping MAGIC_MODE_SWITCH = new KeyMapping("key.typemoonworld.magic_mode_switch", 341, "key.categories.typemoonworld");
   public static final KeyMapping MAGIC_WHEEL_SWITCH = new KeyMapping("key.typemoonworld.magic_wheel_switch", 342, "key.categories.typemoonworld");

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
      event.register(MAGIC_WHEEL_SWITCH);
   }

   @EventBusSubscriber({Dist.CLIENT})
   public static class KeyEventListener {
      private static final int RIGHT_ARM_CAST_POSE_TICKS = 6;
      private static final int MACHINE_GUN_POSE_WARMUP_TICKS = 30;
      private static final int MACHINE_GUN_POSE_STOP_NO_COOLDOWN_TICKS = 8;
      private static final long MACHINE_GUN_HOLD_RELEASE_STOP_MS = 250L;
      private static boolean isTabDown = false;
      private static boolean isCycleMagicDown = false;
      private static boolean isModeSwitchDown = false;
      private static boolean isWheelSwitchDown = false;
      private static final boolean[] numpadWheelDown = new boolean[10];
      private static long castPressStartMs = -1L;
      private static boolean castLongTriggered = false;
      private static boolean machineGunCastKeyDown = false;
      private static long machineGunCastPressStartMs = -1L;
      private static boolean machineGunReleaseStopArmed = false;
      private static boolean ganderCastKeyDown = false;
      private static long ganderCastPressStartMs = -1L;
      private static int rightArmCastPoseTicks = 0;
      private static boolean machineGunPoseLatched = false;
      private static int machineGunPoseWarmupTicks = 0;
      private static int machineGunPoseNoCooldownTicks = 0;
      private static HumanoidArm localCastingArm = HumanoidArm.RIGHT;

      @SubscribeEvent
      public static void onMouseScroll(MouseScrollingEvent event) {
         if (Minecraft.getInstance().screen == null) {
            double scrollDelta = event.getScrollDeltaY();
            if (scrollDelta == 0.0) {
               return;
            }

            if (StructuralProjectionPlacementClient.handleScroll(scrollDelta)) {
               event.setCanceled(true);
               return;
            }

            if (TypeMoonWorldModKeyMappings.MAGIC_MODE_SWITCH.isDown()) {
               Player player = Minecraft.getInstance().player;
               if (player != null) {
                  TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
                     TypeMoonWorldModVariables.PLAYER_VARIABLES
                  );
                  if (vars.is_magus && vars.is_magic_circuit_open) {
                     if (isCurrentSelectionFromCrest(vars)) {
                        event.setCanceled(true);
                        return;
                     }

                     boolean forward = scrollDelta > 0.0;
                     PacketDistributor.sendToServer(new MagicModeSwitchMessage(1, forward ? 1 : -1), new CustomPacketPayload[0]);
                     event.setCanceled(true);
                     return;
                  }
               }
            }

            if (TypeMoonWorldModKeyMappings.CYCLE_MAGIC.isDown()) {
               Player player = Minecraft.getInstance().player;
               if (player != null) {
                  TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
                     TypeMoonWorldModVariables.PLAYER_VARIABLES
                  );
                  if (vars.is_magus && vars.is_magic_circuit_open) {
                     PacketDistributor.sendToServer(new CycleMagicMessage(scrollDelta > 0.0), new CustomPacketPayload[0]);
                     event.setCanceled(true);
                  }
               }
            }
         }
      }

      @SubscribeEvent
      public static void onClientTick(Post event) {
         if (rightArmCastPoseTicks > 0) {
            rightArmCastPoseTicks--;
         }

         if (Minecraft.getInstance().screen == null) {
            Player player = Minecraft.getInstance().player;
            if (player == null) {
               return;
            }

            TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
               TypeMoonWorldModVariables.PLAYER_VARIABLES
            );
            localCastingArm = resolveLocalCastingArm(player);
            syncProjectionSelectionFromCurrentCrestPreset(player, vars);
            updateMachineGunFiringPose(vars);
            StructuralProjectionPlacementClient.cancelIfInvalid(vars);
            if (TypeMoonWorldModKeyMappings.MAGIC_MODE_SWITCH.isDown()) {
               if (!isModeSwitchDown) {
                  if (vars.is_magus
                     && vars.is_magic_circuit_open
                     && !vars.selected_magics.isEmpty()
                     && vars.current_magic_index >= 0
                     && vars.current_magic_index < vars.selected_magics.size()) {
                     String currentMagic = vars.selected_magics.get(vars.current_magic_index);
                     if (vars.isCurrentSelectionFromCrest(currentMagic)) {
                        player.displayClientMessage(Component.translatable("message.typemoonworld.crest.preset_runtime_locked"), true);
                     } else if ("sword_barrel_full_open".equals(currentMagic)) {
                        if (Minecraft.getInstance().screen == null) {
                           Minecraft.getInstance().setScreen(new MagicModeSwitcherScreen(vars.sword_barrel_mode));
                           isModeSwitchDown = true;
                        }
                     } else if (!"reinforcement".equals(currentMagic)
                        && !"reinforcement_self".equals(currentMagic)
                        && !"reinforcement_other".equals(currentMagic)
                        && !"reinforcement_item".equals(currentMagic)) {
                        if ("gravity_magic".equals(currentMagic)) {
                           if (Minecraft.getInstance().screen == null) {
                              Minecraft.getInstance().setScreen(new MagicModeSwitcherScreen(vars.gravity_magic_mode));
                              isModeSwitchDown = true;
                           }
                        } else if ("gandr_machine_gun".equals(currentMagic)) {
                           PacketDistributor.sendToServer(new MagicModeSwitchMessage(1, 1), new CustomPacketPayload[0]);
                           isModeSwitchDown = true;
                        }
                     } else if (Minecraft.getInstance().screen == null) {
                        Minecraft.getInstance().setScreen(new MagicModeSwitcherScreen(vars.reinforcement_mode));
                        isModeSwitchDown = true;
                     }
                  }

                  isModeSwitchDown = true;
               }
            } else {
               isModeSwitchDown = false;
            }

            if (TypeMoonWorldModKeyMappings.MAGIC_WHEEL_SWITCH.isDown()) {
               if (!isWheelSwitchDown) {
                  isWheelSwitchDown = true;
                  if (vars.is_magus && vars.is_magic_circuit_open && !(Minecraft.getInstance().screen instanceof MagicWheelSwitchScreen)) {
                     Minecraft.getInstance().setScreen(new MagicWheelSwitchScreen(vars.active_wheel_index));
                  }
               }
            } else {
               isWheelSwitchDown = false;
            }

            handleNumpadWheelQuickSwitch(vars);
            if (TypeMoonWorldModKeyMappings.CYCLE_MAGIC.isDown()) {
               if (!isCycleMagicDown) {
                  isCycleMagicDown = true;
                  if (vars.is_magus
                     && vars.is_magic_circuit_open
                     && !vars.selected_magics.isEmpty()
                     && !(Minecraft.getInstance().screen instanceof MagicRadialMenuScreen)) {
                     Minecraft.getInstance()
                        .setScreen(
                           new MagicRadialMenuScreen(
                              vars.selected_magics, vars.selected_magic_display_names, buildRadialSourceFlags(vars), buildRadialCrestPresetHints(vars, player)
                           )
                        );
                  }
               }
            } else {
               isCycleMagicDown = false;
            }

            if (TypeMoonWorldModKeyMappings.MAGIC_CIRCUIT_SWITCH.consumeClick() && vars.is_magus) {
               PacketDistributor.sendToServer(new MagicCircuitSwitchMessage(0, 0), new CustomPacketPayload[0]);
               MagicCircuitSwitchMessage.pressAction(player, 0, 0);
            }

            handleCastKey(player, vars);
            if (TypeMoonWorldModKeyMappings.LOSE_HEALTH_REGAIN_MANA.consumeClick()) {
               PacketDistributor.sendToServer(new Lose_health_regain_mana_Message(0, 0), new CustomPacketPayload[0]);
               Lose_health_regain_mana_Message.pressAction(player, 0, 0);
            }

            if (TypeMoonWorldModKeyMappings.BASIC_INFORMATION_GUI.consumeClick() && vars.is_magus) {
               PacketDistributor.sendToServer(new Basic_information_gui_Message(0, 0), new CustomPacketPayload[0]);
               Basic_information_gui_Message.pressAction(player, 0, 0);
            }

            if (TypeMoonWorldModKeyMappings.MYSTIC_EYES_ACTIVATE.consumeClick() && vars.is_magus) {
               PacketDistributor.sendToServer(new MysticEyesToggleMessage(0), new CustomPacketPayload[0]);
               MysticEyesToggleMessage.pressAction(player, 0);
            }

            if (TypeMoonWorldModKeyMappings.OPEN_PROJECTION_PRESET.isDown()) {
               if (!isTabDown) {
                  isTabDown = true;
                  if (vars.is_magus && vars.is_magic_circuit_open && !vars.selected_magics.isEmpty()) {
                     int index = vars.current_magic_index;
                     if (index >= 0 && index < vars.selected_magics.size()) {
                        String magicId = vars.selected_magics.get(index);
                        if ("projection".equals(magicId)
                           || "structural_analysis".equals(magicId)
                           || "unlimited_blade_works".equals(magicId)
                           || "broken_phantasm".equals(magicId)) {
                           if (vars.isCurrentSelectionFromCrest(magicId)) {
                              player.displayClientMessage(Component.translatable("message.typemoonworld.crest.preset_runtime_locked"), true);
                           } else {
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

      private static void handleCastKey(Player player, TypeMoonWorldModVariables.PlayerVariables vars) {
         syncProjectionSelectionFromCurrentCrestPreset(player, vars);
         boolean selectionActive = StructuralAnalysisSelectionClient.isActive();
         boolean structuralSelected = isStructuralAnalysisSelected(vars);
         boolean projectionStructureSelected = isStructureProjectionSelected(vars);
         if (projectionStructureSelected) {
            if (TypeMoonWorldModKeyMappings.CAST_MAGIC.consumeClick() && vars.is_magus) {
               if (!StructuralProjectionPlacementClient.isActive()) {
                  StructuralProjectionPlacementClient.startPreview();
               } else if (!StructuralProjectionPlacementClient.isLocked()) {
                  StructuralProjectionPlacementClient.confirmPlacement();
               } else {
                  StructuralProjectionPlacementClient.startProjection();
               }
            }

            machineGunCastKeyDown = false;
            machineGunCastPressStartMs = -1L;
            machineGunReleaseStopArmed = false;
            machineGunPoseLatched = false;
            machineGunPoseWarmupTicks = 0;
            machineGunPoseNoCooldownTicks = 0;
            ganderCastKeyDown = false;
            ganderCastPressStartMs = -1L;
            castPressStartMs = -1L;
            castLongTriggered = false;
         } else if (!selectionActive && !structuralSelected) {
            if (isGanderSelected(vars)) {
               long now = System.currentTimeMillis();
               if (isFireInputDown(vars)) {
                  if (!ganderCastKeyDown && vars.is_magus && EntityUtils.hasAnyEmptyHand(player)) {
                     ganderCastKeyDown = true;
                     ganderCastPressStartMs = now;
                     triggerCast(player, 1, 0);
                  }
               } else {
                  if (ganderCastKeyDown && vars.is_magus) {
                     long holdMs = ganderCastPressStartMs < 0L ? 0L : now - ganderCastPressStartMs;
                     triggerCast(player, 2, (int)Math.min(2147483647L, holdMs));
                  }

                  ganderCastKeyDown = false;
                  ganderCastPressStartMs = -1L;
               }

               machineGunCastKeyDown = false;
               machineGunCastPressStartMs = -1L;
               machineGunReleaseStopArmed = false;
            } else if (isJewelMachineGunSelected(vars)) {
               long now = System.currentTimeMillis();
               boolean fireDown = isFireInputDown(vars);
               if (fireDown && !machineGunCastKeyDown) {
                  machineGunCastPressStartMs = now;
                  if (vars.is_magus && EntityUtils.hasAnyEmptyHand(player)) {
                     boolean likelyStopping = machineGunPoseLatched || vars.magic_cooldown > 0.0;
                     machineGunPoseLatched = !likelyStopping;
                     machineGunPoseWarmupTicks = machineGunPoseLatched ? 30 : 0;
                     machineGunPoseNoCooldownTicks = 0;
                     machineGunReleaseStopArmed = !likelyStopping;
                     triggerCast(player, 0, 0);
                  } else {
                     machineGunReleaseStopArmed = false;
                  }
               } else if (!fireDown && machineGunCastKeyDown) {
                  long holdMs = machineGunCastPressStartMs < 0L ? 0L : now - machineGunCastPressStartMs;
                  if (vars.is_magus && machineGunReleaseStopArmed && holdMs >= 250L) {
                     machineGunPoseLatched = false;
                     machineGunPoseWarmupTicks = 0;
                     machineGunPoseNoCooldownTicks = 0;
                     triggerCast(player, 0, (int)Math.min(2147483647L, holdMs));
                  }

                  machineGunCastPressStartMs = -1L;
                  machineGunReleaseStopArmed = false;
               }

               machineGunCastKeyDown = fireDown;
               ganderCastKeyDown = false;
               ganderCastPressStartMs = -1L;
            } else {
               machineGunCastKeyDown = false;
               machineGunCastPressStartMs = -1L;
               machineGunReleaseStopArmed = false;
               machineGunPoseLatched = false;
               machineGunPoseWarmupTicks = 0;
               machineGunPoseNoCooldownTicks = 0;
               ganderCastKeyDown = false;
               ganderCastPressStartMs = -1L;
               if (TypeMoonWorldModKeyMappings.CAST_MAGIC.consumeClick() && vars.is_magus) {
                  triggerRightArmCastPose(vars);
                  triggerCast(player, 0, 0);
               }
            }

            castPressStartMs = -1L;
            castLongTriggered = false;
         } else {
            machineGunCastKeyDown = false;
            machineGunCastPressStartMs = -1L;
            machineGunReleaseStopArmed = false;
            machineGunPoseLatched = false;
            machineGunPoseWarmupTicks = 0;
            machineGunPoseNoCooldownTicks = 0;
            ganderCastKeyDown = false;
            ganderCastPressStartMs = -1L;
            long now = System.currentTimeMillis();
            if (TypeMoonWorldModKeyMappings.CAST_MAGIC.isDown()) {
               if (castPressStartMs < 0L) {
                  castPressStartMs = now;
               }

               if (!selectionActive && structuralSelected && !castLongTriggered && now - castPressStartMs >= 1000L) {
                  StructuralAnalysisSelectionClient.startSelectionFromCrosshair();
                  castLongTriggered = true;
               }
            } else {
               if (castPressStartMs >= 0L) {
                  if (!castLongTriggered) {
                     if (StructuralAnalysisSelectionClient.isActive()) {
                        StructuralAnalysisSelectionClient.confirmWithCrosshair();
                     } else if (vars.is_magus) {
                        triggerCast(player, 0, 0);
                     }
                  }

                  castPressStartMs = -1L;
                  castLongTriggered = false;
               }
            }
         }
      }

      private static void handleNumpadWheelQuickSwitch(TypeMoonWorldModVariables.PlayerVariables vars) {
         if (vars.is_magus && vars.is_magic_circuit_open) {
            if (Minecraft.getInstance().screen == null) {
               long window = Minecraft.getInstance().getWindow().getWindow();
               int[] keys = new int[]{320, 321, 322, 323, 324, 325, 326, 327, 328, 329};

               for (int wheel = 0; wheel < keys.length; wheel++) {
                  boolean down = GLFW.glfwGetKey(window, keys[wheel]) == 1;
                  if (down && !numpadWheelDown[wheel]) {
                     PacketDistributor.sendToServer(new SwitchMagicWheelMessage(wheel), new CustomPacketPayload[0]);
                  }

                  numpadWheelDown[wheel] = down;
               }
            }
         } else {
            for (int i = 0; i < numpadWheelDown.length; i++) {
               numpadWheelDown[i] = false;
            }
         }
      }

      private static void triggerCast(Player player, int eventType, int pressedMs) {
         PacketDistributor.sendToServer(new CastMagicMessage(eventType, pressedMs), new CustomPacketPayload[0]);
         CastMagicMessage.pressAction(player, eventType, pressedMs);
      }

      private static void triggerRightArmCastPose(TypeMoonWorldModVariables.PlayerVariables vars) {
         int poseTicks = getTapCastPoseTicks(vars);
         if (poseTicks > 0) {
            rightArmCastPoseTicks = Math.max(rightArmCastPoseTicks, poseTicks);
         }
      }

      private static void updateMachineGunFiringPose(TypeMoonWorldModVariables.PlayerVariables vars) {
         if (!isJewelMachineGunSelected(vars)) {
            machineGunPoseLatched = false;
            machineGunPoseWarmupTicks = 0;
            machineGunPoseNoCooldownTicks = 0;
         } else if (machineGunPoseLatched) {
            if (machineGunPoseWarmupTicks > 0) {
               machineGunPoseWarmupTicks--;
               machineGunPoseNoCooldownTicks = 0;
            } else if (vars.magic_cooldown > 0.0) {
               machineGunPoseNoCooldownTicks = 0;
            } else {
               machineGunPoseNoCooldownTicks++;
               if (machineGunPoseNoCooldownTicks > 8) {
                  machineGunPoseLatched = false;
                  machineGunPoseWarmupTicks = 0;
                  machineGunPoseNoCooldownTicks = 0;
               }
            }
         }
      }

      private static int getTapCastPoseTicks(TypeMoonWorldModVariables.PlayerVariables vars) {
         if (!vars.is_magus || !vars.is_magic_circuit_open) {
            return 0;
         } else if (vars.selected_magics.isEmpty()) {
            return 0;
         } else {
            int index = vars.current_magic_index;
            if (index >= 0 && index < vars.selected_magics.size()) {
               String magicId = vars.selected_magics.get(index);

               return switch (magicId) {
                  case "jewel_random_shoot", "broken_phantasm" -> 6;
                  default -> 0;
               };
            } else {
               return 0;
            }
         }
      }

      private static boolean isFireInputDown(TypeMoonWorldModVariables.PlayerVariables vars) {
         if (TypeMoonWorldModKeyMappings.CAST_MAGIC.isDown()) {
            return true;
         } else if (!isGunLikeSpellSelected(vars)) {
            return false;
         } else {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft.options != null && minecraft.options.keyAttack.isDown();
         }
      }

      private static boolean isGunLikeSpellSelected(TypeMoonWorldModVariables.PlayerVariables vars) {
         return isGanderSelected(vars) || isJewelMachineGunSelected(vars);
      }

      private static HumanoidArm resolveLocalCastingArm(Player player) {
         HumanoidArm arm = EntityUtils.resolveEmptyCastingArm(player);
         return arm == null ? player.getMainArm() : arm;
      }

      private static List<Boolean> buildRadialSourceFlags(TypeMoonWorldModVariables.PlayerVariables vars) {
         List<Boolean> flags = new ArrayList<>(vars.selected_magics.size());

         for (int i = 0; i < vars.selected_magics.size(); i++) {
            TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = getRuntimeWheelEntry(vars, i);
            flags.add(entry != null && "crest".equals(entry.sourceType));
         }

         return flags;
      }

      private static List<String> buildRadialCrestPresetHints(TypeMoonWorldModVariables.PlayerVariables vars, Player player) {
         List<String> hints = new ArrayList<>(vars.selected_magics.size());

         for (int i = 0; i < vars.selected_magics.size(); i++) {
            TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = getRuntimeWheelEntry(vars, i);
            if (entry != null && "crest".equals(entry.sourceType)) {
               hints.add(buildCrestPresetHintForEntry(entry, vars, player));
            } else {
               hints.add("");
            }
         }

         return hints;
      }

      private static TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry getRuntimeWheelEntry(
         TypeMoonWorldModVariables.PlayerVariables vars, int runtimeIndex
      ) {
         if (runtimeIndex >= 0 && runtimeIndex < vars.selected_magic_runtime_slot_indices.size()) {
            int slot = vars.selected_magic_runtime_slot_indices.get(runtimeIndex);
            return vars.getWheelSlotEntry(vars.active_wheel_index, slot);
         } else {
            return null;
         }
      }

      private static String buildCrestPresetHintForEntry(
         TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry, TypeMoonWorldModVariables.PlayerVariables vars, Player player
      ) {
         if (entry != null && entry.magicId != null && !entry.magicId.isEmpty()) {
            CompoundTag payload = entry.presetPayload == null ? new CompoundTag() : entry.presetPayload;
            if (payload.isEmpty()) {
               return "";
            } else {
               String var4 = entry.magicId;

               return switch (var4) {
                  case "reinforcement" -> buildReinforcementHint(payload);
                  case "gravity_magic" -> buildGravityHint(payload);
                  case "gandr_machine_gun" -> buildGandrMachineGunHint(payload);
                  case "projection" -> buildProjectionHint(payload, vars, player);
                  default -> "";
               };
            }
         } else {
            return "";
         }
      }

      private static String buildReinforcementHint(CompoundTag payload) {
         int target = payload.contains("reinforcement_target") ? payload.getInt("reinforcement_target") : 0;
         int mode = payload.contains("reinforcement_mode") ? payload.getInt("reinforcement_mode") : 0;
         int level = payload.contains("reinforcement_level") ? payload.getInt("reinforcement_level") : 1;

         String targetKey = switch (target) {
            case 1 -> "gui.typemoonworld.overlay.reinforcement.target.other.short";
            case 2 -> "gui.typemoonworld.overlay.reinforcement.target.item.short";
            case 3 -> "gui.typemoonworld.overlay.reinforcement.target.cancel.short";
            default -> "gui.typemoonworld.overlay.reinforcement.target.self.short";
         };
         if (target == 3) {
            String cancelKey = switch (mode) {
               case 1 -> "gui.typemoonworld.overlay.reinforcement.cancel.other.short";
               case 2 -> "gui.typemoonworld.overlay.reinforcement.cancel.item.short";
               default -> "gui.typemoonworld.overlay.reinforcement.cancel.self.short";
            };
            return Component.translatable(targetKey).getString() + "/" + Component.translatable(cancelKey).getString();
         } else if (target == 2) {
            return Component.translatable(targetKey).getString() + " L" + Math.max(1, Math.min(5, level));
         } else {
            String partKey = switch (mode) {
               case 1 -> "gui.typemoonworld.overlay.reinforcement.part.arm.short";
               case 2 -> "gui.typemoonworld.overlay.reinforcement.part.leg.short";
               case 3 -> "gui.typemoonworld.overlay.reinforcement.part.eye.short";
               default -> "gui.typemoonworld.overlay.reinforcement.part.body.short";
            };
            return Component.translatable(targetKey).getString() + "/" + Component.translatable(partKey).getString() + " L" + Math.max(1, Math.min(5, level));
         }
      }

      private static String buildGravityHint(CompoundTag payload) {
         int target = payload.contains("gravity_target") ? payload.getInt("gravity_target") : 0;
         int mode = payload.contains("gravity_mode") ? payload.getInt("gravity_mode") : 0;
         String targetKey = target == 0 ? "gui.typemoonworld.overlay.gravity.target.self.short" : "gui.typemoonworld.overlay.gravity.target.other.short";

         String modeKey = switch (mode) {
            case -2 -> "gui.typemoonworld.overlay.gravity.mode.ultra_light.short";
            case -1 -> "gui.typemoonworld.overlay.gravity.mode.light.short";
            default -> "gui.typemoonworld.overlay.gravity.mode.normal.short";
            case 1 -> "gui.typemoonworld.overlay.gravity.mode.heavy.short";
            case 2 -> "gui.typemoonworld.overlay.gravity.mode.ultra_heavy.short";
         };
         return Component.translatable(targetKey).getString() + "/" + Component.translatable(modeKey).getString();
      }

      private static String buildGandrMachineGunHint(CompoundTag payload) {
         int mode = payload.contains("gandr_machine_gun_mode") ? payload.getInt("gandr_machine_gun_mode") : 0;
         String modeKey = mode == 1 ? "gui.typemoonworld.overlay.gandr.mode.barrage.short" : "gui.typemoonworld.overlay.gandr.mode.rapid.short";
         return Component.translatable(modeKey).getString();
      }

      private static String buildProjectionHint(CompoundTag payload, TypeMoonWorldModVariables.PlayerVariables vars, Player player) {
         if (payload.getBoolean("projection_lock_empty")) {
            return "EMPTY";
         } else if (!payload.contains("projection_structure_id")) {
            if (payload.contains("projection_item", 10) && player != null) {
               ItemStack projectionItem = ItemStack.parse(player.registryAccess(), payload.getCompound("projection_item")).orElse(ItemStack.EMPTY);
               String mode = Component.translatable("gui.typemoonworld.gem_carving_table.projection_mode.item").getString();
               return !projectionItem.isEmpty() ? mode + ":" + projectionItem.getHoverName().getString() : mode;
            } else {
               return "";
            }
         } else {
            String structureId = payload.getString("projection_structure_id");
            TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure = vars.getStructureById(structureId);
            String structureName = structure != null && structure.name != null && !structure.name.isEmpty() ? structure.name : structureId;
            return Component.translatable("gui.typemoonworld.gem_carving_table.projection_mode.structure").getString() + ":" + structureName;
         }
      }

      private static boolean isStructuralAnalysisSelected(TypeMoonWorldModVariables.PlayerVariables vars) {
         if (!vars.is_magus || !vars.is_magic_circuit_open) {
            return false;
         } else if (vars.selected_magics.isEmpty()) {
            return false;
         } else {
            int index = vars.current_magic_index;
            return index >= 0 && index < vars.selected_magics.size() ? "structural_analysis".equals(vars.selected_magics.get(index)) : false;
         }
      }

      private static boolean isStructureProjectionSelected(TypeMoonWorldModVariables.PlayerVariables vars) {
         if (!vars.is_magus || !vars.is_magic_circuit_open) {
            return false;
         } else if (vars.selected_magics.isEmpty()) {
            return false;
         } else {
            int index = vars.current_magic_index;
            if (index < 0 || index >= vars.selected_magics.size()) {
               return false;
            } else if (!"projection".equals(vars.selected_magics.get(index))) {
               return false;
            } else {
               if (vars.isCurrentSelectionFromCrest("projection")) {
                  TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = getRuntimeWheelEntry(vars, index);
                  if (entry != null) {
                     CompoundTag payload = TypeMoonWorldModVariables.PlayerVariables.normalizeProjectionPresetPayload(entry.presetPayload);
                     return payload.contains("projection_structure_id") && !payload.getString("projection_structure_id").isEmpty();
                  }
               }

               return vars.projection_selected_structure_id != null && !vars.projection_selected_structure_id.isEmpty();
            }
         }
      }

      private static void syncProjectionSelectionFromCurrentCrestPreset(Player player, TypeMoonWorldModVariables.PlayerVariables vars) {
         if (player != null && vars != null) {
            if (vars.is_magus && vars.is_magic_circuit_open && !vars.selected_magics.isEmpty()) {
               int runtimeIndex = vars.current_magic_index;
               if (runtimeIndex >= 0 && runtimeIndex < vars.selected_magics.size()) {
                  if ("projection".equals(vars.selected_magics.get(runtimeIndex))) {
                     if (vars.isCurrentSelectionFromCrest("projection")) {
                        TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = getRuntimeWheelEntry(vars, runtimeIndex);
                        if (entry != null) {
                           CompoundTag payload = TypeMoonWorldModVariables.PlayerVariables.normalizeProjectionPresetPayload(entry.presetPayload);
                           if (payload.getBoolean("projection_lock_empty")) {
                              vars.projection_selected_structure_id = "";
                              vars.projection_selected_item = ItemStack.EMPTY;
                           } else if (payload.contains("projection_structure_id")) {
                              vars.projection_selected_structure_id = payload.getString("projection_structure_id");
                              vars.projection_selected_item = ItemStack.EMPTY;
                           } else {
                              if (payload.contains("projection_item", 10)) {
                                 ItemStack parsed = ItemStack.parse(player.registryAccess(), payload.getCompound("projection_item")).orElse(ItemStack.EMPTY);
                                 vars.projection_selected_item = parsed;
                                 vars.projection_selected_structure_id = "";
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      private static boolean isJewelMachineGunSelected(TypeMoonWorldModVariables.PlayerVariables vars) {
         if (!vars.is_magus || !vars.is_magic_circuit_open) {
            return false;
         } else if (vars.selected_magics.isEmpty()) {
            return false;
         } else {
            int index = vars.current_magic_index;
            if (index >= 0 && index < vars.selected_magics.size()) {
               String magicId = vars.selected_magics.get(index);
               return "jewel_machine_gun".equals(magicId) || "gandr_machine_gun".equals(magicId);
            } else {
               return false;
            }
         }
      }

      private static boolean isGanderSelected(TypeMoonWorldModVariables.PlayerVariables vars) {
         if (!vars.is_magus || !vars.is_magic_circuit_open) {
            return false;
         } else if (vars.selected_magics.isEmpty()) {
            return false;
         } else {
            int index = vars.current_magic_index;
            return index >= 0 && index < vars.selected_magics.size() ? "gander".equals(vars.selected_magics.get(index)) : false;
         }
      }

      private static boolean isCurrentSelectionFromCrest(TypeMoonWorldModVariables.PlayerVariables vars) {
         if (!vars.is_magus || !vars.is_magic_circuit_open) {
            return false;
         } else if (vars.selected_magics.isEmpty()) {
            return false;
         } else {
            int index = vars.current_magic_index;
            return index >= 0 && index < vars.selected_magics.size() ? vars.isCurrentSelectionFromCrest(vars.selected_magics.get(index)) : false;
         }
      }

      public static boolean isLocalGanderCharging() {
         return ganderCastKeyDown;
      }

      public static boolean isLocalGandrMachineGunCasting() {
         if (!machineGunCastKeyDown) {
            return false;
         } else {
            Player player = Minecraft.getInstance().player;
            if (player == null) {
               return false;
            } else {
               TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
                  TypeMoonWorldModVariables.PLAYER_VARIABLES
               );
               if (vars.selected_magics.isEmpty()) {
                  return false;
               } else {
                  int index = vars.current_magic_index;
                  return index >= 0 && index < vars.selected_magics.size() ? "gandr_machine_gun".equals(vars.selected_magics.get(index)) : false;
               }
            }
         }
      }

      public static boolean isLocalTapCastPoseActive() {
         return rightArmCastPoseTicks > 0;
      }

      public static boolean isLocalMachineGunFiringPoseActive() {
         return machineGunPoseLatched;
      }

      public static HumanoidArm getLocalCastingArm() {
         return localCastingArm;
      }
   }
}
