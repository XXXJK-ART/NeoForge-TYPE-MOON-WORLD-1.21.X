package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
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
      double expectedRegen = regenPerSecondFor(vars.servant_card_id);
      ServerPlayer master = getMaster(player, vars);
      boolean linked = MasterServantLinkService.canUseMasterMana(player, vars, master);
      boolean medeaException = "medea".equals(vars.servant_card_id);
      if (linked && master != null && !medeaException) {
         TypeMoonWorldModVariables.PlayerVariables masterVars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         expectedRegen = masterVars.player_mana_egenerated_every_moment * MasterServantLinkService.linkedRegenMultiplier(player, vars);
      }
      if (player.hasEffect(ModMobEffects.FANATIC_CIRCUIT_DISRUPTION)) {
         expectedRegen *= 0.5;
      }
      if (Math.abs(vars.servant_card_mana_regen - expectedRegen) > 1.0E-6) {
         vars.servant_card_mana_regen = expectedRegen;
      }
      if ((medeaException || !linked || expectedRegen > 0.0) && vars.servant_card_mana < vars.servant_card_max_mana) {
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
      if (noblePhantasm && MasterServantLinkService.STATE_INDEPENDENT.equals(vars.master_servant_link_state)) {
         return false;
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

   public static ServerPlayer getMaster(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      return MasterStateManager.getMaster(player, vars);
   }
}
