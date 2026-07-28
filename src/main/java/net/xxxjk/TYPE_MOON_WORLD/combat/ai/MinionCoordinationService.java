package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class MinionCoordinationService {
   private MinionCoordinationService() { }

   /** Stable surround slot derived from owner/minion identity, so slots do not reshuffle each tick. */
   public static Vec3 surroundPoint(LivingEntity owner, Mob minion, LivingEntity target, double radius) {
      int hash = 31 * owner.getUUID().hashCode() + minion.getUUID().hashCode();
      double angle = Math.floorMod(hash, 360) * Math.PI / 180.0;
      double ring = Math.max(1.5, radius) + Math.floorMod(hash >>> 9, 3) * 0.65;
      return target.position().add(Math.cos(angle) * ring, 0.0, Math.sin(angle) * ring);
   }

   public static boolean hasAttackSlot(Mob minion, LivingEntity target) {
      int phase = Math.floorMod(minion.getUUID().hashCode(), 4);
      return Math.floorMod((int)(minion.level().getGameTime() / 5L), 4) == phase
         || minion.distanceToSqr(target) > 9.0;
   }
}
