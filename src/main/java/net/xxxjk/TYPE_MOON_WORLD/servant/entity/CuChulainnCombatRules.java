package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

public final class CuChulainnCombatRules {
   public static final double MELEE_GAE_BOLG_RANGE = 3.75;
   public static final double MELEE_GAE_BOLG_COMMIT_RANGE = 7.0;
   public static final double PROJECTILE_GAE_BOLG_RANGE = 12.0;
   public static final int ARMY_DECISION_INTERVAL_TICKS = 40;
   public static final double ARMY_HEALTH_RATIO = 0.15;
   public static final double ARMY_CRITICAL_HEALTH_RATIO = 0.08;
   public static final double ARMY_MINIMUM_MP_RATIO = 0.65;
   public static final int ARMY_MINIMUM_GROUP_SIZE = 3;
   public static final double ARMY_FORMIDABLE_TARGET_HEALTH = 160.0;

   private CuChulainnCombatRules() {
   }

   public enum SingleGaeBolgPlan {
      NONE,
      CLOSE_FOR_MELEE,
      MELEE,
      PROJECTILE
   }

   public static SingleGaeBolgPlan singleGaeBolgPlan(double distance) {
      if (!Double.isFinite(distance) || distance < 0.0) {
         return SingleGaeBolgPlan.NONE;
      }
      if (distance <= MELEE_GAE_BOLG_RANGE) {
         return SingleGaeBolgPlan.MELEE;
      }
      if (distance <= MELEE_GAE_BOLG_COMMIT_RANGE) {
         return SingleGaeBolgPlan.CLOSE_FOR_MELEE;
      }
      if (distance <= PROJECTILE_GAE_BOLG_RANGE) {
         return SingleGaeBolgPlan.PROJECTILE;
      }
      return SingleGaeBolgPlan.NONE;
   }

   public static boolean isArmyDecisionDue(long now, long lastDecisionTick) {
      return lastDecisionTick <= 0L || now - lastDecisionTick >= ARMY_DECISION_INTERVAL_TICKS;
   }

   public static boolean isArmyFinalWindow(
      double healthRatio,
      boolean decisivePhase,
      int nearbyEnemyCount,
      double targetMaxHealth,
      double mpRatio
   ) {
      if (!decisivePhase || healthRatio > ARMY_HEALTH_RATIO || mpRatio < ARMY_MINIMUM_MP_RATIO) {
         return false;
      }
      return nearbyEnemyCount >= ARMY_MINIMUM_GROUP_SIZE
         || targetMaxHealth >= ARMY_FORMIDABLE_TARGET_HEALTH;
   }

   public static int armyUseChance(double healthRatio, int nearbyEnemyCount) {
      if (healthRatio <= ARMY_CRITICAL_HEALTH_RATIO) {
         return 30;
      }
      return nearbyEnemyCount >= ARMY_MINIMUM_GROUP_SIZE ? 18 : 8;
   }
}
