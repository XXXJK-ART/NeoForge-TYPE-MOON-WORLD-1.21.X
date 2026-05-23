package net.xxxjk.TYPE_MOON_WORLD.servant.model;

import java.util.List;
import java.util.Map;

public record ServantSkillDefinition(
   String id,
   String displayName,
   String displayNameZh,
   SkillType type,
   int mpCost,
   int cooldownTicks,
   int durationTicks,
   List<SkillEffectEntry> effects
) {
   public enum SkillType {
      ACTIVE,
      PASSIVE;

      public static SkillType fromKey(String key) {
         if (key == null) {
            return ACTIVE;
         }

         return switch (key.toLowerCase()) {
            case "passive" -> PASSIVE;
            default -> ACTIVE;
         };
      }
   }
}
