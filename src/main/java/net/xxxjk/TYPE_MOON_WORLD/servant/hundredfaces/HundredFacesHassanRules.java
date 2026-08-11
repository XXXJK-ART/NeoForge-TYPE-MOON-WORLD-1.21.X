package net.xxxjk.TYPE_MOON_WORLD.servant.hundredfaces;

public final class HundredFacesHassanRules {
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

   public static final double MAIN_MOVEMENT_SPEED = 0.20;
   public static final double MAIN_CONCEALED_MOVEMENT_SPEED = 0.23;
   public static final double MAIN_RETREAT_MOVEMENT_SPEED = 0.28;
   public static final double MP_DIRK_THROW = 6.0;
   public static final double MP_SHADOW_STEP = 10.0;
   public static final double MP_SHADOW_LUNGE = 7.0;
   public static final double MP_KNIFE_FEINT = 5.0;
   public static final double MP_STAB_COMBO = 4.0;
   public static final float MAIN_DIRK_DAMAGE = 15.0F;
   public static final float PERSONA_DIRK_DAMAGE = 9.0F;

   public static final double PERSONA_ATTACK_DAMAGE = 7.0;
   public static final double PERSONA_MOVEMENT_SPEED = 0.23;
   public static final double PERSONA_BASE_HEALTH = 100.0;
   public static final double PERSONA_MIN_HEALTH = 100.0;
   public static final double PERSONA_BASE_ARMOR = 4.0;
   public static final double PERSONA_MIN_ARMOR = 3.0;

   private HundredFacesHassanRules() {
   }

   public static double personaHealthForCount(int liveCount) {
      double t = countRatio(liveCount);
      return PERSONA_BASE_HEALTH - (PERSONA_BASE_HEALTH - PERSONA_MIN_HEALTH) * t;
   }

   public static double personaArmorForCount(int liveCount) {
      double t = countRatio(liveCount);
      return PERSONA_BASE_ARMOR - (PERSONA_BASE_ARMOR - PERSONA_MIN_ARMOR) * t;
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
}
