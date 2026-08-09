package net.xxxjk.TYPE_MOON_WORLD.magic.player;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.combat.OriginBulletHelper;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.magic.WheelCastingModifierService;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.magic.registry.MagicModularRegistry;
import net.xxxjk.TYPE_MOON_WORLD.api.MagicDefinitionRegistry;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.xxxjk.typemoonworld.api.ExecutionResult;
import net.xxxjk.typemoonworld.api.MagicCastContext;
import net.xxxjk.typemoonworld.api.event.MagicCastEvent;
import net.xxxjk.TYPE_MOON_WORLD.talent.TalentService;
import net.xxxjk.TYPE_MOON_WORLD.passive.AdvancedPassiveService;

public final class PlayerMagicCastService {
   private static final double DEFAULT_COOLDOWN = 10.0;
   private static final double JEWEL_BASE_COOLDOWN = 20.0;

   private PlayerMagicCastService() {
   }

   public static void execute(Entity entity) {
      if (entity == null || entity.level().isClientSide()) {
         return;
      }
      if (EntityUtils.isPetrified(entity)) {
         return;
      }

      MagicModularRegistry.ensureInitialized();
      TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)entity.getData(
         TypeMoonWorldModVariables.PLAYER_VARIABLES
      );
      vars.ensureMagicSystemInitialized();
      vars.rebuildSelectedMagicsFromActiveWheel();
      if (vars.selected_magics.isEmpty()) {
         displayClientMessage(entity, "message.typemoonworld.magic.no_magic_selected");
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

      if (entity instanceof net.minecraft.server.level.ServerPlayer player && TalentService.isTalent(entry.magicId)) {
         TalentService.cast(player, vars, entry.magicId);
         return;
      }

      if (entity instanceof Player player && OriginBulletHelper.isSealed(player) && !AdvancedPassiveService.ignoresOriginBulletSeal(player)) {
         displayClientMessage(entity, "message.typemoonworld.origin_bullet.sealed");
         return;
      }
      if (!vars.is_magic_circuit_open) {
         displayClientMessage(entity, "message.typemoonworld.magic.circuit_not_open");
         return;
      }
      if (vars.magic_cooldown > 0.0) return;

      var dynamicDefinition = MagicDefinitionRegistry.get(entry.magicId);
      if (!MagicDefinitionRegistry.meetsAttributeRequirements(vars, entry.magicId)) {
         displayClientMessage(entity, "message.typemoonworld.magic.missing_attribute");
         return;
      }
      double dynamicCost = dynamicDefinition != null ? adjustedWheelCost(entity, vars, entry, dynamicDefinition.manaCost()) : 0.0;
      if (dynamicDefinition != null && vars.player_mana < dynamicCost) {
         displayClientMessage(entity, "message.typemoonworld.magic.insufficient_mana");
         return;
      }

      // Bajiquan uses its server-owned input state machine and never enters a magic executor.
      if ("bajiquan".equals(entry.magicId) || "ganryu".equals(entry.magicId)
         || "hokushin_ittoryu".equals(entry.magicId) || "tennen_rishin_ryu".equals(entry.magicId)) return;

      ResourceLocation publicMagicId = ResourceLocation.tryParse(entry.magicId);
      MagicCastContext publicContext = new MagicCastContext(
         entity instanceof LivingEntity living ? living : null,
         null,
         entity.level(),
         entry.magicId,
         entry.presetPayload == null ? new CompoundTag() : entry.presetPayload.copy(),
         "crest".equals(entry.sourceType),
         vars.magic_proficiencies.getOrDefault(entry.magicId, 0.0));
      if (publicMagicId != null && NeoForge.EVENT_BUS.post(new MagicCastEvent.Pre(publicMagicId, publicContext)).isCanceled()) {
         return;
      }
      MagicExecutionResult result;
      if (entity instanceof net.minecraft.server.level.ServerPlayer serverPlayer && !"crest".equals(entry.sourceType)) {
         try (var ignored = WheelCastingModifierService.begin(serverPlayer, entry.magicId)) {
            result = MagicModularRegistry.execute(
               new MagicExecutionContext(entity, vars, entry.magicId, false)
            );
         }
      } else {
         result = MagicModularRegistry.execute(
            new MagicExecutionContext(entity, vars, entry.magicId, "crest".equals(entry.sourceType))
         );
      }
      if (publicMagicId != null) {
         NeoForge.EVENT_BUS.post(new MagicCastEvent.Post(publicMagicId, publicContext,
            result.handled() ? new ExecutionResult(true, result.success(), result.manaCost(), result.cooldownTicks()) : ExecutionResult.NOT_HANDLED));
      }
      if (!result.handled() || !result.success()) {
         if (fullSyncNeeded) {
            vars.syncPlayerVariables(entity);
         }

         return;
      }

      if (result.manaCost() > 0.0) {
         double cost = adjustedWheelCost(entity, vars, entry, result.manaCost());
         vars.player_mana = Math.max(0.0, vars.player_mana - cost);
      } else if (dynamicDefinition != null && dynamicDefinition.manaCost() > 0.0) {
         double cost = adjustedWheelCost(entity, vars, entry, dynamicDefinition.manaCost());
         vars.player_mana = Math.max(0.0, vars.player_mana - cost);
      }

      applyPostCastState(entity, vars, entry.magicId);
      fullSyncNeeded |= vars.recordCrestCastPractice(entity, entry.magicId);
      if (fullSyncNeeded) {
         vars.syncPlayerVariables(entity);
      } else {
         vars.syncMana(entity);
         vars.syncProficiency(entity);
      }
   }

   private static void applyPostCastState(Entity entity, TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      double cooldown = DEFAULT_COOLDOWN;
      if (MagicDefinitionRegistry.contains(magicId)) {
         cooldown = MagicDefinitionRegistry.get(magicId).cooldownTicks();
      }
      if ("jewel_random_shoot".equals(magicId)) {
         cooldown = Math.max(1.0, JEWEL_BASE_COOLDOWN - vars.proficiency_jewel_magic_shoot * 0.2);
         net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService.add(vars, "jewel_magic_shoot", 0.1);
      } else if ("jewel_machine_gun".equals(magicId)) {
         net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService.add(vars, "jewel_magic_release", 0.5);
      } else if (isLegacyJewelMagic(magicId)) {
         cooldown = Math.max(1.0, JEWEL_BASE_COOLDOWN - vars.proficiency_jewel_magic_shoot * 0.2);
      }

      if (entity instanceof LivingEntity living) {
         cooldown = MercurySwordMagicAmplifier.amplifyCooldown(living, cooldown);
      }
      var definition = MagicDefinitionRegistry.get(magicId);
      if (definition != null && definition.cooldownTicks() > 0) {
         cooldown = definition.cooldownTicks();
      }
      vars.magic_cooldown = Math.max(vars.magic_cooldown, cooldown);
   }

   private static double adjustedWheelCost(Entity entity, TypeMoonWorldModVariables.PlayerVariables vars, TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry, double cost) {
      if (cost <= 0.0 || !(entity instanceof Player) || entry == null || "crest".equals(entry.sourceType)) {
         return cost;
      }
      return Math.max(0.0, cost * AdvancedPassiveService.manaMultiplier(vars));
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
