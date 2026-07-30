package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;

public final class ServantCardManaService {
   public record ManaSnapshot(double servantMana, ServerPlayer master, double masterMana) {
   }

   private ServantCardManaService() {
   }

   public static double maxManaFor(String servantId) {
      ServantDefinition definition = ServantDataRegistry.get(servantId);
      return definition != null ? definition.parameters().manaPool() : 200.0;
   }

   public static double regenPerSecondFor(String servantId) {
      ServantDefinition definition = ServantDataRegistry.get(servantId);
      if (definition == null) {
         return 4.0;
      }
      ServantParams params = definition.parameters();
      return params.manaPool() / fullRegenSeconds(params);
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!vars.servant_card_transformed) {
         return;
      }
      if (vars.servant_card_max_mana <= 0.0) {
         vars.servant_card_max_mana = maxManaFor(vars.servant_card_id);
      }
      double expectedRegen = 0.0;
      ServerPlayer master = getMaster(player, vars);
      boolean linked = MasterServantLinkService.canUseMasterMana(player, vars, master);
      boolean medeaException = "medea".equals(vars.servant_card_id);
      double linkedRegen = 0.0;
      if (linked && master != null) {
         if (medeaException) {
            linkedRegen = regenPerSecondFor(vars.servant_card_id)
               * Math.max(0.0, 1.0 - MasterServantLinkService.distanceDecay(player, vars, master));
         } else {
            TypeMoonWorldModVariables.PlayerVariables masterVars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            linkedRegen = masterVars.player_mana_egenerated_every_moment * MasterServantLinkService.linkedRegenMultiplier(player, vars);
         }
      }
      expectedRegen = passiveRegenForContractState(vars.servant_card_contract_state,
         regenPerSecondFor(vars.servant_card_id), linkedRegen);
      if (player.hasEffect(ModMobEffects.FANATIC_CIRCUIT_DISRUPTION)) {
         expectedRegen *= 0.5;
      }
      if (Math.abs(vars.servant_card_mana_regen - expectedRegen) > 1.0E-6) {
         vars.servant_card_mana_regen = expectedRegen;
      }
      if (expectedRegen > 0.0 && vars.servant_card_mana < vars.servant_card_max_mana) {
         vars.servant_card_mana = Math.min(vars.servant_card_max_mana, vars.servant_card_mana + vars.servant_card_mana_regen / 20.0);
      }
      if (player.tickCount % 20 == 0) {
         vars.syncMana(player);
      }
   }

   private static double fullRegenSeconds(ServantParams params) {
      return switch (params.magic()) {
         case A -> 60.0;
         case B -> 120.0;
         case C -> 180.0;
         case D -> 240.0;
         case E -> 300.0;
      };
   }

   static double passiveRegenForContractState(String state, double nativeRegen, double linkedRegen) {
      return switch (MasterServantLinkService.sanitizeServantContractState(state)) {
         case MasterServantLinkService.SERVANT_CONTRACT_NATIVE -> Math.max(0.0, nativeRegen);
         case MasterServantLinkService.SERVANT_CONTRACT_CONTRACTED -> Math.max(0.0, linkedRegen);
         default -> 0.0;
      };
   }

   public static boolean consume(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, double amount) {
      return consume(player, vars, amount, false, true);
   }

   public static boolean consumeSilently(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, double amount) {
      return consume(player, vars, amount, false, false);
   }

   public static boolean consumeNoblePhantasm(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, double amount) {
      return consume(player, vars, amount, true, true);
   }

   public static ManaSnapshot snapshot(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      ServerPlayer master = getMaster(player, vars);
      double masterMana = master == null ? 0.0 : master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_mana;
      return new ManaSnapshot(vars.servant_card_mana, master, masterMana);
   }

   public static void restore(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, ManaSnapshot snapshot) {
      vars.servant_card_mana = snapshot.servantMana();
      vars.syncMana(player);
      ServerPlayer master = snapshot.master();
      if (master != null) {
         TypeMoonWorldModVariables.PlayerVariables masterVars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         masterVars.player_mana = snapshot.masterMana();
         masterVars.syncMana(master);
      }
   }

   private static boolean consume(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, double amount, boolean noblePhantasm, boolean sync) {
      if (amount <= 0.0) {
         return true;
      }
      if (ServantCardUnlimitedMode.isEnabled(player)) {
         return true;
      }
      if (noblePhantasm) {
         double multiplier = MasterServantLinkService.noblePhantasmCostMultiplier(player, vars);
         if (!Double.isFinite(multiplier)) {
            return false;
         }
         amount *= multiplier;
      }
      double own = Math.min(vars.servant_card_mana, amount);
      double remaining = amount - own;
      ServerPlayer master = getMaster(player, vars);
      TypeMoonWorldModVariables.PlayerVariables masterVars = master == null
         ? null
         : master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (remaining > 0.0 && (masterVars == null || !MasterStateManager.canDrawMasterMana(player, vars, master) || masterVars.player_mana < remaining)) {
         return false;
      }
      vars.servant_card_mana -= own;
      if (remaining > 0.0 && masterVars != null) {
         masterVars.player_mana -= remaining;
         MasterServantLinkService.markDrawingMasterMana(master, player);
         if (sync) {
            masterVars.syncMana(master);
         }
      }
      if (sync) {
         vars.syncMana(player);
      }
      return true;
   }

   public static boolean consumeOwnMana(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, double amount) {
      if (amount <= 0.0) return true;
      if (vars.servant_card_mana + 1.0E-6 < amount) return false;
      vars.servant_card_mana = Math.max(0.0, vars.servant_card_mana - amount);
      vars.syncMana(player);
      return true;
   }

   /** Restores card MP from explicit mana media in the player's inventory. */
   public static boolean restoreFromInventory(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (player == null || vars == null || !vars.servant_card_transformed
         || vars.servant_card_mana >= vars.servant_card_max_mana) {
         return false;
      }
      double need = vars.servant_card_max_mana - vars.servant_card_mana;
      ManaSource source = null;
      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         ManaSource candidate = manaSource(slot, stack);
         if (candidate == null) continue;
         if (source == null || (candidate.amount() >= need && source.amount() < need)
            || (candidate.amount() >= need && source.amount() >= need && candidate.amount() < source.amount())
            || (candidate.amount() < need && source.amount() < need && candidate.amount() > source.amount())) {
            source = candidate;
         }
      }
      if (source == null) return false;
      consumeInventorySource(player, source);
      vars.servant_card_mana = Math.min(vars.servant_card_max_mana, vars.servant_card_mana + source.amount());
      vars.syncMana(player);
      return true;
   }

   private static ManaSource manaSource(int slot, ItemStack stack) {
      if (stack == null || stack.isEmpty()) return null;
      if (stack.getItem() instanceof FullManaCarvedGemItem gem) {
         return new ManaSource(slot, gem.getManaAmount(stack), gem.getEmptyGemItem());
      }
      if (stack.is(ModItems.MAGIC_FRAGMENTS.get())) {
         return new ManaSource(slot, 10.0, null);
      }
      return stack.is(((Block)ModBlocks.SPIRIT_VEIN_BLOCK.get()).asItem())
         ? new ManaSource(slot, 90.0, null) : null;
   }

   private static void consumeInventorySource(ServerPlayer player, ManaSource source) {
      ItemStack stack = player.getInventory().getItem(source.slot());
      if (stack.getCount() > 1) {
         stack.shrink(1);
         if (source.remainder() != null) {
            ItemStack remainder = new ItemStack(source.remainder());
            if (!player.getInventory().add(remainder)) player.drop(remainder, false);
         }
      } else {
         player.getInventory().setItem(source.slot(), source.remainder() == null ? ItemStack.EMPTY : new ItemStack(source.remainder()));
      }
      player.getInventory().setChanged();
   }

   private record ManaSource(int slot, double amount, net.minecraft.world.item.Item remainder) {
   }

   public static ServerPlayer getMaster(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      return MasterStateManager.getMaster(player, vars);
   }
}
