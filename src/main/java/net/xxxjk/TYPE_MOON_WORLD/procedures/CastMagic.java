package net.xxxjk.TYPE_MOON_WORLD.procedures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.magic.registry.MagicModularRegistry;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public class CastMagic {
   public static void execute(Entity entity) {
      if (entity != null) {
         if (!entity.level().isClientSide()) {
            if (entity instanceof Player player && player.isSpectator()) {
               player.displayClientMessage(Component.translatable("message.typemoonworld.magic.spectator_disabled"), true);
            } else {
               TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)entity.getData(
                  TypeMoonWorldModVariables.PLAYER_VARIABLES
               );
               MagicModularRegistry.ensureInitialized();
               vars.ensureMagicSystemInitialized();
               vars.rebuildSelectedMagicsFromActiveWheel();
               if (!vars.is_magic_circuit_open) {
                  if (entity instanceof Player player && !player.level().isClientSide()) {
                     player.displayClientMessage(Component.translatable("message.typemoonworld.magic.circuit_not_open"), true);
                  }
               } else if (vars.selected_magics.isEmpty()) {
                  if (entity instanceof Player player && !player.level().isClientSide()) {
                     player.displayClientMessage(Component.translatable("message.typemoonworld.magic.no_magic_selected"), true);
                  }
               } else {
                  boolean machineGunSelected = false;
                  if (!vars.selected_magics.isEmpty()) {
                     int selected = vars.current_magic_index;
                     machineGunSelected = selected >= 0
                        && selected < vars.selected_magics.size()
                        && ("jewel_machine_gun".equals(vars.selected_magics.get(selected)) || "gandr_machine_gun".equals(vars.selected_magics.get(selected)));
                  }

                  if (!(vars.magic_cooldown > 0.0) || machineGunSelected) {
                     int index = vars.current_magic_index;
                     if (index >= 0 && index < vars.selected_magics.size()) {
                        String magicId = vars.selected_magics.get(index);
                        boolean crestCast = isCastingFromCrestSlot(vars, magicId);
                        if (!vars.learned_magics.contains(magicId)) {
                           boolean crestBypass = vars.canCastCurrentSelectionViaCrest(magicId);
                           if (!crestBypass) {
                              if (entity instanceof Player player && !player.level().isClientSide()) {
                                 player.displayClientMessage(Component.translatable("message.typemoonworld.magic.not_learned"), true);
                              }

                              return;
                           }
                        }

                        CastMagic.CrestCastSnapshot crestSnapshot = null;
                        if (crestCast) {
                           crestSnapshot = CastMagic.CrestCastSnapshot.capture(vars);
                           applyMaxProficiencyForCrestCast(vars);
                           vars.crest_cast_context = true;
                        }

                        boolean castSuccess = false;

                        try {
                           vars.applyCurrentCrestPreset(entity);
                           MagicExecutionResult executionResult = MagicModularRegistry.execute(new MagicExecutionContext(entity, vars, magicId, crestCast));
                           if (!executionResult.handled()) {
                              TYPE_MOON_WORLD.LOGGER.warn("No magic executor registered for magicId={}", magicId);
                           }

                           castSuccess = executionResult.success();
                        } finally {
                           if (crestCast) {
                              if (crestSnapshot != null) {
                                 boolean restoreProjectionState = !"projection".equals(magicId);
                                 boolean restoredLearningState = crestSnapshot.restore(vars, restoreProjectionState);
                                 if (restoredLearningState) {
                                    vars.syncPlayerVariables(entity);
                                 }
                              }

                              vars.crest_cast_context = false;
                           }
                        }

                        if (castSuccess) {
                           double cooldown = 10.0;
                           if (vars.selected_magics.get(index).startsWith("jewel_magic_shoot") || "jewel_random_shoot".equals(vars.selected_magics.get(index))) {
                              double baseCooldown = 20.0;
                              double effectiveProficiency = crestCast ? 100.0 : vars.proficiency_jewel_magic_shoot;
                              cooldown = Math.max(1.0, baseCooldown - effectiveProficiency * 0.2);
                              if (!crestCast) {
                                 vars.proficiency_jewel_magic_shoot = Math.min(100.0, vars.proficiency_jewel_magic_shoot + 0.1);
                                 vars.syncProficiency(entity);
                              }
                           } else if (!"jewel_machine_gun".equals(vars.selected_magics.get(index))
                              && !"gandr_machine_gun".equals(vars.selected_magics.get(index))) {
                              if (vars.selected_magics.get(index).startsWith("jewel_magic_release")) {
                                 double baseCooldown = 20.0;
                                 double effectiveProficiency = crestCast ? 100.0 : vars.proficiency_jewel_magic_release;
                                 cooldown = Math.max(1.0, baseCooldown - effectiveProficiency * 0.2);
                                 if (!crestCast) {
                                    vars.proficiency_jewel_magic_release = Math.min(100.0, vars.proficiency_jewel_magic_release + 0.1);
                                    vars.syncProficiency(entity);
                                 }
                              } else {
                                 cooldown = 10.0;
                              }
                           } else {
                              cooldown = 0.0;
                           }

                           vars.magic_cooldown = cooldown;
                           if (crestCast) {
                              vars.syncMana(entity);
                           } else {
                              vars.syncMana(entity);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean isCastingFromCrestSlot(TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry current = vars.getCurrentRuntimeWheelEntry();
      return current != null && "crest".equals(current.sourceType) && vars.isWheelSlotEntryCastable(current) && magicId.equals(current.magicId);
   }

   private static void applyMaxProficiencyForCrestCast(TypeMoonWorldModVariables.PlayerVariables vars) {
      vars.proficiency_structural_analysis = 100.0;
      vars.proficiency_projection = 100.0;
      vars.proficiency_reinforcement = 100.0;
      vars.proficiency_jewel_magic_shoot = 100.0;
      vars.proficiency_jewel_magic_release = 100.0;
      vars.proficiency_unlimited_blade_works = 100.0;
      vars.proficiency_sword_barrel_full_open = 100.0;
      vars.proficiency_gravity_magic = 100.0;
      vars.proficiency_gander = 100.0;
   }

   private static class CrestCastSnapshot {
      final double structuralAnalysis;
      final double projection;
      final double reinforcement;
      final double jewelShoot;
      final double jewelRelease;
      final double ubw;
      final double swordBarrel;
      final double gravity;
      final double gander;
      final int reinforcementTarget;
      final int reinforcementMode;
      final int reinforcementLevel;
      final int gravityTarget;
      final int gravityMode;
      final int gandrMachineGunMode;
      final String projectionSelectedStructureId;
      final ItemStack projectionSelectedItem;
      final List<String> learnedMagics;
      final Map<String, Integer> crestPractice;

      private CrestCastSnapshot(TypeMoonWorldModVariables.PlayerVariables vars) {
         this.structuralAnalysis = vars.proficiency_structural_analysis;
         this.projection = vars.proficiency_projection;
         this.reinforcement = vars.proficiency_reinforcement;
         this.jewelShoot = vars.proficiency_jewel_magic_shoot;
         this.jewelRelease = vars.proficiency_jewel_magic_release;
         this.ubw = vars.proficiency_unlimited_blade_works;
         this.swordBarrel = vars.proficiency_sword_barrel_full_open;
         this.gravity = vars.proficiency_gravity_magic;
         this.gander = vars.proficiency_gander;
         this.reinforcementTarget = vars.reinforcement_target;
         this.reinforcementMode = vars.reinforcement_mode;
         this.reinforcementLevel = vars.reinforcement_level;
         this.gravityTarget = vars.gravity_magic_target;
         this.gravityMode = vars.gravity_magic_mode;
         this.gandrMachineGunMode = vars.gandr_machine_gun_mode;
         this.projectionSelectedStructureId = vars.projection_selected_structure_id == null ? "" : vars.projection_selected_structure_id;
         this.projectionSelectedItem = vars.projection_selected_item == null ? ItemStack.EMPTY : vars.projection_selected_item.copy();
         this.learnedMagics = new ArrayList<>(vars.learned_magics);
         this.crestPractice = new HashMap<>(vars.crest_practice_count);
      }

      static CastMagic.CrestCastSnapshot capture(TypeMoonWorldModVariables.PlayerVariables vars) {
         return new CastMagic.CrestCastSnapshot(vars);
      }

      boolean restore(TypeMoonWorldModVariables.PlayerVariables vars, boolean restoreProjectionState) {
         vars.proficiency_structural_analysis = this.structuralAnalysis;
         vars.proficiency_projection = this.projection;
         vars.proficiency_reinforcement = this.reinforcement;
         vars.proficiency_jewel_magic_shoot = this.jewelShoot;
         vars.proficiency_jewel_magic_release = this.jewelRelease;
         vars.proficiency_unlimited_blade_works = this.ubw;
         vars.proficiency_sword_barrel_full_open = this.swordBarrel;
         vars.proficiency_gravity_magic = this.gravity;
         vars.proficiency_gander = this.gander;
         boolean changed = false;
         if (vars.reinforcement_target != this.reinforcementTarget) {
            vars.reinforcement_target = this.reinforcementTarget;
            changed = true;
         }

         if (vars.reinforcement_mode != this.reinforcementMode) {
            vars.reinforcement_mode = this.reinforcementMode;
            changed = true;
         }

         if (vars.reinforcement_level != this.reinforcementLevel) {
            vars.reinforcement_level = this.reinforcementLevel;
            changed = true;
         }

         if (vars.gravity_magic_target != this.gravityTarget) {
            vars.gravity_magic_target = this.gravityTarget;
            changed = true;
         }

         if (vars.gravity_magic_mode != this.gravityMode) {
            vars.gravity_magic_mode = this.gravityMode;
            changed = true;
         }

         if (vars.gandr_machine_gun_mode != this.gandrMachineGunMode) {
            vars.gandr_machine_gun_mode = this.gandrMachineGunMode;
            changed = true;
         }

         if (restoreProjectionState) {
            String currentProjectionStructure = vars.projection_selected_structure_id == null ? "" : vars.projection_selected_structure_id;
            if (!currentProjectionStructure.equals(this.projectionSelectedStructureId)) {
               vars.projection_selected_structure_id = this.projectionSelectedStructureId;
               changed = true;
            }

            ItemStack currentProjectionItem = vars.projection_selected_item == null ? ItemStack.EMPTY : vars.projection_selected_item;
            if (!ItemStack.isSameItemSameComponents(currentProjectionItem, this.projectionSelectedItem)) {
               vars.projection_selected_item = this.projectionSelectedItem.copy();
               changed = true;
            }
         }

         if (!vars.learned_magics.equals(this.learnedMagics)) {
            vars.learned_magics = new ArrayList<>(this.learnedMagics);
            changed = true;
         }

         if (!vars.crest_practice_count.equals(this.crestPractice)) {
            vars.crest_practice_count = new HashMap<>(this.crestPractice);
            changed = true;
         }

         return changed;
      }
   }
}
