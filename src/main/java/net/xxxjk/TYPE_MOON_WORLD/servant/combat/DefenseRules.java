package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

public final class DefenseRules {
   private DefenseRules() { }
   public static double poiseLoss(double actualDamage) { return Math.max(10.0, actualDamage * 0.5); }
   public static int safeDodgeCooldown(int proposedTicks, int longestInvulnerability) {
      return Math.max(proposedTicks, longestInvulnerability + 10);
   }
   public static double guardCost(double base, double bonus, boolean active, boolean perfect) {
      return perfect ? 0 : base * (1.0 - Math.min(0.25, Math.max(0, bonus))) * (active ? 1 : 2);
   }
   public static float guardedDamage(float amount, boolean active, boolean perfect) {
      return perfect ? 0 : amount * (active ? 0.2F : 0.5F);
   }
}
