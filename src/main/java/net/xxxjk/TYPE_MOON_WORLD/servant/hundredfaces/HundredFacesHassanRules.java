package net.xxxjk.TYPE_MOON_WORLD.servant.hundredfaces;

import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;

public final class HundredFacesHassanRules {
   public static final String TAG_TOTAL_SPLIT_COUNT = "HundredFacesTotalSplitCount";
   public static final int MAX_PERSONAS = 80;
   public static final int MAX_SUMMON_BATCH = 10;
   public static final int MP_PER_PERSONA = 10;
   public static final int CONCEALMENT_EXPOSURE_TICKS = 60;
   public static final int RETREAT_COOLDOWN_TICKS = 400;
   public static final int RETREAT_DURATION_TICKS = 200;
   public static final int TARGET_SCAN_INTERVAL_TICKS = 20;
   public static final int DIRK_COOLDOWN_TICKS = 70;
   public static final int PERSONA_DIRK_COOLDOWN_TICKS = 120;
   public static final int SHADOW_STEP_COOLDOWN_TICKS = 120;
   public static final int PERSONA_SHADOW_STEP_COOLDOWN_TICKS = 180;
   public static final int SHADOW_LUNGE_COOLDOWN_TICKS = 90;
   public static final int PERSONA_SHADOW_LUNGE_COOLDOWN_TICKS = 150;
   public static final int KNIFE_FEINT_COOLDOWN_TICKS = 75;
   public static final int STAB_COMBO_COOLDOWN_TICKS = 42;
   public static final int REPOSITION_COOLDOWN_TICKS = 100;

   public static final float HUMANOID_BASE_WIDTH = 0.6F;
   public static final float HUMANOID_BASE_HEIGHT = 1.8F;
   public static final float MAIN_VISUAL_HEIGHT = 0.8F;
   public static final float PERSONA_MIN_VISUAL_HEIGHT = 0.8F;
   public static final float PERSONA_MAX_VISUAL_HEIGHT = 1.0F;

   public static final double MAIN_MOVEMENT_SPEED = 0.36;
   public static final double MAIN_CONCEALED_MOVEMENT_SPEED = 0.414;
   public static final double MAIN_RETREAT_MOVEMENT_SPEED = 0.504;
   public static final double MP_DIRK_THROW = 6.0;
   public static final double MP_SHADOW_STEP = 10.0;
   public static final double MP_SHADOW_LUNGE = 7.0;
   public static final double MP_KNIFE_FEINT = 5.0;
   public static final double MP_STAB_COMBO = 4.0;
   public static final float MAIN_DIRK_DAMAGE = 15.0F;
   public static final float PERSONA_DIRK_DAMAGE = 5.0F;
   public static final double PERSONA_ATTACK_DAMAGE = 5.0;
   public static final double PERSONA_MOVEMENT_SPEED = 0.36;
   public static final double PERSONA_BASE_HEALTH = 100.0;
   public static final double PERSONA_MIN_HEALTH = 50.0;
   public static final double PERSONA_BASE_ARMOR = 3.0;
   public static final double PERSONA_MIN_ARMOR = 3.0;
   public static final double PERSONA_KNOCKBACK_RESISTANCE = 0.2;
   public static final double PERSONA_DEFENSE_RECOVERY_MULTIPLIER = 1.0;

   public static final ServantParams MAIN_FULL_PARAMS = ServantParams.of(
      "D", false, "C", false, "A", false, "C", false, "E", false
   );
   public static final ServantParams PERSONA_E_RANK_PARAMS = ServantParams.of(
      "E", false, "E", false, "E", false, "E", false, "E", false
   );
   public static final ServantParams MAIN_FULL_SPLIT_PARAMS = ServantParams.of(
      "E", false, "E", false, "E", false, "E", false, "E", false
   );
   public static final double MAIN_BASE_HEALTH = MAIN_FULL_PARAMS.maxHealth();
   public static final double MAIN_BASE_ATTACK_DAMAGE = MAIN_FULL_PARAMS.attackDamage();
   public static final double MAIN_BASE_ARMOR = MAIN_FULL_PARAMS.armor();
   public static final double MAIN_BASE_MANA = MAIN_FULL_PARAMS.manaPool();
   public static final double MAIN_MIN_HEALTH = PERSONA_MIN_HEALTH;
   public static final double MAIN_MIN_ATTACK_DAMAGE = PERSONA_ATTACK_DAMAGE;
   public static final double MAIN_MIN_ARMOR = PERSONA_MIN_ARMOR;
   public static final double MAIN_MIN_MANA = PERSONA_E_RANK_PARAMS.manaPool();

