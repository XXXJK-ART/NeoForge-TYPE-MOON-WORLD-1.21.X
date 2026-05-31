package net.xxxjk.TYPE_MOON_WORLD.magic.player;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.magic.registry.MagicModularRegistry;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class PlayerMagicCastService {
   private static final double DEFAULT_COOLDOWN = 10.0;
   private static final double JEWEL_BASE_COOLDOWN = 20.0;

   private PlayerMagicCastService() {
   }

   public static void execute(Entity entity) {
      if (entity == null || entity.level().isClientSide()) {
         return;
      }

      MagicModularRegistry.ensureInitialized();
      TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)entity.getData(
         TypeMoonWorldModVariables.PLAYER_VARIABLES
      );
      vars.ensureMagicSystemInitialized();
      vars.rebuildSelectedMagicsFromActiveWheel();
      if (!vars.is_magic_circuit_open) {
         displayClientMessage(entity, "message.typemoonworld.magic.circuit_not_open");
         return;
      }

      if (vars.selected_magics.isEmpty()) {
         displayClientMessage(entity, "message.typemoonworld.magic.no_magic_selected");
         return;
      }

      if (vars.magic_cooldown > 0.0) {
         return;
      }

      boolean fullSyncNeeded = PlayerMagicSelectionService.prepareCurrentSelection(entity, vars);
      TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = PlayerMagicSelectionService.getCurrentEntry(vars);
      if (entry == null || entry.isEmpty()) {
         displayClientMessage(entity, "message.typemoonworld.magic.no_magic_selected");
         if (fullSyncNeeded) {
            vars.syncPlayerVariables(entity);
         }

         return;
      }

      if (!vars.isWheelSlotEntryCastable(entry)) {
         displayClientMessage(entity, "message.typemoonworld.magic.not_learned");
         if (fullSyncNeeded) {
            vars.syncPlayerVariables(entity);
         }

         return;
      }

      MagicExecutionResult result = MagicModularRegistry.execute(
         new MagicExecutionContext(entity, vars, entry.magicId, "crest".equals(entry.sourceType))
      );
      if (!result.handled() || !result.success()) {
         if (fullSyncNeeded) {
            vars.syncPlayerVariables(entity);
         }

         return;
      }

      applyPostCastState(vars, entry.magicId);
      fullSyncNeeded |= vars.recordCrestCastPractice(entity, entry.magicId);
      if (fullSyncNeeded) {
         vars.syncPlayerVariables(entity);
      } else {
         vars.syncMana(entity);
         vars.syncProficiency(entity);
      }
   }

   private static void applyPostCastState(TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      double cooldown = DEFAULT_COOLDOWN;
      if ("jewel_random_shoot".equals(magicId)) {
         cooldown = Math.max(1.0, JEWEL_BASE_COOLDOWN - vars.proficiency_jewel_magic_shoot * 0.2);
         vars.proficiency_jewel_magic_shoot = Math.min(100.0, vars.proficiency_jewel_magic_shoot + 0.1);
      } else if ("jewel_machine_gun".equals(magicId)) {
         vars.proficiency_jewel_magic_release = Math.min(100.0, vars.proficiency_jewel_magic_release + 0.5);
      } else if (isLegacyJewelMagic(magicId)) {
         cooldown = Math.max(1.0, JEWEL_BASE_COOLDOWN - vars.proficiency_jewel_magic_shoot * 0.2);
      }

      vars.magic_cooldown = Math.max(vars.magic_cooldown, cooldown);
   }

   private static boolean isLegacyJewelMagic(String magicId) {
      return magicId != null
         && (magicId.startsWith("ruby")
            || magicId.startsWith("sapphire")
            || magicId.startsWith("emerald")
            || magicId.startsWith("topaz")
            || magicId.startsWith("cyan"));
   }

   private static void displayClientMessage(Entity entity, String translationKey) {
      if (entity instanceof Player player && !player.level().isClientSide()) {
         player.displayClientMessage(Component.translatable(translationKey), true);
      }
   }
}
