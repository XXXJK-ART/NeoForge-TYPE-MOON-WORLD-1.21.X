package net.xxxjk.TYPE_MOON_WORLD.combat;

public final class ChurchDeadApostleRules {
   public static final int BLACK_KEY_MAX_BROKEN_BLOCKS = 2;
   public static final float BLACK_KEY_MAX_BREAK_HARDNESS = 1.5F;

   private ChurchDeadApostleRules() {}

   public static float blackKeyMeleeDamage(boolean expanded, int count) {
      if (!expanded) return 3.0F;
      return switch (Math.clamp(count, 1, 3)) {
         case 1 -> 9.0F;
         case 2 -> 12.0F;
         default -> 15.0F;
      };
   }

   public static float blackKeyThrowYawOffset(int count, int index) {
      int clampedCount = Math.clamp(count, 1, 3);
      int clampedIndex = Math.clamp(index, 0, clampedCount - 1);
      return (clampedIndex - (clampedCount - 1) * 0.5F) * 7.0F;
   }

   public static boolean blackKeyCanBreakBlock(float hardness, boolean hasBlockEntity, boolean protectedBlock) {
      return hardness >= 0.0F && hardness <= BLACK_KEY_MAX_BREAK_HARDNESS && !hasBlockEntity && !protectedBlock;
   }

   public static boolean shouldStigmaDelay(float finalDamage, boolean excluded) {
      return !excluded && finalDamage > 0.0F && finalDamage <= 20.0F;
   }

   public static boolean cassockBlocks(float finalProjectileDamage) {
      return finalProjectileDamage >= 0.0F && finalProjectileDamage < 10.0F;
   }

   public static ConversionStage conversionStage(double roll) {
      if (roll < 0.0 || roll >= 1.0) throw new IllegalArgumentException("roll must be in [0, 1)");
      if (roll < 0.70) return ConversionStage.THE_DEAD;
      if (roll < 0.99) return ConversionStage.GHOUL;
      if (roll < 0.999) return ConversionStage.LIVING_DEAD;
      return ConversionStage.NIGHT_KIN;
   }

   public enum ConversionStage { THE_DEAD, GHOUL, LIVING_DEAD, NIGHT_KIN }
}
