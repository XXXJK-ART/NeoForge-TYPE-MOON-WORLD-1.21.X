package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Shared EA/Excalibur three-dimensional beam geometry, including 180-degree turns. */
public final class BeamSweepGeometry {
   private BeamSweepGeometry() { }
   public static Vec3 normalized(Vec3 value) {
      return value == null || value.lengthSqr() < 1.0E-8 ? new Vec3(0, 0, 1) : value.normalize();
   }
   public static List<Vec3> directions(Vec3 previous, Vec3 current) {
      Vec3 to = normalized(current), from = previous == null ? to : normalized(previous);
      double dot = Math.clamp(from.dot(to), -1, 1), angle = Math.acos(dot);
      if (angle < 1.0E-5) return List.of(to);
      Vec3 axis = from.cross(to);
      if (axis.lengthSqr() < 1.0E-8) axis = Math.abs(from.y) < .9
         ? new Vec3(0, 1, 0).subtract(from.scale(from.y)) : new Vec3(1, 0, 0).subtract(from.scale(from.x));
      axis = axis.normalize();
      int steps = Math.max(1, (int)Math.ceil(angle / Math.toRadians(2)));
      List<Vec3> result = new ArrayList<>(steps + 1);
      for (int i = 0; i <= steps; i++) {
         double a = angle * i / steps;
         result.add(i == steps ? to : from.scale(Math.cos(a)).add(axis.cross(from).scale(Math.sin(a))).normalize());
      }
      return result;
   }
   public static boolean intersects(AABB bounds, Vec3 start, Vec3 forward, double length, double width, double height) {
      Vec3 direction = normalized(forward);
      Vec3 right = direction.cross(Math.abs(direction.y) > .95 ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0)).normalize();
      Vec3 up = right.cross(direction).normalize(), relative = bounds.getCenter().subtract(start);
      double along = relative.dot(direction), radius = radius(bounds, direction);
      if (along + radius < -3 || along - radius > length) return false;
      double beamAlong = Math.clamp(along, 0, length);
      double taper = Math.max(.22, Math.sin(Math.PI * beamAlong / length));
      double allowed = along < 0 ? 2.8 : width * Math.pow(taper, .35);
      return Math.abs(relative.dot(right)) <= allowed + radius(bounds, right)
         && Math.abs(relative.dot(up)) <= height + radius(bounds, up);
   }
   private static double radius(AABB box, Vec3 axis) {
      return (Math.abs(axis.x) * box.getXsize() + Math.abs(axis.y) * box.getYsize() + Math.abs(axis.z) * box.getZsize()) * .5;
   }
}
