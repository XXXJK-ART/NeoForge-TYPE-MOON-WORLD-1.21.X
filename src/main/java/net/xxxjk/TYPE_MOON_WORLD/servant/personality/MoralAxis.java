package net.xxxjk.TYPE_MOON_WORLD.servant.personality;

import java.util.List;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;

public enum MoralAxis {
   GOOD("good"),
   NEUTRAL("neutral"),
   EVIL("evil");

   private final String key;

   MoralAxis(String key) {
      this.key = key;
   }

   public String key() {
      return this.key;
   }

   public static MoralAxis fromKey(String key) {
      if (key == null) {
         return NEUTRAL;
      }
      for (MoralAxis axis : values()) {
         if (axis.key.equalsIgnoreCase(key)) {
            return axis;
         }
      }
      return NEUTRAL;
   }

   public static MoralAxis fromTraits(List<ServantTraitTag> traits) {
      if (traits == null || traits.isEmpty()) {
         return NEUTRAL;
      }
      if (traits.contains(ServantTraitTag.GOOD)) {
         return GOOD;
      }
      if (traits.contains(ServantTraitTag.EVIL)) {
         return EVIL;
      }
      return NEUTRAL;
   }
}
