package net.xxxjk.TYPE_MOON_WORLD.servant.card;

public final class ServantCardUshiwakamaruRules {
   private ServantCardUshiwakamaruRules() {
   }

   public static int jumpLimit(boolean eightBoatActive) {
      return eightBoatActive ? 8 : 4;
   }

   public static int clampJumpCharges(int charges, boolean eightBoatActive) {
      return Math.max(0, Math.min(jumpLimit(eightBoatActive), charges));
   }

   public static boolean canRecoverJump(boolean eightBoatActive, boolean standingOnCollision) {
      return eightBoatActive || standingOnCollision;
   }

   public static float spiderSlayerDamage(boolean demonic) {
      return spiderSlayerDamage(demonic, 0.0);
   }

   public static float spiderSlayerDamage(boolean demonic, double distance) {
      return net.xxxjk.TYPE_MOON_WORLD.servant.entity.UshiwakamaruCombatRules.spiderSlayerDamage(demonic, distance);
   }

   public static double eightBoatLeapDistance(double requestedDistance, boolean targetValid, boolean landingValid) {
      return targetValid && landingValid ? Math.max(0.0, Math.min(32.0, requestedDistance)) : 0.0;
   }

   public static net.minecraft.world.phys.Vec3 eightBoatLeapVector(net.minecraft.world.phys.Vec3 origin,
                                                             net.minecraft.world.phys.Vec3 target,
                                                             double stopDistance) {
      net.minecraft.world.phys.Vec3 delta = target.subtract(origin).multiply(1.0, 0.0, 1.0);
      double distance = Math.min(32.0, Math.max(0.0, delta.length() - Math.max(0.0, stopDistance)));
      return delta.lengthSqr() < 1.0E-8 ? null : origin.add(delta.normalize().scale(distance));
   }

}
