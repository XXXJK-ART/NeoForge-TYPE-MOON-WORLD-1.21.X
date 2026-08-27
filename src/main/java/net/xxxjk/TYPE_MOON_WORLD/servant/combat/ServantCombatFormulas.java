package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.StatRank;

public final class ServantCombatFormulas {
   public static final double OUT_OF_COMBAT_SPEED = 0.20;
   public static final double SERVANT_SPEED_E = StatRank.E.toMovementSpeed();
   public static final double SERVANT_SPEED_D = StatRank.D.toMovementSpeed();
   private static final double BASE_STAMINA = 60.0;
   private static final double STAMINA_PER_STRENGTH_STEP = 8.0;
   private static final double BASE_POISE = 56.0;
   private static final double POISE_PER_ENDURANCE_STEP = 12.0;
   private static final double DEFENSE_ARMOR_TOUGHNESS_BASE = 0.8;
   private static final double DEFENSE_ARMOR_TOUGHNESS_PER_STEP = 0.3;
   private static final double DEFENSE_RECOVERY_PER_SECOND = 6.0;
   private static final double POISE_RECOVERY_PER_SECOND = 6.0;

   private ServantCombatFormulas() {
   }

   public static int rankStep(StatRank rank, boolean plus) {
      int base = switch (rank == null ? StatRank.E : rank) {
         case E -> 0;
         case D -> 1;
         case C -> 2;
         case B -> 3;
         case A -> 4;
         // Keep A++ above A+ so the three-times rank remains distinguishable.
         case A_PLUS_PLUS -> 6;
      };
      return plus ? (rank == StatRank.A_PLUS_PLUS ? base : Math.min(5, base + 1)) : base;
   }

   public static int strengthStep(ServantParams params) {
      return params == null ? 0 : rankStep(params.strength(), params.strengthPlus());
   }

   /** Defense is derived from Strength; keep the semantic name at call sites. */
   public static int defenseStep(ServantParams params) {
      return strengthStep(params);
   }

   public static int enduranceStep(ServantParams params) {
      return params == null ? 0 : rankStep(params.endurance(), params.endurancePlus());
   }

   public static int agilityStep(ServantParams params) {
      return params == null ? 0 : rankStep(params.agility(), params.agilityPlus());
   }

   public static int magicStep(ServantParams params) {
      return params == null ? 0 : rankStep(params.magic(), params.magicPlus());
   }

   public static double combatMovementSpeed(ServantParams params) {
      return switch (agilityStep(params)) {
         case 0 -> 0.200;
         case 1 -> 0.260;
         case 2 -> 0.320;
         case 3 -> 0.380;
         case 4 -> 0.440;
         default -> 0.500;
      };
   }

   public static double rampedMovementSpeed(ServantParams params, int runningTicks, double maxSpeed) {
      double fastest = Math.max(SERVANT_SPEED_E, maxSpeed);
      if (agilityStep(params) <= 0 || fastest <= SERVANT_SPEED_E + 1.0E-6) {
         return SERVANT_SPEED_E;
      }
      int ticks = Math.max(0, Math.min(40, runningTicks));
      double firstTarget = Math.min(SERVANT_SPEED_D, fastest);
      if (ticks <= 20) {
         return lerp(SERVANT_SPEED_E, firstTarget, ticks / 20.0);
      }
      return lerp(firstTarget, fastest, (ticks - 20) / 20.0);
   }

   public static int dodgeInvulnerabilityTicks(ServantParams params) {
      return secondsToTicks((0.30 + agilityStep(params) * 0.02) * agilityMultiplier(params));
   }

   public static double baseDodgeChance(ServantParams params, boolean urgent) {
      // Agility is the only rank that changes the baseline dodge chance.
      double chance = 0.12 + agilityStep(params) * 0.04 + (urgent ? 0.12 : 0.0);
      return Math.min(0.75, chance);
   }

   public static int perfectDodgeInvulnerabilityTicks(ServantParams params) {
      return Math.max(10, secondsToTicks(0.5 * agilityMultiplier(params)));
   }

   public static int dodgeCooldownTicks(ServantParams params) {
      return secondsToTicks(Math.max(0.70, 1.0 - agilityStep(params) * 0.05) / agilityMultiplier(params));
   }

   public static int perfectDodgeWindowTicks(ServantParams params) {
      return secondsToTicks((0.15 + agilityStep(params) * 0.01) * agilityMultiplier(params));
   }

   public static double dodgeMpCost(ServantParams params) {
      return Math.max(2.0, 5.0 - agilityStep(params) * 0.5);
   }

   public static double staminaMax(ServantParams params) {
      return BASE_STAMINA + defenseStep(params) * STAMINA_PER_STRENGTH_STEP;
   }

   public static double staminaRegenPerSecond(ServantParams params) {
      return DEFENSE_RECOVERY_PER_SECOND;
   }

   public static double blockReduction(ServantParams params) {
      return Math.min(0.46, 0.28 + defenseStep(params) * 0.025);
   }

   public static int parryWindowTicks(ServantParams params) {
      return secondsToTicks(0.20 + defenseStep(params) * 0.01);
   }

   public static double blockStaminaCost(ServantParams params) {
      return Math.max(5.0, 12.0 - defenseStep(params));
   }

   public static double parryStaminaCost(ServantParams params) {
      return 5.0;
   }

   public static double poiseMax(ServantParams params) {
      return (BASE_POISE + enduranceStep(params) * POISE_PER_ENDURANCE_STEP) * toughnessMultiplier(params);
   }

   public static double poiseRegenPerSecond(ServantParams params) {
      return POISE_RECOVERY_PER_SECOND * toughnessMultiplier(params);
   }

   /** Vanilla armor toughness used by both manifested servants and servant cards. */
   public static double armorToughness(ServantParams params) {
      return DEFENSE_ARMOR_TOUGHNESS_BASE + defenseStep(params) * DEFENSE_ARMOR_TOUGHNESS_PER_STEP;
   }

   public static double toughnessMultiplier(ServantParams params) {
      return 1.0;
   }

   public static double agilityMultiplier(ServantParams params) {
      return 1.0;
   }

   public static double launcherPoiseCost(ServantParams params) {
      return Math.max(5.0, 25.0 - strengthStep(params) * 2.0);
   }

   public static int guardBreakTicks(ServantParams params) {
      return secondsToTicks(Math.max(0.2, 1.2 - enduranceStep(params) * 0.1));
   }

   public static double damageSuppressionThreshold(ServantParams params) {
      return 20.0 + magicStep(params) * 5.0;
   }

   public static double launcherDistance(ServantParams params) {
      return 5.0 + strengthStep(params) * 1.1;
   }

   public static int launcherHitstunTicks(ServantParams params) {
      return secondsToTicks(0.55 + strengthStep(params) * 0.07);
   }

   public static double comboProtectionHealPercentPerSecond(ServantParams params) {
      return 0.04 + enduranceStep(params) * 0.01;
   }

   public static double guardBreakDamageBonus(ServantParams params) {
      return Math.max(0.05, 0.25 - enduranceStep(params) * 0.02);
   }

   private static int secondsToTicks(double seconds) {
      return Math.max(1, (int)Math.round(seconds * 20.0));
   }

   private static double lerp(double from, double to, double progress) {
      return from + (to - from) * Math.max(0.0, Math.min(1.0, progress));
   }
}
