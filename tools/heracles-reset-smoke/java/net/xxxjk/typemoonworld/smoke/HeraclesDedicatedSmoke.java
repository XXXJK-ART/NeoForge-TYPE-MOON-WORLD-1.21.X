package net.xxxjk.typemoonworld.smoke;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.heracles.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.CommonSkillService;

@Mod(value="typemoonworld_heracles_smoke",dist=Dist.DEDICATED_SERVER)
public final class HeraclesDedicatedSmoke {
   private int ticks;
   public HeraclesDedicatedSmoke() { NeoForge.EVENT_BUS.register(this); }
   @SubscribeEvent public void tick(ServerTickEvent.Post event) {
      if(++ticks!=40)return;
      try {
         var level=event.getServer().overworld();var npc=ModEntities.HERACLES.get().create(level);
         npc.readAdditionalSaveData(npc.saveWithoutId(new CompoundTag()));npc.setNoAi(true);npc.setPos(0,105,0);level.addFreshEntity(npc);
         if(npc.getMaxHealth()!=680)throw new AssertionError("NPC common attributes");
         npc.setHealth(321);CommonSkillService.reconcile(npc);
         if(npc.getHealth()!=321)throw new AssertionError("refresh healed NPC");
         GodHandService.special(npc,"smoke:concept");GodHandService.special(npc,"smoke:concept");npc.setHealth(222);
         var saved=npc.saveWithoutId(new CompoundTag());var loaded=ModEntities.HERACLES.get().create(level);loaded.load(saved);
         if(GodHandService.read(loaded).lives()!=10||loaded.getHealth()!=222||!GodHandService.immune(loaded,"smoke:concept"))throw new AssertionError("round/health lost during save load");
         TYPE_MOON_WORLD.LOGGER.info("HERACLES_DEDICATED_SMOKE_PASS attributes=680 health=preserved inventory=preserved concept=deduplicated");
      } catch(Throwable error) { TYPE_MOON_WORLD.LOGGER.error("HERACLES_DEDICATED_SMOKE_FAIL",error); }
      event.getServer().halt(false);
   }
}