   private HundredFacesHassanRules() {
   }

   public static double personaHealthForCount(int liveCount) {
      double t = countRatio(liveCount);
      return PERSONA_BASE_HEALTH - (PERSONA_BASE_HEALTH - PERSONA_MIN_HEALTH) * t;
   }

   public static double personaAttackDamageForCount(int liveCount) {
      double t = countRatio(liveCount);
      return MAIN_DIRK_DAMAGE - (MAIN_DIRK_DAMAGE - PERSONA_ATTACK_DAMAGE) * t;
   }

   public static double personaArmorForCount(int liveCount) {
      double t = countRatio(liveCount);
      return PERSONA_BASE_ARMOR - (PERSONA_BASE_ARMOR - PERSONA_MIN_ARMOR) * t;
   }

   public static double mainHealthForSplitCount(int splitCount) {
      double t = splitRatio(splitCount);
      return MAIN_BASE_HEALTH - (MAIN_BASE_HEALTH - MAIN_MIN_HEALTH) * t;
   }

   public static double mainAttackDamageForSplitCount(int splitCount) {
      double t = splitRatio(splitCount);
      return MAIN_BASE_ATTACK_DAMAGE - (MAIN_BASE_ATTACK_DAMAGE - MAIN_MIN_ATTACK_DAMAGE) * t;
   }

   public static double mainArmorForSplitCount(int splitCount) {
      double t = splitRatio(splitCount);
      return MAIN_BASE_ARMOR - (MAIN_BASE_ARMOR - MAIN_MIN_ARMOR) * t;
   }

   public static double mainManaForSplitCount(int splitCount) {
      double t = splitRatio(splitCount);
      return MAIN_BASE_MANA - (MAIN_BASE_MANA - MAIN_MIN_MANA) * t;
   }

   public static ServantParams mainCombatParamsForSplitCount(int splitCount, ServantParams fallback) {
      if (splitCount >= MAX_PERSONAS) {
         return MAIN_FULL_SPLIT_PARAMS;
      }
      return fallback;
   }

   public static float visualScaleForHeight(float height) {
      return clampVisualHeight(height);
   }

   public static float widthForHeight(float height) {
      return HUMANOID_BASE_WIDTH * visualScaleForHeight(height);
   }

   public static float collisionHeightForVisualHeight(float height) {
      return HUMANOID_BASE_HEIGHT * visualScaleForHeight(height);
   }

   public static float clampVisualHeight(float height) {
      return Math.max(PERSONA_MIN_VISUAL_HEIGHT, Math.min(PERSONA_MAX_VISUAL_HEIGHT, height));
   }

   public static int affordableSummonCount(double currentMp, int liveCount, int requested) {
      int capacity = Math.max(0, MAX_PERSONAS - Math.max(0, liveCount));
      int mpLimited = (int)Math.floor(Math.max(0.0, currentMp) / MP_PER_PERSONA);
      return Math.max(0, Math.min(Math.min(requested, MAX_SUMMON_BATCH), Math.min(capacity, mpLimited)));
   }

   private static double countRatio(int liveCount) {
      int count = Math.max(1, Math.min(MAX_PERSONAS, liveCount));
      return (count - 1) / (double)(MAX_PERSONAS - 1);
   }

   private static double splitRatio(int splitCount) {
      int count = Math.max(0, Math.min(MAX_PERSONAS, splitCount));
      return count / (double)MAX_PERSONAS;
   }
}
