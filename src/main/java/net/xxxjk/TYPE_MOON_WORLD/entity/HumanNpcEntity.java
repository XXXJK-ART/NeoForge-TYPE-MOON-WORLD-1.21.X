package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.MountedCombatIntentService;

/** Marker base for the human faction. Faction membership does not imply NPC-to-NPC alliance. */
public abstract class HumanNpcEntity extends PathfinderMob {
   protected HumanNpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
      super(type, level);
   }

   @Override
   protected void customServerAiStep() {
      MountedCombatIntentService.tick(this);
      super.customServerAiStep();
   }

   public final boolean isHumanFaction() {
      return true;
   }
}
