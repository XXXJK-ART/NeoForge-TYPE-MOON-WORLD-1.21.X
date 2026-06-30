package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.StatRank;

public final class ServantCombatFormulas {
   public static final double OUT_OF_COMBAT_SPEED = 0.20;

   private ServantCombatFormulas() {
   }

   public static int rankStep(StatRank rank, boolean plus) {
      int base = switch (rank == null ? StatRank.E : rank) {
         case E -> 0;
         case D -> 1;
         case C -> 2;
         case B -> 3;
         case A -> 4;
      };
      return plus ? Math.min(5, base + 1) : base;
   }

   public static int strengthStep(ServantParams params) {
      return params == null ? 0 : rankStep(params.strength(), params.strengthPlus());
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

   public static int dodgeInvulnerabilityTicks(ServantParams params) {
      return secondsToTicks((0.30 + agilityStep(params) * 0.02) * agilityMultiplier(params));
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
      return 80.0 + enduranceStep(params) * 20.0;
   }

   public static double staminaRegenPerSecond(ServantParams params) {
      return 8.0 + enduranceStep(params) * 2.0;
   }

   public static double blockReduction(ServantParams params) {
      return Math.min(0.95, 0.65 + enduranceStep(params) * 0.02);
   }

   public static int parryWindowTicks(ServantParams params) {
      return secondsToTicks(0.20 + enduranceStep(params) * 0.01);
   }

   public static double blockStaminaCost(ServantParams params) {
      return Math.max(5.0, 12.0 - enduranceStep(params));
   }

   public static double parryStaminaCost(ServantParams params) {
      return 5.0;
   }

   public static double poiseMax(ServantParams params) {
      return (80.0 + enduranceStep(params) * 20.0 + strengthStep(params) * 10.0) * toughnessMultiplier(params);
   }

   public static double poiseRegenPerSecond(ServantParams params) {
      return (5.0 + enduranceStep(params) * 2.0) * toughnessMultiplier(params);
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
}
