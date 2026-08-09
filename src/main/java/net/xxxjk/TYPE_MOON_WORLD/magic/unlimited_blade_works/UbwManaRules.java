package net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works;

public final class UbwManaRules {
   public static final int PLAYER_OPENING_PAYMENT_COUNT = 10;
   public static final double PLAYER_OPENING_PAYMENT = 20.0D;
   public static final double PLAYER_OPENING_TOTAL = PLAYER_OPENING_PAYMENT_COUNT * PLAYER_OPENING_PAYMENT;
   public static final int PLAYER_FREE_UPKEEP_TICKS = 30 * 20;
   public static final double PLAYER_UPKEEP_PER_SECOND = 2.0D;
   public static final double SERVANT_OPENING_PAYMENT = 50.0D;
   public static final double SERVANT_UPKEEP_PER_SECOND = 10.0D;

   private UbwManaRules() {
   }

   public static double openingPayment(boolean servantCard) {
      return servantCard ? SERVANT_OPENING_PAYMENT : PLAYER_OPENING_PAYMENT;
   }

   public static double upkeepCost(boolean servantCard, long activeTicks) {
      if (servantCard) {
         return SERVANT_UPKEEP_PER_SECOND;
      }
      return activeTicks >= PLAYER_FREE_UPKEEP_TICKS ? PLAYER_UPKEEP_PER_SECOND : 0.0D;
   }
}
