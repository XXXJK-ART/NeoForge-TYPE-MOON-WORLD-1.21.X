package net.xxxjk.TYPE_MOON_WORLD.martial;

public final class BajiquanComboRules {
   private BajiquanComboRules() {}

   public static boolean isLegalRecoveryCancel(BajiquanMove last, BajiquanMove next, boolean pursuitActive) {
      if (last == null || next == null) return false;
      if (next == BajiquanMove.FIERCE_TIGER || next == BajiquanMove.CLAMP) return true;
      return switch (last) {
         case PUNCH -> next == BajiquanMove.ELBOW || next == BajiquanMove.SHOULDER
            || next == BajiquanMove.RIGHT_KICK || next == BajiquanMove.FINISHER_KICK;
         case ELBOW -> next == BajiquanMove.FLURRY;
         case FLURRY -> next == BajiquanMove.PALM || next == BajiquanMove.FINISHER_KICK;
         case PALM -> next == BajiquanMove.SHOULDER || next == BajiquanMove.FINISHER_KICK
            || next == BajiquanMove.TREMOR || next == BajiquanMove.CHARGED_TREMOR;
         case SHOULDER -> next == BajiquanMove.FLURRY || next == BajiquanMove.PUNCH || next == BajiquanMove.PUSH;
         case PUSH -> next == BajiquanMove.RIGHT_KICK;
         case RIGHT_KICK, LEFT_KICK -> next == BajiquanMove.PUNCH || next == BajiquanMove.LEFT_KICK || next == BajiquanMove.KNEE;
         case KNEE -> next == BajiquanMove.DOWN_KICK || next == BajiquanMove.FLURRY;
         case DOWN_KICK -> next == BajiquanMove.FLURRY;
         case CHOP -> next == BajiquanMove.ELBOW;
         case STOMP -> next == BajiquanMove.TREMOR;
         case TREMOR -> next == BajiquanMove.PARRY;
         case CHARGED_TREMOR -> next == BajiquanMove.DOUBLE_PALM;
         case PARRY, FA_JIN -> next == BajiquanMove.FA_JIN || next == BajiquanMove.PUNCH;
         case CLAMP -> next == BajiquanMove.SHOULDER;
         default -> pursuitActive && (next == BajiquanMove.PUNCH || next == BajiquanMove.PUSH);
      };
   }
}
