package net.xxxjk.typemoonworld.api;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record AiTacticProfile(double followDistance, double attackDistance, double retreatHealthRatio,
      int decisionCooldown, List<AiTactic> tactics) {
   public AiTacticProfile {
      followDistance = Math.max(0.0, followDistance);
      attackDistance = Math.max(0.0, attackDistance);
      retreatHealthRatio = Math.max(0.0, Math.min(1.0, retreatHealthRatio));
      decisionCooldown = Math.max(0, decisionCooldown);
      tactics = tactics == null ? List.of() : List.copyOf(tactics);
   }
}
