package net.xxxjk.TYPE_MOON_WORLD.servant.card;

final class ServantCardUshiwakamaruRules {
   private ServantCardUshiwakamaruRules() {
   }

   static int jumpLimit(boolean eightBoatActive) {
      return eightBoatActive ? 8 : 4;
   }

   static int clampJumpCharges(int charges, boolean eightBoatActive) {
      return Math.max(0, Math.min(jumpLimit(eightBoatActive), charges));
   }

   static boolean canRecoverJump(boolean eightBoatActive, boolean standingOnCollision) {
      return eightBoatActive || standingOnCollision;
   }

   static float spiderSlayerDamage(boolean demonic) {
      return demonic ? 150.0F : 100.0F;
   }
}
