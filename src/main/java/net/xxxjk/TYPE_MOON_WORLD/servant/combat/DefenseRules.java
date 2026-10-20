package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

public final class DefenseRules {
   private DefenseRules() { }
   public static double poiseLoss(double actualDamage) { return Math.max(10.0, actualDamage * 0.5); }
   public static int safeDodgeCooldown(int proposedTicks, int longestInvulnerability) {
      return Math.max(proposedTicks, longestInvulnerability + 10);
   }
   public static double guardCost(double base, double bonus, boolean active, boolean perfect) {
      return guardCost(base, bonus, active, perfect, 0.0);
   }
   public static double guardCost(double base, double defenderReduction, boolean active,
                                  boolean perfect, double attackerSouwaBonus) {
      return net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroRules.guardStaminaCost(
         base, defenderReduction, active, perfect, attackerSouwaBonus);
   }
   public static float guardedDamage(float amount, boolean active, boolean perfect) {
      return perfect ? 0 : amount * (active ? 0.2F : 0.5F);
   }
}
