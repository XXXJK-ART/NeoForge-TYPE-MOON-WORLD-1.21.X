package net.xxxjk.typemoonworld.api;

/** Optional advanced movement and reaction policy layered over the compatible v1 tactic profile. */
public record AdvancedAiTacticProfile(AiTacticProfile base, AiCombatStyle style, double minimumRange,
      double preferredRange, double maximumRange, double repositionDistance, double pursuitAggression,
      double interceptBias, double verticalMobility, double recoveryTendency, double collateralCaution,
      String maximumTerrainImpact) {
   public AdvancedAiTacticProfile {
      if (base == null) throw new IllegalArgumentException("base");
      style = style == null ? AiCombatStyle.BALANCED : style;
      minimumRange = Math.max(0.0, Math.min(48.0, minimumRange));
      preferredRange = Math.max(minimumRange, Math.min(48.0, preferredRange));
      maximumRange = Math.max(preferredRange, Math.min(48.0, maximumRange));
      repositionDistance = Math.max(0.0, Math.min(24.0, repositionDistance));
      pursuitAggression = clamp01(pursuitAggression);
      interceptBias = Math.max(0.0, Math.min(8.0, interceptBias));
      verticalMobility = clamp01(verticalMobility);
      recoveryTendency = clamp01(recoveryTendency);
      collateralCaution = clamp01(collateralCaution);
      maximumTerrainImpact = maximumTerrainImpact == null || maximumTerrainImpact.isBlank() ? "small" : maximumTerrainImpact;
   }

   public static AdvancedAiTacticProfile compatible(AiTacticProfile base) {
      double preferred = base == null ? 8.0 : base.attackDistance();
      return new AdvancedAiTacticProfile(base, AiCombatStyle.BALANCED, Math.max(0.0, preferred - 4.0),
         preferred, Math.min(48.0, Math.max(preferred, preferred + 12.0)), 10.0, 0.5, 1.5, 0.25, 0.5, 0.7, "small");
   }

   private static double clamp01(double value) { return Math.max(0.0, Math.min(1.0, value)); }
}
