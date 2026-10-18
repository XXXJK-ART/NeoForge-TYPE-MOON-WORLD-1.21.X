package net.xxxjk.TYPE_MOON_WORLD.magic.npc;

import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;

/** Cheap hazard gate used before the expensive magic decision phase. */
public final class NpcHazardController {
   public static final String EMERGENCY_TAG = "TypeMoonNpcEmergencyHazard";
   private NpcHazardController() {
   }

   public static boolean needsEmergencyAction(MysticMagicianEntity npc) {
      if (npc == null || !npc.isAlive()) return false;
      LivingEntity target = npc.getTarget();
      return npc.isOnFire() || (target != null && target.isAlive() && npc.getHealth() <= npc.getMaxHealth() * 0.15F);
   }

   public static void update(MysticMagicianEntity npc) {
      if (npc != null) npc.getPersistentData().putBoolean(EMERGENCY_TAG, needsEmergencyAction(npc));
   }
}
