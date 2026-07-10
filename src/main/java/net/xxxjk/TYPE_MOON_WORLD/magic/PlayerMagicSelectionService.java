package net.xxxjk.TYPE_MOON_WORLD.magic;

import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class PlayerMagicSelectionService {
   private PlayerMagicSelectionService() {
   }

   public static TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry getCurrentEntry(TypeMoonWorldModVariables.PlayerVariables vars) {
      return vars == null ? null : vars.getCurrentRuntimeWheelEntry();
   }

   public static String getCurrentMagicId(TypeMoonWorldModVariables.PlayerVariables vars) {
      TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = getCurrentEntry(vars);
      return entry != null && entry.magicId != null ? entry.magicId : "";
   }

   public static boolean isCurrentSelection(TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = getCurrentEntry(vars);
      return entry != null && Objects.equals(entry.magicId, magicId) && vars.isWheelSlotEntryCastable(entry);
   }

   public static void initializeSelfPresetIfNeeded(Entity entity, TypeMoonWorldModVariables.PlayerVariables vars, TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry) {
      if (vars == null || entry == null || !"self".equals(entry.sourceType)) {
         return;
      }

      CompoundTag payload = normalizePresetPayload(entry.magicId, entry.presetPayload);
      if (payload.isEmpty() && supportsRuntimePreset(entry.magicId)) {
         payload = buildPresetFromCurrentVars(entity, vars, entry.magicId);
      }

      entry.presetPayload = payload;
   }

   public static void applyCurrentSelectionPreset(Entity entity, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars == null) {
         return;
      }

      TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = getCurrentEntry(vars);
      if (entry == null || !vars.isWheelSlotEntryCastable(entry)) {
         return;
      }

      applyPresetToVars(entity, vars, entry.magicId, normalizePresetPayload(entry.magicId, entry.presetPayload));
   }

   public static boolean prepareCurrentSelection(Entity entity, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars == null) {
         return false;
      }

      TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = getCurrentEntry(vars);
      if (entry == null || entry.isEmpty()) {
         return false;
      }

      CompoundTag before = entry.presetPayload == null ? new CompoundTag() : entry.presetPayload.copy();
      initializeSelfPresetIfNeeded(entity, vars, entry);
      CompoundTag after = entry.presetPayload == null ? new CompoundTag() : entry.presetPayload.copy();
      boolean changed = !before.equals(after);
      if (changed) {
         vars.setWheelSlotEntry(entry.wheelIndex, entry.slotIndex, entry);
         vars.rebuildSelectedMagicsFromActiveWheel();
      }

      applyCurrentSelectionPreset(entity, vars);
      return changed;
   }

   public static void syncCurrentSelection(Entity entity, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars == null) {
         return;
      }

      if (prepareCurrentSelection(entity, vars)) {
         vars.syncPlayerVariables(entity);
      }

      vars.syncRuntimeSelection(entity);
      vars.syncModeState(entity);
   }

   public static boolean persistCurrentSelectionPreset(Entity entity, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars == null) {
         return false;
      }

      TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = getCurrentEntry(vars);
      if (entry == null || entry.isEmpty() || !"self".equals(entry.sourceType) || !supportsRuntimePreset(entry.magicId)) {
         return false;
      }

      TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry updated = entry.copy();
      CompoundTag nextPayload = buildPresetFromCurrentVars(entity, vars, updated.magicId);
      CompoundTag currentPayload = updated.presetPayload == null ? new CompoundTag() : updated.presetPayload.copy();
      if (nextPayload.equals(currentPayload)) {
         return false;
      }

      updated.presetPayload = nextPayload;
      vars.setWheelSlotEntry(updated.wheelIndex, updated.slotIndex, updated);
      vars.rebuildSelectedMagicsFromActiveWheel();
      applyCurrentSelectionPreset(entity, vars);
      return true;
   }

   public static void syncPresetMutation(Entity entity, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars == null) {
         return;
      }

      if (persistCurrentSelectionPreset(entity, vars)) {
         vars.syncPlayerVariables(entity);
      }

      vars.syncRuntimeSelection(entity);
      vars.syncModeState(entity);
   }

   public static CompoundTag normalizePresetPayload(String magicId, CompoundTag payload) {
      CompoundTag normalized = payload == null ? new CompoundTag() : payload.copy();
      if ("projection".equals(magicId)) {
         return TypeMoonWorldModVariables.PlayerVariables.normalizeProjectionPresetPayload(normalized);
      }

      if ("reinforcement".equals(magicId)) {
         if (normalized.contains("reinforcement_target")) {
            normalized.putInt("reinforcement_target", clamp(normalized.getInt("reinforcement_target"), 0, 3));
         }

         if (normalized.contains("reinforcement_mode")) {
            normalized.putInt("reinforcement_mode", clamp(normalized.getInt("reinforcement_mode"), 0, 3));
         }

         if (normalized.contains("reinforcement_level")) {
            normalized.putInt("reinforcement_level", clamp(normalized.getInt("reinforcement_level"), 1, 5));
         }
      } else if ("gravity_magic".equals(magicId)) {
         if (normalized.contains("gravity_target")) {
            normalized.putInt("gravity_target", clamp(normalized.getInt("gravity_target"), 0, 1));
         }

         if (normalized.contains("gravity_mode")) {
            normalized.putInt("gravity_mode", clamp(normalized.getInt("gravity_mode"), -2, 2));
         }
      } else if ("gandr_machine_gun".equals(magicId) && normalized.contains("gandr_machine_gun_mode")) {
         normalized.putInt("gandr_machine_gun_mode", clamp(normalized.getInt("gandr_machine_gun_mode"), 0, 1));
      } else if ("healing_magic".equals(magicId) && normalized.contains("healing_target")) {
         normalized.putInt("healing_target", clamp(normalized.getInt("healing_target"), 0, 1));
      } else if (isElementalMagic(magicId) && normalized.contains("element_mode")) {
         normalized.putInt("element_mode", clamp(normalized.getInt("element_mode"), 0, 1));
      }

      return normalized;
   }

   private static boolean supportsRuntimePreset(String magicId) {
      return "reinforcement".equals(magicId)
         || "gravity_magic".equals(magicId)
         || "gandr_machine_gun".equals(magicId)
         || "projection".equals(magicId)
         || "healing_magic".equals(magicId)
         || isElementalMagic(magicId);
   }

   private static CompoundTag buildPresetFromCurrentVars(Entity entity, TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      CompoundTag payload = new CompoundTag();
      if ("reinforcement".equals(magicId)) {
         payload.putInt("reinforcement_target", clamp(vars.reinforcement_target, 0, 3));
         payload.putInt("reinforcement_mode", clamp(vars.reinforcement_mode, 0, 3));
         payload.putInt("reinforcement_level", clamp(vars.reinforcement_level, 1, 5));
      } else if ("gravity_magic".equals(magicId)) {
         payload.putInt("gravity_target", clamp(vars.gravity_magic_target, 0, 1));
         payload.putInt("gravity_mode", clamp(vars.gravity_magic_mode, -2, 2));
      } else if ("gandr_machine_gun".equals(magicId)) {
         payload.putInt("gandr_machine_gun_mode", clamp(vars.gandr_machine_gun_mode, 0, 1));
      } else if ("healing_magic".equals(magicId)) {
         payload.putInt("healing_target", clamp(vars.healing_magic_target, 0, 1));
      } else if (isElementalMagic(magicId)) {
         payload.putInt("element_mode", clamp(getElementMode(vars, magicId), 0, 1));
      } else if ("projection".equals(magicId)) {
         if (vars.projection_selected_structure_id != null && !vars.projection_selected_structure_id.isEmpty()) {
            payload.putString("projection_structure_id", vars.projection_selected_structure_id);
         } else if (entity != null && vars.projection_selected_item != null && !vars.projection_selected_item.isEmpty()) {
            payload.put("projection_item", vars.projection_selected_item.save(entity.registryAccess()));
         } else {
            payload.putBoolean("projection_lock_empty", true);
         }
      }

      return normalizePresetPayload(magicId, payload);
   }

   private static void applyPresetToVars(Entity entity, TypeMoonWorldModVariables.PlayerVariables vars, String magicId, CompoundTag payload) {
      if ("reinforcement".equals(magicId)) {
         if (payload.contains("reinforcement_target")) {
            vars.reinforcement_target = clamp(payload.getInt("reinforcement_target"), 0, 3);
         }

         if (payload.contains("reinforcement_mode")) {
            vars.reinforcement_mode = clamp(payload.getInt("reinforcement_mode"), 0, 3);
         }

         if (payload.contains("reinforcement_level")) {
            vars.reinforcement_level = clamp(payload.getInt("reinforcement_level"), 1, 5);
         }
      } else if ("gravity_magic".equals(magicId)) {
         if (payload.contains("gravity_target")) {
            vars.gravity_magic_target = clamp(payload.getInt("gravity_target"), 0, 1);
         }

         if (payload.contains("gravity_mode")) {
            vars.gravity_magic_mode = clamp(payload.getInt("gravity_mode"), -2, 2);
         }
      } else if ("gandr_machine_gun".equals(magicId)) {
         if (payload.contains("gandr_machine_gun_mode")) {
            vars.gandr_machine_gun_mode = clamp(payload.getInt("gandr_machine_gun_mode"), 0, 1);
         }
      } else if ("healing_magic".equals(magicId)) {
         if (payload.contains("healing_target")) {
            vars.healing_magic_target = clamp(payload.getInt("healing_target"), 0, 1);
         }
      } else if (isElementalMagic(magicId)) {
         if (payload.contains("element_mode")) {
            setElementMode(vars, magicId, clamp(payload.getInt("element_mode"), 0, 1));
         }
      } else if ("projection".equals(magicId) && entity != null) {
         CompoundTag projectionPayload = TypeMoonWorldModVariables.PlayerVariables.normalizeProjectionPresetPayload(payload);
         if (projectionPayload.getBoolean("projection_lock_empty")) {
            vars.projection_selected_structure_id = "";
            vars.projection_selected_item = ItemStack.EMPTY;
         } else {
            if (projectionPayload.contains("projection_structure_id")) {
               vars.projection_selected_structure_id = projectionPayload.getString("projection_structure_id");
               vars.projection_selected_item = ItemStack.EMPTY;
            }

            if (projectionPayload.contains("projection_item", 10)) {
               ItemStack.parse(entity.registryAccess(), projectionPayload.getCompound("projection_item")).ifPresent(stack -> {
                  vars.projection_selected_item = stack;
                  vars.projection_selected_structure_id = "";
               });
            }
         }
      }
   }

   private static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }

   public static boolean isElementalMagic(String magicId) {
      return "fire_magic".equals(magicId)
         || "water_magic".equals(magicId)
         || "wind_magic".equals(magicId)
         || "earth_magic".equals(magicId);
   }

   public static int getElementMode(TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      if (vars == null) {
         return 0;
      }
      return switch (magicId) {
         case "fire_magic" -> vars.fire_magic_mode;
         case "water_magic" -> vars.water_magic_mode;
         case "wind_magic" -> vars.wind_magic_mode;
         case "earth_magic" -> vars.earth_magic_mode;
         default -> 0;
      };
   }

   public static void setElementMode(TypeMoonWorldModVariables.PlayerVariables vars, String magicId, int mode) {
      if (vars == null) {
         return;
      }
      int clamped = clamp(mode, 0, 1);
      switch (magicId) {
         case "fire_magic" -> vars.fire_magic_mode = clamped;
         case "water_magic" -> vars.water_magic_mode = clamped;
         case "wind_magic" -> vars.wind_magic_mode = clamped;
         case "earth_magic" -> vars.earth_magic_mode = clamped;
      }
   }
}
