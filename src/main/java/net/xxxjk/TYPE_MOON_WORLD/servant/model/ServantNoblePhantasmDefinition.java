package net.xxxjk.TYPE_MOON_WORLD.servant.model;

import java.util.List;

public record ServantNoblePhantasmDefinition(
   String id,
   String displayName,
   String displayNameZh,
   NpType type,
   String rank,
   int mpCost,
   boolean overChargeSupported,
   int overChargeLevels,
   double baseDamageMultiplier,
   double range,
   List<SkillEffectEntry> effects,
   List<ServantTraitTag> specialAttackConditions,
   List<ServantClassType> classRestrictions
) {
   public enum NpType {
      ARMY("army"),
      CASTLE("castle"),
      WORLD("world"),
      ANTI_UNIT("anti_unit"),
      BARRIER("barrier"),
      SUPPORT("support");

      private final String key;

      NpType(String key) {
         this.key = key;
      }

      public String key() {
         return this.key;
      }

      public static NpType fromKey(String key) {
         if (key == null) {
            return ANTI_UNIT;
         }

         for (NpType type : values()) {
            if (type.key.equals(key.toLowerCase())) {
               return type;
            }
         }

         return ANTI_UNIT;
      }
   }
}
