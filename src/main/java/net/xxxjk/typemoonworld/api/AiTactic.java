package net.xxxjk.typemoonworld.api;

import net.minecraft.resources.ResourceLocation;

public record AiTactic(ResourceLocation action, int weight, double minDistance, double maxDistance,
      double minHealthRatio, int cooldown) {
   public AiTactic {
      weight = Math.max(0, Math.min(10000, weight));
      minDistance = Math.max(0.0, minDistance);
      maxDistance = Math.max(minDistance, maxDistance);
      minHealthRatio = Math.max(0.0, Math.min(1.0, minHealthRatio));
      cooldown = Math.max(0, cooldown);
   }
}
