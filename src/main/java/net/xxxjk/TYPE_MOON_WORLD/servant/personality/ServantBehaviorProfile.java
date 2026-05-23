package net.xxxjk.TYPE_MOON_WORLD.servant.personality;

public record ServantBehaviorProfile(
   double obedienceModifier,
   double retreatHealthRatio,
   double aggressionRange,
   double attackCommitDistance,
   double skillUsageFrequency
) {
   public ServantBehaviorProfile {
   }

   public double effectiveObedienceRate(double baseRate, double favor) {
      return Math.min(1.0, Math.max(0.0, baseRate + this.obedienceModifier + favor * 0.005));
   }

   public double applyCombatDisposition(CombatDisposition disposition) {
      return switch (disposition) {
         case CAUTIOUS -> this.retreatHealthRatio + 0.10;
         case FRENZIED -> this.retreatHealthRatio - 0.10;
         default -> this.retreatHealthRatio;
      };
   }
}
