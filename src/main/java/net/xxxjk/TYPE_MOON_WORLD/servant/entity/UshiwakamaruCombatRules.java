package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

public final class UshiwakamaruCombatRules {
   public static final float SHIELD_MAX_HP = 1000.0F;
   public static final double SPIDER_SLAYER_RANGE = 15.0;

   private UshiwakamaruCombatRules() {
   }

   public static int phaseFor(float health, float maxHealth, int currentPhase) {
      float ratio = health / Math.max(1.0F, maxHealth);
      int desired = ratio <= 0.33F ? 3 : ratio <= 0.66F ? 2 : 1;
      return Math.max(Math.max(1, Math.min(3, currentPhase)), desired);
   }

   public static float swallowDodgeChance(boolean attackFromAbove) {
      return attackFromAbove ? 0.45F : 0.30F;
   }

   public static float applyRidingDefense(float amount) {
      return Math.max(0.0F, amount) * 0.85F;
   }

   public static ShieldHit absorbShieldHit(float shieldHp, float incomingDamage) {
      float remaining = Math.max(0.0F, shieldHp - Math.max(0.0F, incomingDamage));
      return new ShieldHit(remaining, remaining <= 0.0F);
   }

   public static float spiderSlayerDamage(boolean demonic, double distance) {
      double falloff = 1.0 - 0.5 * Math.max(0.0, Math.min(SPIDER_SLAYER_RANGE, distance)) / SPIDER_SLAYER_RANGE;
      return (float)(100.0 * falloff * (demonic ? 2.0 : 1.0));
   }

   public record ShieldHit(float remainingShieldHp, boolean broken) {
   }
}
