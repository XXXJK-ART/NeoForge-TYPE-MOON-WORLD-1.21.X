package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GawainCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GawainEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesGodHandHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

/** Live combat dispositions that deliberately trade defense for forward pressure. */
public final class ServantCombatDisposition {
   private ServantCombatDisposition() { }

   public static boolean isRelentlessAdvance(ServantEntity entity) {
      return entity != null && (HeraclesGodHandHelper.hasGodHand(entity)
         || entity instanceof GawainEntity gawain && GawainCombatHelper.hasSunBlessing(gawain));
   }
}
