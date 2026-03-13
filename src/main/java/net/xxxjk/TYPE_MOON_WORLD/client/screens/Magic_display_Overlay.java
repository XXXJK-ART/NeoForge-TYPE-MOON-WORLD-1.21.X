package net.xxxjk.TYPE_MOON_WORLD.client.screens;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent.Pre;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

@EventBusSubscriber({Dist.CLIENT})
public class Magic_display_Overlay {
   @SubscribeEvent(
      priority = EventPriority.NORMAL
   )
   public static void eventHandler(Pre event) {
      int h = event.getGuiGraphics().guiHeight();
      Player entity = Minecraft.getInstance().player;
      if (entity != null) {
         try {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)entity.getData(
               TypeMoonWorldModVariables.PLAYER_VARIABLES
            );
            if (vars.is_magus) {
               double currentMana = vars.player_mana;
               double maxMana = vars.player_max_mana;
               int barWidth = 120;
               int barHeight = 10;
               int barX = 10;
               int barY = h - 20;
               event.getGuiGraphics().fill(barX, barY, barX + barWidth, barY + barHeight, Integer.MIN_VALUE);
               int startColor = -16718337;
               int endColor = -14059009;
               if (currentMana <= maxMana * 0.2) {
                  startColor = -49152;
                  endColor = -65536;
               } else if (currentMana > maxMana) {
                  startColor = -65281;
                  endColor = -6487809;
               }

               if (maxMana > 0.0) {
                  double ratio = Math.min(1.0, Math.max(0.0, currentMana / maxMana));
                  int fillWidth = (int)(barWidth * ratio);
                  event.getGuiGraphics().fillGradient(barX, barY, barX + fillWidth, barY + barHeight, startColor, endColor);
               }

               event.getGuiGraphics().fill(barX - 1, barY - 1, barX + barWidth + 1, barY, -1);
               event.getGuiGraphics().fill(barX - 1, barY + barHeight, barX + barWidth + 1, barY + barHeight + 1, -1);
               event.getGuiGraphics().fill(barX - 1, barY - 1, barX, barY + barHeight + 1, -1);
               event.getGuiGraphics().fill(barX + barWidth, barY - 1, barX + barWidth + 1, barY + barHeight + 1, -1);
               String manaText = (int)currentMana + " / " + (int)maxMana;
               int textWidth = Minecraft.getInstance().font.width(manaText);
               int textX = barX + (barWidth - textWidth) / 2;
               int textY = barY + (barHeight - 8) / 2 + 1;
               event.getGuiGraphics().drawString(Minecraft.getInstance().font, manaText, textX, textY, -1, true);
               int iconX = barX - 4;
               int iconY = barY - 3;
               event.getGuiGraphics().blit(ResourceLocation.parse("typemoonworld:textures/screens/mana.png"), iconX, iconY, 0.0F, 0.0F, 16, 16, 16, 16);
               if (!vars.is_magic_circuit_open) {
                  return;
               }

               MutableComponent magicName = Component.translatable("gui.typemoonworld.mode.none");
               int magicColor = -16711681;
               if (!vars.selected_magics.isEmpty() && vars.current_magic_index >= 0 && vars.current_magic_index < vars.selected_magics.size()) {
                  String magicId = vars.selected_magics.get(vars.current_magic_index);
                  String translationKey = "magic.typemoonworld." + magicId + ".name";
                  magicName = Component.translatable(translationKey);
                  TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry runtimeEntry = vars.getCurrentRuntimeWheelEntry();
                  boolean fromCrest = runtimeEntry != null && "crest".equals(runtimeEntry.sourceType) && vars.isWheelSlotEntryCastable(runtimeEntry);
                  CompoundTag crestPayload = fromCrest && runtimeEntry.presetPayload != null ? runtimeEntry.presetPayload.copy() : new CompoundTag();
                  int selectedIndex = vars.current_magic_index;
                  if (selectedIndex >= 0 && selectedIndex < vars.selected_magic_display_names.size()) {
                     String displayName = vars.selected_magic_display_names.get(selectedIndex);
                     if (displayName != null && !displayName.isEmpty()) {
                        magicName = Component.literal(displayName);
                     }
                  }

                  if (magicId.startsWith("ruby")) {
                     magicColor = -65536;
                  } else if (magicId.startsWith("sapphire")) {
                     magicColor = -16742145;
                  } else if (magicId.startsWith("emerald")) {
                     magicColor = -16711936;
                  } else if (magicId.startsWith("topaz")) {
                     magicColor = -256;
                  } else if ("projection".equals(magicId)) {
                     magicColor = -16711681;
                     if (fromCrest) {
                        String hint = buildProjectionHint(crestPayload, vars, entity);
                        if (!hint.isEmpty()) {
                           magicName = Component.literal(magicName.getString() + "[" + hint + "]");
                        }
                     }
                  } else if ("structural_analysis".equals(magicId)) {
                     magicColor = -16711681;
                  } else if ("broken_phantasm".equals(magicId)) {
                     magicColor = -49152;
                  } else if ("unlimited_blade_works".equals(magicId)) {
                     magicColor = -65536;
                  } else if ("sword_barrel_full_open".equals(magicId)) {
                     magicColor = -65536;
                     magicName = Component.translatable(
                        "gui.typemoonworld.overlay.mode_with_index", new Object[]{Component.translatable(translationKey), vars.sword_barrel_mode}
                     );
                  } else if ("gravity_magic".equals(magicId)) {
                     magicColor = -7701249;
                     int gravityTarget = vars.gravity_magic_target;
                     int gravityMode = vars.gravity_magic_mode;
                     if (fromCrest) {
                        if (crestPayload.contains("gravity_target")) {
                           gravityTarget = Mth.clamp(crestPayload.getInt("gravity_target"), 0, 1);
                        }

                        if (crestPayload.contains("gravity_mode")) {
                           gravityMode = Mth.clamp(crestPayload.getInt("gravity_mode"), -2, 2);
                        }
                     }

                     String targetKey = gravityTarget == 0
                        ? "gui.typemoonworld.overlay.gravity.target.self.short"
                        : "gui.typemoonworld.overlay.gravity.target.other.short";

                     String modeKey = switch (gravityMode) {
                        case -2 -> "gui.typemoonworld.overlay.gravity.mode.ultra_light.short";
                        case -1 -> "gui.typemoonworld.overlay.gravity.mode.light.short";
                        default -> "gui.typemoonworld.overlay.gravity.mode.normal.short";
                        case 1 -> "gui.typemoonworld.overlay.gravity.mode.heavy.short";
                        case 2 -> "gui.typemoonworld.overlay.gravity.mode.ultra_heavy.short";
                     };
                     magicName = Component.translatable(
                        "gui.typemoonworld.overlay.gravity.format",
                        new Object[]{Component.translatable(translationKey), Component.translatable(targetKey), Component.translatable(modeKey)}
                     );
                  } else if ("gander".equals(magicId)) {
                     magicColor = -5230544;
                  } else if ("jewel_random_shoot".equals(magicId)) {
                     magicColor = -1381654;
                  } else if ("jewel_machine_gun".equals(magicId)) {
                     magicColor = -65281;
                  } else if ("gandr_machine_gun".equals(magicId)) {
                     magicColor = -4173744;
                     int gandrMode = vars.gandr_machine_gun_mode;
                     if (fromCrest && crestPayload.contains("gandr_machine_gun_mode")) {
                        gandrMode = Mth.clamp(crestPayload.getInt("gandr_machine_gun_mode"), 0, 1);
                     }

                     String modeKey = gandrMode == 1
                        ? "gui.typemoonworld.overlay.gandr.mode.barrage.short"
                        : "gui.typemoonworld.overlay.gandr.mode.rapid.short";
                     magicName = Component.translatable(
                        "gui.typemoonworld.overlay.gandr.format", new Object[]{Component.translatable(translationKey), Component.translatable(modeKey)}
                     );
                  } else if ("reinforcement".equals(magicId)
                     || "reinforcement_self".equals(magicId)
                     || "reinforcement_other".equals(magicId)
                     || "reinforcement_item".equals(magicId)) {
                     magicColor = -16733696;
                     int reinforcementTarget = vars.reinforcement_target;
                     int reinforcementMode = vars.reinforcement_mode;
                     int reinforcementLevel = vars.reinforcement_level;
                     if (fromCrest) {
                        if (crestPayload.contains("reinforcement_target")) {
                           reinforcementTarget = Mth.clamp(crestPayload.getInt("reinforcement_target"), 0, 3);
                        }

                        if (crestPayload.contains("reinforcement_mode")) {
                           reinforcementMode = Mth.clamp(crestPayload.getInt("reinforcement_mode"), 0, 3);
                        }

                        if (crestPayload.contains("reinforcement_level")) {
                           reinforcementLevel = Mth.clamp(crestPayload.getInt("reinforcement_level"), 1, 5);
                        }
                     }
                     String targetKey = switch (reinforcementTarget) {
                        case 0 -> "gui.typemoonworld.overlay.reinforcement.target.self.short";
                        case 1 -> "gui.typemoonworld.overlay.reinforcement.target.other.short";
                        case 2 -> "gui.typemoonworld.overlay.reinforcement.target.item.short";
                        case 3 -> "gui.typemoonworld.overlay.reinforcement.target.cancel.short";
                        default -> "gui.typemoonworld.overlay.reinforcement.unknown.short";
                     };

                     String partKey = switch (reinforcementMode) {
                        case 0 -> "gui.typemoonworld.overlay.reinforcement.part.body.short";
                        case 1 -> "gui.typemoonworld.overlay.reinforcement.part.arm.short";
                        case 2 -> "gui.typemoonworld.overlay.reinforcement.part.leg.short";
                        case 3 -> "gui.typemoonworld.overlay.reinforcement.part.eye.short";
                        default -> "gui.typemoonworld.overlay.reinforcement.unknown.short";
                     };

                     String cancelKey = switch (reinforcementMode) {
                        case 0 -> "gui.typemoonworld.overlay.reinforcement.cancel.self.short";
                        case 1 -> "gui.typemoonworld.overlay.reinforcement.cancel.other.short";
                        case 2 -> "gui.typemoonworld.overlay.reinforcement.cancel.item.short";
                        default -> "gui.typemoonworld.overlay.reinforcement.unknown.short";
                     };
                     Component targetShort = Component.translatable(targetKey);
                     Component partShort = Component.translatable(partKey);
                     Component cancelShort = Component.translatable(cancelKey);
                     if (reinforcementTarget == 3) {
                        magicName = Component.translatable("gui.typemoonworld.overlay.reinforcement.format.cancel", new Object[]{targetShort, cancelShort});
                     } else if (reinforcementTarget == 2) {
                        magicName = Component.translatable("gui.typemoonworld.overlay.reinforcement.format.item", new Object[]{targetShort, reinforcementLevel});
                     } else {
                        magicName = Component.translatable(
                           "gui.typemoonworld.overlay.reinforcement.format.part", new Object[]{targetShort, partShort, reinforcementLevel}
                        );
                     }
                  }

                  Component sourceLabel = Component.translatable(
                     fromCrest ? "gui.typemoonworld.overlay.source.crest.short" : "gui.typemoonworld.overlay.source.self.short"
                  );
                  magicName = Component.translatable("gui.typemoonworld.overlay.magic_with_source", new Object[]{sourceLabel, magicName});
               }

               Component labelStr = Component.translatable("gui.typemoonworld.overlay.current_magic");
               int magicTextY = barY - 12;
               event.getGuiGraphics().drawString(Minecraft.getInstance().font, labelStr, barX, magicTextY, -1, true);
               event.getGuiGraphics()
                  .drawString(Minecraft.getInstance().font, magicName, barX + Minecraft.getInstance().font.width(labelStr), magicTextY, magicColor, true);
               return;
            }
         } finally {
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }
      }
   }

   private static String buildProjectionHint(CompoundTag payload, TypeMoonWorldModVariables.PlayerVariables vars, Player player) {
      if (payload == null || payload.isEmpty()) {
         return "";
      } else if (payload.getBoolean("projection_lock_empty")) {
         return "EMPTY";
      } else if (!payload.contains("projection_structure_id")) {
         if (payload.contains("projection_item", 10) && player != null) {
            ItemStack projectionItem = ItemStack.parse(player.registryAccess(), payload.getCompound("projection_item")).orElse(ItemStack.EMPTY);
            if (!projectionItem.isEmpty()) {
               return projectionItem.getHoverName().getString();
            }
         }

         return "";
      } else {
         String structureId = payload.getString("projection_structure_id");
         TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure = vars.getStructureById(structureId);
         return structure != null && structure.name != null && !structure.name.isEmpty() ? structure.name : structureId;
      }
   }
}
