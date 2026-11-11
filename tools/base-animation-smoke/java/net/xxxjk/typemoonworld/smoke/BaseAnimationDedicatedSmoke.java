package net.xxxjk.typemoonworld.smoke;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantBaseAction;

@Mod(value = "typemoonworld_base_animation_smoke", dist = Dist.DEDICATED_SERVER)
public final class BaseAnimationDedicatedSmoke {
   private int ticks;

   public BaseAnimationDedicatedSmoke() { NeoForge.EVENT_BUS.register(this); }

   @SubscribeEvent
   public void tick(ServerTickEvent.Post event) {
      if (++ticks != 40) return;
      try {
         require("servant_base_sword_huijian1".equals(ServantBaseAction.animationId(ServantBaseAction.SWORD, 0)),
            "base action common IDs failed on dedicated server");
         require("servant_base_sword_huijian".equals(ServantBaseAction.animationId(ServantBaseAction.SWORD_COMBO, 0)),
            "continuous action common IDs failed on dedicated server");
         require(ServantBaseAction.STOP == -1, "stop ID changed");
         TYPE_MOON_WORLD.LOGGER.info("BASE_ANIMATION_DEDICATED_SMOKE_PASS client_classes=not_loaded common_action_ids=valid");
      } catch (Throwable error) {
         TYPE_MOON_WORLD.LOGGER.error("BASE_ANIMATION_DEDICATED_SMOKE_FAIL", error);
      }
      event.getServer().halt(false);
   }

   private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
