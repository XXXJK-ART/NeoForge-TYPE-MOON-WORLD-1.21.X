package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

public final class UshiwakamaruCombatRules {
   public static final float SHIELD_MAX_HP = 2000.0F;

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

   public record ShieldHit(float remainingShieldHp, boolean broken) {
   }
}
