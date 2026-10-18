package net.xxxjk.typemoonworld.smoke;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;

@Mod(value = "typemoonworld_oda_smoke", dist = Dist.DEDICATED_SERVER)
public final class OdaDedicatedSmoke {
   private int ticks;
   public OdaDedicatedSmoke() { NeoForge.EVENT_BUS.register(this); }
   @SubscribeEvent public void tick(ServerTickEvent.Post event) {
      if (++ticks != 40) return;
      try {
         var definition = ServantDataRegistry.get("oda_nobunaga");
         if (definition == null || definition.noblePhantasmIds().size() != 2 || definition.parameters().luck().coefficient() != 25) throw new AssertionError("Nobunaga definitions failed to load");
         var owner = ModEntities.ODA_NOBUNAGA.get().create(event.getServer().overworld());
         owner.setPos(0,105,0); owner.setNoAi(true); event.getServer().overworld().addFreshEntity(owner);
         net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaAbilityService.cast(owner,0);
         TYPE_MOON_WORLD.LOGGER.info("ODA_DEDICATED_SMOKE_PASS definitions=loaded entity=spawned guns=summoned old_save=loaded");
      } catch (Throwable error) { TYPE_MOON_WORLD.LOGGER.error("ODA_DEDICATED_SMOKE_FAIL",error); }
      event.getServer().halt(false);
   }
}
