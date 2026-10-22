package net.xxxjk.typemoonworld.smoke;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.cu.*;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

@Mod("typemoonworld_cu_smoke")
public final class CuServerSmoke {
   private int ticks;
   public CuServerSmoke(){NeoForge.EVENT_BUS.register(this);}
   @SubscribeEvent public void tick(ServerTickEvent.Post event){
      var server=event.getServer();if(server.isSingleplayer())return;
      if(++ticks==20){
         var level=server.overworld();var owner=ModEntities.CU_CHULAINN.get().create(level);
         owner.setPos(0,100,0);owner.setNoAi(true);owner.setNoGravity(true);level.addFreshEntity(owner);owner.ensureDefaultNpcLoadout(false);owner.setCurrentMp(600);
         if(!CuChulainnAbilityService.start(owner,0))throw new AssertionError("dedicated Ansuz rejected");
      }
      if(ticks==70){TYPE_MOON_WORLD.LOGGER.info("CU_DEDICATED_SMOKE_OK entities, shared skills and logical-side loading");server.halt(false);}
   }
}
