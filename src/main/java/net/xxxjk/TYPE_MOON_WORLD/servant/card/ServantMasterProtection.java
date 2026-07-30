package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GawainEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

/** Prevents incidental heavy-servant damage to that servant's contracted master. */
public final class ServantMasterProtection {
   private ServantMasterProtection() {
   }

   public static boolean isProtectedMaster(LivingEntity attacker, LivingEntity target) {
      if (!(target instanceof ServerPlayer master) || attacker == null) return false;
      if (attacker instanceof ServantEntity servant) {
         return isHeavyServant(servant) && servant.isBoundTo(master);
      }
      if (!(attacker instanceof ServerPlayer servantPlayer)) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = servantPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || !isHeavyServantId(vars.servant_card_id)) return false;
      return MasterServantLinkService.getLinkedMaster(servantPlayer, vars) == master;
   }

   private static boolean isHeavyServant(ServantEntity servant) {
      return servant instanceof HeraclesEntity || servant instanceof GawainEntity;
   }

   private static boolean isHeavyServantId(String servantId) {
      return "heracles".equals(servantId) || "gawain".equals(servantId);
   }
}
