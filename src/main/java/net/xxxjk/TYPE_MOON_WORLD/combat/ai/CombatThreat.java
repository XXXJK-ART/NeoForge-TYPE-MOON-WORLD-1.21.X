package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public record CombatThreat(
   ResourceLocation actionId,
   UUID sourceUuid,
   @Nullable UUID targetUuid,
   Vec3 origin,
   Vec3 direction,
   Shape shape,
   double radius,
   double length,
   int danger,
   long startTick,
   long impactTick,
   long endTick,
   boolean blockable,
   boolean dodgeable,
   boolean interruptible
) {
   public CombatThreat {
      if (actionId == null || sourceUuid == null || origin == null) throw new IllegalArgumentException("invalid combat threat");
      direction = direction == null || direction.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : direction.normalize();
      shape = shape == null ? Shape.POINT : shape;
      radius = Math.max(0.0, radius);
      length = Math.max(0.0, length);
      danger = Math.max(0, danger);
      impactTick = Math.max(startTick, impactTick);
      endTick = Math.max(impactTick, endTick);
   }

   public boolean activeAt(long tick) {
      return tick >= startTick && tick <= endTick;
   }

   public long ticksToImpact(long tick) {
      return Math.max(0L, impactTick - tick);
   }

   public boolean threatens(Vec3 point, double padding) {
      if (shape == Shape.SPHERE || shape == Shape.POINT) {
         double reach = radius + padding;
         return origin.distanceToSqr(point) <= reach * reach;
      }
      Vec3 relative = point.subtract(origin);
      double along = relative.dot(direction);
      if (along < -padding || along > length + padding) return false;
      Vec3 closest = origin.add(direction.scale(Math.max(0.0, Math.min(length, along))));
      double reach = radius + padding;
      return closest.distanceToSqr(point) <= reach * reach;
   }

   public enum Shape { POINT, SPHERE, LINE, CONE, HEMISPHERE }
}
