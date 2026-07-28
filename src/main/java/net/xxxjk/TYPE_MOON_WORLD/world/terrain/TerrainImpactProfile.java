package net.xxxjk.TYPE_MOON_WORLD.world.terrain;

public record TerrainImpactProfile(Tier tier, double radius, float maximumHardness, int debrisCount, int dustCount) {
   public TerrainImpactProfile {
      tier = tier == null ? Tier.NONE : tier;
      radius = Math.max(0.0, radius);
      maximumHardness = Math.max(0.0F, maximumHardness);
      debrisCount = Math.max(0, debrisCount);
      dustCount = Math.max(0, dustCount);
   }

   public static TerrainImpactProfile of(Tier tier) {
      return switch (tier == null ? Tier.NONE : tier) {
         case NONE -> new TerrainImpactProfile(Tier.NONE, 0.0, 0.0F, 0, 0);
         case CHIP -> new TerrainImpactProfile(Tier.CHIP, 1.5, 0.6F, 4, 12);
         case SMALL -> new TerrainImpactProfile(Tier.SMALL, 2.5, 2.0F, 12, 28);
         case MEDIUM -> new TerrainImpactProfile(Tier.MEDIUM, 4.0, 6.0F, 24, 48);
         case HEAVY -> new TerrainImpactProfile(Tier.HEAVY, 6.0, 30.0F, 36, 64);
         case NP -> new TerrainImpactProfile(Tier.NP, 0.0, Float.MAX_VALUE, 48, 96);
      };
   }

   public boolean limitsSelfFootDepth() {
      return tier == Tier.CHIP || tier == Tier.SMALL || tier == Tier.MEDIUM;
   }

   public int physicalDebrisCount() {
      return switch (tier) {
         case MEDIUM -> 4;
         case HEAVY -> 8;
         case NP -> 12;
         default -> 0;
      };
   }

   public enum Tier { NONE, CHIP, SMALL, MEDIUM, HEAVY, NP }
}
