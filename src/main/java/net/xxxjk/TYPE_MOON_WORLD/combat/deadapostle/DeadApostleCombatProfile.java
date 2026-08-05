package net.xxxjk.TYPE_MOON_WORLD.combat.deadapostle;

/**
 * Data-only combat profile for high-tier dead apostles.
 *
 * <p>The low-tier dead apostles intentionally do not use this system. Keeping
 * the profile separate from ServantParams prevents accidental coupling to the
 * servant card/runtime systems.</p>
 */
public record DeadApostleCombatProfile(
   double maxHealth,
   double attackDamage,
   double movementSpeed,
   double armor,
   double armorToughness,
   double knockbackResistance,
   double followRange,
   double staminaMax,
   double staminaRegenPerSecond,
   double blockReduction,
   double blockStaminaCost,
   double dodgeChance,
   double urgentDodgeChance,
   int dodgeCooldownTicks,
   int dodgeInvulnerabilityTicks,
   double poiseMax,
   double poiseRegenPerSecond,
   double poiseDamagePerHit,
   int guardBreakTicks
) {
   public static DeadApostleCombatProfile neroChaos() {
      return new DeadApostleCombatProfile(
         400.0, 20.0, 0.32, 12.0, 4.0, 0.30, 48.0,
         140.0, 14.0, 0.35, 9.0,
         0.72, 0.90, 13, 9,
         170.0, 11.0, 10.0, 18
      );
   }
}
