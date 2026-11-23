package net.xxxjk.typemoonworld.smoke;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantBaseAction;

@Mod(value = "typemoonworld_zhao_yun_smoke", dist = Dist.DEDICATED_SERVER)
public final class ZhaoYunDedicatedSmoke {
   private int ticks;
   public ZhaoYunDedicatedSmoke() { NeoForge.EVENT_BUS.register(this); }
   @SubscribeEvent public void tick(ServerTickEvent.Post event) {
      if (++ticks != 40) return;
      if (ModSounds.ZHAO_YUN_VOICE_ATTACK.get() != null
         && ServantBaseAction.zhaoYunAttack(ServantBaseAction.SWORD_COMBO, 0) == ServantBaseAction.ZHAO_YUN_COMBO) {
         TYPE_MOON_WORLD.LOGGER.info("ZHAO_YUN_DEDICATED_SMOKE_PASS registered_voice=true common_action_mapping=true");
      } else TYPE_MOON_WORLD.LOGGER.error("ZHAO_YUN_DEDICATED_SMOKE_FAIL");
      event.getServer().halt(false);
   }
}
