package net.xxxjk.TYPE_MOON_WORLD.servant.palerider;

public final class PaleRiderCombatRules {
   public static final int MAX_CROWS = 50;
   public static final int DEATH_JUDGMENT_INTERVAL_TICKS = 10 * 20;
   public static final int ASH_STEP_COOLDOWN_TICKS = 5 * 20;
   public static final int PLAGUE_RUSH_COOLDOWN_TICKS = 7 * 20;
   public static final int DEATH_PULSE_COOLDOWN_TICKS = 10 * 20;
   public static final int MINOR_SKILL_LOCK_TICKS = 30;
   public static final double UNDERWORLD_CAST_MP_COST = 150.0;
   public static final double CALAMITY_CAST_MP_COST = 300.0;
   public static final double CALAMITY_UPKEEP_MP_PER_SECOND = 10.0;
   public static final double ASH_STEP_MP_COST = 8.0;
   public static final double PLAGUE_RUSH_MP_COST = 12.0;
   public static final double DEATH_PULSE_MP_COST = 18.0;

   private PaleRiderCombatRules() {
   }

   public static boolean canSpawnCrow(int currentCount) {
      return currentCount < MAX_CROWS;
   }

   public static float plagueSpecialAttackMultiplier(String servantId) {
      return "enkidu".equals(servantId) ? 2.0F : 1.0F;
   }
}
