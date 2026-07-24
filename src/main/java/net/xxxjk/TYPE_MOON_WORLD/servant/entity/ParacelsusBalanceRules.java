package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

public final class ParacelsusBalanceRules {
   public static final float DAMAGE_MULTIPLIER = 0.8F;

   private ParacelsusBalanceRules() {
   }

   public static float reduceDamage(float currentDamage) {
      return Math.max(0.0F, currentDamage) * DAMAGE_MULTIPLIER;
   }

   public static double reduceDamage(double currentDamage) {
      return Math.max(0.0, currentDamage) * DAMAGE_MULTIPLIER;
   }
}
