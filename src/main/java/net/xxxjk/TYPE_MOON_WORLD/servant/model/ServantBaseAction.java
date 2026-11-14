package net.xxxjk.TYPE_MOON_WORLD.servant.model;

/** Shared presentation IDs; these never perform attacks or change combat timing. */
public final class ServantBaseAction {
   public static final int NONE = -2;
   public static final int STOP = -1;
   public static final int SWORD = 0;
   public static final int FIST = 1;
   public static final int SWORD_COMBO = 9;
   public static final int FIST_COMBO = 10;
   public static final int ZHAO_YUN_THRUST = 11;
   public static final int ZHAO_YUN_SWEEP = 12;
   public static final int ZHAO_YUN_DANCE = 13;
   public static final int MURAMASA_DOWNWARD_SLASH = 40;
   public static final int MURAMASA_THRUST = 41;
   public static final int MURAMASA_SPINNING_SLASH = 42;
   public static final int ZHAO_YUN_COMBO = 14;
   public static final int ZHAO_YUN_HELD_DANCE = 15;
   public static final int ZHAO_YUN_RECOVERY = 16;
   private static final String[] DODGES = {"", "", "servant_base_dodge_small_shanbi1", "servant_base_dodge_small_shanbi2",
      "servant_base_dodge_large_forward", "servant_base_dodge_large_back", "servant_base_dodge_large_left",
      "servant_base_dodge_large_right", "servant_base_dodge_large_forward_right"};

   private ServantBaseAction() { }

   public static boolean isAttack(int action) {
      return action == SWORD || action == FIST || isContinuous(action)
         || action == ZHAO_YUN_THRUST || action == ZHAO_YUN_SWEEP || action == ZHAO_YUN_DANCE || action == ZHAO_YUN_RECOVERY
         || action == MURAMASA_DOWNWARD_SLASH || action == MURAMASA_THRUST || action == MURAMASA_SPINNING_SLASH;
   }

   public static boolean isContinuous(int action) {
      return action == SWORD_COMBO || action == FIST_COMBO || action == ZHAO_YUN_COMBO || action == ZHAO_YUN_HELD_DANCE;
   }

   public static boolean isZhaoYunSpear(int action) {
      return action >= ZHAO_YUN_THRUST && action <= ZHAO_YUN_RECOVERY;
   }

   public static int zhaoYunAttack(int action, int sequence) {
      if (action == SWORD_COMBO) return ZHAO_YUN_COMBO;
      if (action != SWORD) return action;
      return switch (Math.floorMod(sequence, 6)) {
         case 0, 2, 4 -> ZHAO_YUN_THRUST;
         case 1, 3 -> ZHAO_YUN_SWEEP;
         default -> ZHAO_YUN_DANCE;
      };
   }

   public static String animationId(int action, int variant) {
      return switch (action) {
         case SWORD -> "servant_base_sword_huijian1";
         case FIST -> (variant & 1) == 0 ? "servant_base_fist_qingji1" : "servant_base_fist_qingji2";
         case SWORD_COMBO -> "servant_base_sword_huijian";
         case FIST_COMBO -> "servant_base_fist_lianji";
         case ZHAO_YUN_THRUST -> "zhao_yun_spear_thrust";
         case ZHAO_YUN_SWEEP -> "zhao_yun_spear_sweep";
         case ZHAO_YUN_DANCE -> "zhao_yun_spear_dance";
         case MURAMASA_DOWNWARD_SLASH -> "muramasa_downward_slash";
         case MURAMASA_THRUST -> "muramasa_thrust";
         case MURAMASA_SPINNING_SLASH -> "muramasa_spinning_slash";
         case ZHAO_YUN_COMBO -> "zhao_yun_spear_combo";
         case ZHAO_YUN_HELD_DANCE -> "zhao_yun_spear_held_dance";
         case ZHAO_YUN_RECOVERY -> "zhao_yun_spear_recovery";
         default -> action >= 2 && action < DODGES.length ? DODGES[action] : null;
      };
   }
}
