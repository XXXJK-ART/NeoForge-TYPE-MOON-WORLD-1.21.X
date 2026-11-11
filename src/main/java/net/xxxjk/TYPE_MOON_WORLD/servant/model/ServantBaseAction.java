package net.xxxjk.TYPE_MOON_WORLD.servant.model;

/** Shared presentation IDs; these never perform attacks or change combat timing. */
public final class ServantBaseAction {
   public static final int NONE = -2;
   public static final int STOP = -1;
   public static final int SWORD = 0;
   public static final int FIST = 1;
   public static final int SWORD_COMBO = 9;
   public static final int FIST_COMBO = 10;
   private static final String[] DODGES = {"", "", "servant_base_dodge_small_shanbi1", "servant_base_dodge_small_shanbi2",
      "servant_base_dodge_large_forward", "servant_base_dodge_large_back", "servant_base_dodge_large_left",
      "servant_base_dodge_large_right", "servant_base_dodge_large_forward_right"};

   private ServantBaseAction() { }

   public static boolean isAttack(int action) {
      return action == SWORD || action == FIST || isContinuous(action);
   }

   public static boolean isContinuous(int action) {
      return action == SWORD_COMBO || action == FIST_COMBO;
   }

   public static String animationId(int action, int variant) {
      return switch (action) {
         case SWORD -> "servant_base_sword_huijian1";
         case FIST -> (variant & 1) == 0 ? "servant_base_fist_qingji1" : "servant_base_fist_qingji2";
         case SWORD_COMBO -> "servant_base_sword_huijian";
         case FIST_COMBO -> "servant_base_fist_lianji";
         default -> action >= 2 && action < DODGES.length ? DODGES[action] : null;
      };
   }
}
