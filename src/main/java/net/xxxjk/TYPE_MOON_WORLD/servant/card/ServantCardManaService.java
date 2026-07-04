package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;

public final class ServantCardManaService {
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
      return Math.max(2.0, params.manaPool() / 100.0);
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!vars.servant_card_transformed) {
         return;
      }
      if (vars.servant_card_max_mana <= 0.0) {
         vars.servant_card_max_mana = maxManaFor(vars.servant_card_id);
      }
      if (vars.servant_card_mana_regen <= 0.0) {
         vars.servant_card_mana_regen = regenPerSecondFor(vars.servant_card_id);
      }
      vars.servant_card_mana = Math.min(vars.servant_card_max_mana, vars.servant_card_mana + vars.servant_card_mana_regen / 20.0);
      if (player.tickCount % 20 == 0) {
         vars.syncPlayerVariables(player);
      }
   }

   public static boolean consume(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, double amount) {
      if (amount <= 0.0) {
         return true;
      }
      double own = Math.min(vars.servant_card_mana, amount);
      double remaining = amount - own;
      ServerPlayer master = getMaster(player, vars);
      TypeMoonWorldModVariables.PlayerVariables masterVars = master == null
         ? null
         : master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (remaining > 0.0 && (masterVars == null || masterVars.player_mana < remaining)) {
         return false;
      }
      vars.servant_card_mana -= own;
      if (remaining > 0.0 && masterVars != null) {
         masterVars.player_mana -= remaining;
         masterVars.syncPlayerVariables(master);
      }
      vars.syncPlayerVariables(player);
      return true;
   }

   public static ServerPlayer getMaster(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (player == null || player.getServer() == null || vars.servant_card_master_uuid == null || vars.servant_card_master_uuid.isBlank()) {
         return null;
      }
      try {
         UUID uuid = UUID.fromString(vars.servant_card_master_uuid);
         return player.getServer().getPlayerList().getPlayer(uuid);
      } catch (IllegalArgumentException ignored) {
         return null;
      }
   }
}
