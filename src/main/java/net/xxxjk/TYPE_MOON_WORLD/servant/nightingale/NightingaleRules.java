package net.xxxjk.TYPE_MOON_WORLD.servant.nightingale;

public final class NightingaleRules {
   public static final float MAD_ENHANCEMENT_MULTIPLIER = 1.25F;
   public static final float HUMANOID_ATTACK_MULTIPLIER = 1.5F;
   public static final float HUMANOID_DEFENSE_MULTIPLIER = 0.75F;
   public static final float ANGEL_DAMAGE_MULTIPLIER = 1.3F;
   public static final int STEEL_NURSING_COOLDOWN = 200;
   public static final int STEEL_NURSING_COST = 15;
   public static final float STEEL_NURSING_AMOUNT = 150.0F;
   public static final int ANGEL_CRY_COOLDOWN = 300;
   public static final int ANGEL_CRY_COST = 10;
   public static final int ANGEL_CRY_DURATION = 400;
   public static final int NOBLE_PHANTASM_COST = 50;
   public static final int NOBLE_PHANTASM_COOLDOWN = 600;
   public static final int NOBLE_PHANTASM_WINDUP = 20;
   public static final int SAFETY_CIRCLE_DURATION = 100;
   public static final double SUPPORT_RANGE = 15.0;
   public static final float NOBLE_PHANTASM_HEAL = 200.0F;
   public static final double NOBLE_PHANTASM_MANA = 50.0;

   private NightingaleRules() {}

   public static float outgoingMultiplier(boolean humanoidTarget, boolean angelCry) {
      float multiplier = MAD_ENHANCEMENT_MULTIPLIER;
      if (humanoidTarget) multiplier *= HUMANOID_ATTACK_MULTIPLIER;
      if (angelCry) multiplier *= ANGEL_DAMAGE_MULTIPLIER;
      return multiplier;
   }

   public static boolean shouldUseNoblePhantasm(float selfRatio, float lowestAllyRatio, int alliesAtOrBelowHalf) {
      return selfRatio <= 0.35F || lowestAllyRatio <= 0.20F || alliesAtOrBelowHalf >= 2;
   }

   public static boolean canAttackNormallyInnocent(long now, long lastAggressionTick) {
      return lastAggressionTick > 0L && now - lastAggressionTick <= 400L;
   }

   public static boolean isNursingEligible(float health, float maxHealth) {
      return maxHealth - health >= 20.0F;
   }

   public static double nursingPriority(float health, float maxHealth) {
      return health / Math.max(1.0F, maxHealth);
   }

   public static int adjustActionTicks(int baseTicks, boolean angelCry) {
      return angelCry ? Math.max(1, (int)Math.ceil(baseTicks / 1.2)) : Math.max(1, baseTicks);
   }
}
