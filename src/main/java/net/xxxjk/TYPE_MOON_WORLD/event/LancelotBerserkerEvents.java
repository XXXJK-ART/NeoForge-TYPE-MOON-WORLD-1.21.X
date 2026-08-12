package net.xxxjk.TYPE_MOON_WORLD.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.LancelotBerserkerEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.lancelot.LancelotCombatHelper;

@EventBusSubscriber(modid = "typemoonworld")
public final class LancelotBerserkerEvents {
   private LancelotBerserkerEvents() {
   }

   @SubscribeEvent
   public static void onLivingDeath(LivingDeathEvent event) {
      if (event.getSource().getEntity() instanceof LancelotBerserkerEntity lancelot) {
         LancelotCombatHelper.recordKill(lancelot);
      }
      if (event.getEntity() instanceof LancelotBerserkerEntity lancelot) {
         LancelotCombatHelper.stopAroundight(lancelot, false);
      }
   }
}
