package net.xxxjk.TYPE_MOON_WORLD.vfx;

import net.xxxjk.TYPE_MOON_WORLD.Config;

/** Shared cap for server-broadcast vanilla particles. */
public final class ParticleBudget {
   private ParticleBudget() {
   }

   public static int count(int requested) {
      if (requested <= 0) return 0;
      int configured = Config.vfxMaxParticles;
      int qualityBudget = switch (Config.vfxQuality == null ? "AUTO" : Config.vfxQuality) {
         case "LOW" -> 64;
         case "MEDIUM" -> 160;
         case "HIGH" -> 320;
         default -> 192;
      };
      int budget = configured > 0 ? configured : qualityBudget;
      return Math.min(requested, budget);
   }
}
