package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import javax.annotation.Nullable;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantFaction;

public final class ServantFactionAdvantage {
   private ServantFactionAdvantage() {
   }

   public static double computeMultiplier(@Nullable ServantFaction attacker, @Nullable ServantFaction defender) {
      if (attacker == null || defender == null || attacker == defender) {
         return 1.0;
      }

      return switch (attacker) {
         case HEAVEN -> defender == ServantFaction.EARTH ? 1.1 : 1.0;
         case EARTH -> defender == ServantFaction.HUMAN ? 1.1 : 1.0;
         case HUMAN -> defender == ServantFaction.HEAVEN ? 1.1 : 1.0;
         case STAR -> defender == ServantFaction.BEAST ? 1.1 : 1.0;
         case BEAST -> defender == ServantFaction.STAR ? 1.1 : 1.0;
      };
   }
}
