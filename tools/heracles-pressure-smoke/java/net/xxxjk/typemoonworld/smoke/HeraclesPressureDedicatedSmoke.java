package net.xxxjk.typemoonworld.smoke;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.AttackOrigin;
import net.xxxjk.TYPE_MOON_WORLD.servant.heracles.*;

@Mod(value="typemoonworld_heracles_smoke",dist=Dist.DEDICATED_SERVER)
public final class HeraclesPressureDedicatedSmoke {
   private int ticks;
   public HeraclesPressureDedicatedSmoke() { NeoForge.EVENT_BUS.register(this); }
   @SubscribeEvent public void tick(ServerTickEvent.Post event) {
      if(++ticks!=40)return;
      try {
         var level=event.getServer().overworld();var npc=ModEntities.HERACLES.get().create(level);
         npc.readAdditionalSaveData(npc.saveWithoutId(new CompoundTag()));npc.setNoAi(true);npc.setPos(0,105,0);level.addFreshEntity(npc);
         for(String key:new String[]{"revive","adapt","slam_windup","frenzy_walk","frenzy_impact","martial_8"})
            require(npc.hasActionAnimation(key),"missing action: "+key);
         int lives=GodHandService.read(npc).lives();
         require(GodHandService.revive(npc,npc.damageSources().generic(),AttackOrigin.skill("qa","first")),"first lethal failed");
         require(GodHandService.revive(npc,npc.damageSources().generic(),AttackOrigin.skill("qa","second")),"second lethal failed");
         require(GodHandService.read(npc).lives()==lives-2 && npc.invulnerableTime==0,"reactions changed revival rules");
         for(int i=0;i<100;i++)GodHandService.tick(npc);
         require(GodHandService.read(npc).adapted().contains(AttackOrigin.skill("qa","second").key()),"adaptation failed");
         TYPE_MOON_WORLD.LOGGER.info("HERACLES_PRESSURE_DEDICATED_PASS animations=loaded revival=no_lock adaptation=completed");
      } catch(Throwable error) { TYPE_MOON_WORLD.LOGGER.error("HERACLES_PRESSURE_DEDICATED_FAIL",error); }
      event.getServer().halt(false);
   }
   private static void require(boolean value,String message) { if(!value)throw new AssertionError(message); }
}
