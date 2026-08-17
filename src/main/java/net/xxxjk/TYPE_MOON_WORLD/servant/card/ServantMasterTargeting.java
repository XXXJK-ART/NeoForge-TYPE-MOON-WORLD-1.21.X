package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

/** Shared relation checks for automatic servant target selection. */
public final class ServantMasterTargeting {
   private ServantMasterTargeting() {
   }

   public static boolean isContractMaster(LivingEntity attacker, LivingEntity candidate) {
      if (attacker == null || candidate == null || attacker == candidate) return false;
      if (attacker instanceof ServantEntity servant) {
         return candidate instanceof ServerPlayer master && servant.isBoundTo(master);
      }
      if (!(attacker instanceof ServerPlayer servant)) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed
         && vars.servant_card_master_uuid != null
         && vars.servant_card_master_uuid.equals(candidate.getUUID().toString());
   }

   /** Returns whether the entity is an active player-controlled servant card. */
   public static boolean isServantCardPlayer(LivingEntity candidate) {
      if (!(candidate instanceof ServerPlayer player)) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && vars.servant_card_id != null && !vars.servant_card_id.isBlank();
   }

   /**
    * Allows two active servant-card players to fight even when the server-wide
    * PvP switch is disabled, while retaining scoreboard-team friendly-fire rules.
    */
   public static boolean canServantCardPlayersHarm(Player attacker, Player target) {
      if (!(attacker instanceof ServerPlayer) || !(target instanceof ServerPlayer)
         || !isServantCardPlayer(attacker) || !isServantCardPlayer(target)) {
         return false;
      }
      if (attacker.getTeam() != null && attacker.getTeam().isAlliedTo(target.getTeam())) {
         return attacker.getTeam().isAllowFriendlyFire();
      }
      return true;
   }

   public static boolean isAutomaticTarget(LivingEntity attacker, LivingEntity candidate) {
      return candidate != null && candidate.isAlive() && !isContractMaster(attacker, candidate);
   }
}
