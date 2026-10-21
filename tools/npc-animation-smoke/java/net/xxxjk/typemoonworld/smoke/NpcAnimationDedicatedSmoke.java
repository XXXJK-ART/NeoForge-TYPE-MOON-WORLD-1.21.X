package net.xxxjk.typemoonworld.smoke;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.heracles.HeraclesAbilityService;

@Mod(value = "typemoonworld_npc_animation_smoke", dist = Dist.DEDICATED_SERVER)
public final class NpcAnimationDedicatedSmoke {
   private int ticks;
   public NpcAnimationDedicatedSmoke() { NeoForge.EVENT_BUS.register(this); }

   @SubscribeEvent
   public void tick(ServerTickEvent.Post event) {
      if (++ticks != 40) return;
      try {
         var level = event.getServer().overworld();
         var heracles = ModEntities.HERACLES.get().create(level);
         var oda = ModEntities.ODA_NOBUNAGA.get().create(level);
         heracles.readAdditionalSaveData(heracles.saveWithoutId(new CompoundTag()));
         oda.readAdditionalSaveData(oda.saveWithoutId(new CompoundTag()));
         heracles.setNoAi(true); heracles.setNoGravity(true); heracles.setPos(0, 200, 0);
         oda.setNoAi(true); oda.setNoGravity(true); oda.setPos(4, 200, 0);
         var target = EntityType.HUSK.create(level);
         target.setNoAi(true); target.setNoGravity(true); target.setPos(0, 200, 3);
         level.addFreshEntity(heracles); level.addFreshEntity(oda); level.addFreshEntity(target);
         heracles.setTarget(target); oda.setTarget(target);
         heracles.tick(); oda.tick();
         require(heracles.isFrenzyAnimationActive(), "nearby combat did not enable frenzy");
         require(oda.isCombatReady(), "Oda combat pose did not activate");
         heracles.triggerBasicAttackAnimation();
         oda.triggerBasicAttackAnimation();
         require("attack_slash".equals(oda.getActionAnimation()), "Oda basic slash not synchronized");
         target.setPos(0, 200, 12); heracles.tick(); oda.tick();
         require(!heracles.isFrenzyAnimationActive(), "distant pursuit still swings");
         heracles.setCurrentMp(1000);
         require(HeraclesAbilityService.start(heracles, 1), "slam did not start");
         heracles.tick();
         require(heracles.isFrenzyAnimationActive(), "active skill did not sustain frenzy");
         HeraclesAbilityService.cancel(heracles);
         target.setPos(0, 200, 3);
         heracles.getPersistentData().putLong(ServantCombatSystem.TAG_STUN_UNTIL, level.getGameTime() + 40);
         oda.getPersistentData().putLong(ServantCombatSystem.TAG_STUN_UNTIL, level.getGameTime() + 40);
         heracles.tick(); oda.tick();
         require(!heracles.isFrenzyAnimationActive() && !oda.isCombatReady(), "stun did not stop combat display state");
         heracles.getPersistentData().remove(ServantCombatSystem.TAG_STUN_UNTIL);
         oda.getPersistentData().remove(ServantCombatSystem.TAG_STUN_UNTIL);
         heracles.setTarget(null); oda.setTarget(null); heracles.tick(); oda.tick();
         require(!heracles.isFrenzyAnimationActive() && !oda.isCombatReady(), "lost target did not clear combat pose");
         TYPE_MOON_WORLD.LOGGER.info("NPC_ANIMATION_DEDICATED_SMOKE_PASS frenzy=nearby,skills Oda=ready,slash clear=distance,stun,target_loss");
      } catch (Throwable error) {
         TYPE_MOON_WORLD.LOGGER.error("NPC_ANIMATION_DEDICATED_SMOKE_FAIL", error);
      }
      event.getServer().halt(false);
   }

   private static void require(boolean condition, String message) {
      if (!condition) throw new AssertionError(message);
   }
}
