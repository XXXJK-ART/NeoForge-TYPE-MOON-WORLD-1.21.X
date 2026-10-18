package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.ArtificialLeylineBlockEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.typemoonworld.api.AddonManaSource;

public final class ServantCardManaService {
   private static final double SERVANT_CARD_REGEN_MULTIPLIER = 1.0;
   public static final String NOBLE_PHANTASM_POWER_SCALE_TAG = "ServantCardNpPowerScale";
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
         return 4.0 * SERVANT_CARD_REGEN_MULTIPLIER;
      }
      ServantParams params = definition.parameters();
      double regen = params.manaPool() / fullRegenSeconds(params) * SERVANT_CARD_REGEN_MULTIPLIER;
      return "artoria_pendragon".equals(servantId) ? regen * 2.0 : regen;
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      var card = vars.servantCardState();
      if (!card.transformed()) {
         return;
      }
      if (card.maxMana() <= 0.0) {
         card.setMaxMana(maxManaFor(card.id()));
      }
      double expectedRegen = 0.0;
      ServerPlayer master = getMaster(player, vars);
      boolean linked = MasterServantLinkService.canUseMasterMana(player, vars, master);
      boolean medeaException = "medea".equals(card.id());
      double linkedRegen = 0.0;
      if (linked && master != null) {
         if (medeaException) {
            linkedRegen = regenPerSecondFor(card.id())
               * Math.max(0.0, 1.0 - MasterServantLinkService.distanceDecay(player, vars, master));
         } else {
            TypeMoonWorldModVariables.PlayerVariables masterVars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            linkedRegen = masterVars.magicState().manaRegen()
               * MasterServantLinkService.linkedRegenMultiplier(player, vars)
               * SERVANT_CARD_REGEN_MULTIPLIER;
         }
      }
      expectedRegen = passiveRegenForContractState(vars.servant_card_contract_state,
         regenPerSecondFor(card.id()), linkedRegen);
      if (hasValidServantCardLeyline(player, vars)) {
         expectedRegen = regenPerSecondFor(card.id());
      }
      if (player.hasEffect(ModMobEffects.FANATIC_CIRCUIT_DISRUPTION)) {
         expectedRegen *= 0.5;
      }
      if (Math.abs(card.manaRegen() - expectedRegen) > 1.0E-6) {
         card.setManaRegen(expectedRegen);
      }
      if (expectedRegen > 0.0 && card.mana() < card.maxMana()) {
         card.setMana(Math.min(card.maxMana(), card.mana() + card.manaRegen() / 20.0));
      }
      if (player.tickCount % 20 == 0) {
         vars.syncMana(player);
      }
   }

   private static double fullRegenSeconds(ServantParams params) {
      return switch (params.magic()) {
         case A_PLUS_PLUS -> 60.0;
         case A -> 60.0;
         case B -> 120.0;
         case C -> 180.0;
         case A_MINUS -> 180.0;
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
      if (player == null || vars == null || amount <= 0.0) {
         return amount <= 0.0;
      }
      double multiplier = MasterServantLinkService.noblePhantasmCostMultiplier(player, vars);
      if (!Double.isFinite(multiplier)) {
         return false;
      }
      double effectiveCost = amount * multiplier;
      double available = availableForConsume(player, vars);
      if (available + 1.0E-6 < effectiveCost) {
         clearNoblePhantasmPowerScale(player);
         return false;
      }
      ManaSnapshot paymentBefore = snapshot(player, vars);
      consumeUpTo(player, vars, effectiveCost);
      net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.notePlayerPayment(player, player, true, paymentBefore.servantMana() - vars.servantCardState().mana());
      if (paymentBefore.master() != null) net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.notePlayerPayment(player, paymentBefore.master(), false,
         paymentBefore.masterMana() - paymentBefore.master().getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).magicState().mana());
      player.getPersistentData().putDouble(NOBLE_PHANTASM_POWER_SCALE_TAG, 1.0);
      return true;
   }

   public static double noblePhantasmPowerScale(ServerPlayer player) {
      if (player == null) return 1.0;
      double scale = player.getPersistentData().getDouble(NOBLE_PHANTASM_POWER_SCALE_TAG);
      return Double.isFinite(scale) && scale > 0.0 ? Math.min(1.0, scale) : 1.0;
   }

   public static void clearNoblePhantasmPowerScale(ServerPlayer player) {
      if (player != null) {
         player.getPersistentData().remove(NOBLE_PHANTASM_POWER_SCALE_TAG);
      }
   }

   public static double availableForConsume(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (player == null || vars == null) {
         return 0.0;
      }
      if (AddonManaSource.hasInfiniteSupply(player)) {
         return Double.MAX_VALUE;
      }
      if (ServantCardUnlimitedMode.isEnabled(player)) {
         return Double.MAX_VALUE;
      }
      double available = Math.max(0.0, vars.servantCardState().mana());
      ServerPlayer master = getMaster(player, vars);
      if (master != null && MasterStateManager.canDrawMasterMana(player, vars, master)) {
         TypeMoonWorldModVariables.PlayerVariables masterVars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         available += Math.max(0.0, masterVars.magicState().mana());
      }
      return available;
   }

   public static double consumeUpTo(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, double amount) {
      if (player == null || vars == null || amount <= 0.0) {
         return 0.0;
      }
      if (AddonManaSource.hasInfiniteSupply(player)) {
         return amount;
      }
      if (ServantCardUnlimitedMode.isEnabled(player)) {
         return amount;
      }
      var card = vars.servantCardState();
      double own = Math.min(Math.max(0.0, card.mana()), amount);
      double remaining = amount - own;
      ServerPlayer master = getMaster(player, vars);
      TypeMoonWorldModVariables.PlayerVariables masterVars = master == null
         ? null
         : master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double masterSpent = 0.0;
      if (remaining > 0.0 && masterVars != null && MasterStateManager.canDrawMasterMana(player, vars, master)) {
         masterSpent = Math.min(Math.max(0.0, masterVars.magicState().mana()), remaining);
      }
      if (own > 0.0) {
         card.setMana(Math.max(0.0, card.mana() - own));
         vars.syncMana(player);
      }
      if (masterSpent > 0.0 && masterVars != null) {
         masterVars.magicState().setMana(Math.max(0.0, masterVars.magicState().mana() - masterSpent));
         MasterServantLinkService.markDrawingMasterMana(master, player);
         masterVars.syncMana(master);
      }
      return own + masterSpent;
   }

   public static ManaSnapshot snapshot(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      ServerPlayer master = getMaster(player, vars);
      double masterMana = master == null ? 0.0 : master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).magicState().mana();
      return new ManaSnapshot(vars.servantCardState().mana(), master, masterMana);
   }

   public static void restore(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, ManaSnapshot snapshot) {
      clearNoblePhantasmPowerScale(player);
      vars.servantCardState().setMana(snapshot.servantMana());
      vars.syncMana(player);
      ServerPlayer master = snapshot.master();
      if (master != null) {
         TypeMoonWorldModVariables.PlayerVariables masterVars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         masterVars.magicState().setMana(snapshot.masterMana());
         masterVars.syncMana(master);
      }
   }

   private static boolean consume(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, double amount, boolean noblePhantasm, boolean sync) {
      if (amount <= 0.0) {
         return true;
      }
      if (AddonManaSource.hasInfiniteSupply(player)) {
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
      var card = vars.servantCardState();
      double own = Math.min(card.mana(), amount);
      double remaining = amount - own;
      ServerPlayer master = getMaster(player, vars);
      TypeMoonWorldModVariables.PlayerVariables masterVars = master == null
         ? null
         : master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (remaining > 0.0 && (masterVars == null || !MasterStateManager.canDrawMasterMana(player, vars, master) || masterVars.magicState().mana() < remaining)) {
         return false;
      }
      card.setMana(card.mana() - own);
      if (remaining > 0.0 && masterVars != null) {
         masterVars.magicState().setMana(masterVars.magicState().mana() - remaining);
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
      if (AddonManaSource.hasInfiniteSupply(player)) return true;
      var card = vars.servantCardState();
      if (card.mana() + 1.0E-6 < amount) return false;
      card.setMana(Math.max(0.0, card.mana() - amount));
      vars.syncMana(player);
      return true;
   }

   /** Restores card MP from explicit mana media in the player's inventory. */
   public static boolean restoreFromInventory(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (player == null || vars == null || !vars.servantCardState().transformed()
         || vars.servantCardState().mana() >= vars.servantCardState().maxMana()) {
         return false;
      }
      var card = vars.servantCardState();
      double need = card.maxMana() - card.mana();
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
      card.setMana(Math.min(card.maxMana(), card.mana() + source.amount()));
      vars.syncMana(player);
      return true;
   }

   public static boolean restoreFromActiveItem(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars,
                                                InteractionHand hand) {
      if (player == null || vars == null || hand == null || !vars.servantCardState().transformed()
         || vars.servantCardState().mana() >= vars.servantCardState().maxMana()) return false;
      var card = vars.servantCardState();
      ItemStack stack = player.getItemInHand(hand);
      ManaSource source = manaSource(-1, stack);
      if (source == null) return false;
      double amount = Math.min(source.amount(), card.maxMana() - card.mana());
      consumeHeldSource(player, hand, source.remainder());
      card.setMana(Math.min(card.maxMana(), card.mana() + amount));
      vars.syncMana(player);
      return true;
   }

   public static boolean hasValidServantCardLeyline(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (player == null || vars == null || !vars.servantCardState().transformed()
         || !vars.servant_card_artificial_leyline_bonus_active
         || !player.level().dimension().location().toString().equals(vars.servant_card_artificial_leyline_dimension)) return false;
      BlockPos pos = new BlockPos(vars.servant_card_artificial_leyline_x,
         vars.servant_card_artificial_leyline_y, vars.servant_card_artificial_leyline_z);
      return player.level().getBlockEntity(pos) instanceof ArtificialLeylineBlockEntity leyline
         && leyline.isOwnedBy(player.getUUID());
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

   private static void consumeHeldSource(ServerPlayer player, InteractionHand hand, net.minecraft.world.item.Item remainder) {
      ItemStack stack = player.getItemInHand(hand);
      if (stack.getCount() > 1) {
         stack.shrink(1);
         if (remainder != null) {
            ItemStack empty = new ItemStack(remainder);
            if (!player.getInventory().add(empty)) player.drop(empty, false);
         }
      } else {
         player.setItemInHand(hand, remainder == null ? ItemStack.EMPTY : new ItemStack(remainder));
      }
      player.getInventory().setChanged();
   }

   private record ManaSource(int slot, double amount, net.minecraft.world.item.Item remainder) {
   }

   public static ServerPlayer getMaster(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      return MasterStateManager.getMaster(player, vars);
   }
}
