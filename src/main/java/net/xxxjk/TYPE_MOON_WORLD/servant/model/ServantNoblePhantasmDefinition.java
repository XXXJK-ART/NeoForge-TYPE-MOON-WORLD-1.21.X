package net.xxxjk.TYPE_MOON_WORLD.servant.model;

import java.util.List;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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
   public static final Codec<ServantNoblePhantasmDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.STRING.optionalFieldOf("id", "").forGetter(ServantNoblePhantasmDefinition::id),
      Codec.STRING.optionalFieldOf("display_name", "").forGetter(ServantNoblePhantasmDefinition::displayName),
      Codec.STRING.optionalFieldOf("display_name_zh", "").forGetter(ServantNoblePhantasmDefinition::displayNameZh),
      Codec.STRING.xmap(NpType::fromKey, value -> value.key).optionalFieldOf("type", NpType.ANTI_UNIT).forGetter(ServantNoblePhantasmDefinition::type),
      Codec.STRING.optionalFieldOf("rank", "E").forGetter(ServantNoblePhantasmDefinition::rank),
      Codec.INT.optionalFieldOf("mp_cost", 0).forGetter(ServantNoblePhantasmDefinition::mpCost),
      Codec.BOOL.optionalFieldOf("over_charge_supported", false).forGetter(ServantNoblePhantasmDefinition::overChargeSupported),
      Codec.INT.optionalFieldOf("over_charge_levels", 1).forGetter(ServantNoblePhantasmDefinition::overChargeLevels),
      Codec.DOUBLE.optionalFieldOf("base_damage_multiplier", 1.0).forGetter(ServantNoblePhantasmDefinition::baseDamageMultiplier),
      Codec.DOUBLE.optionalFieldOf("range", 16.0).forGetter(ServantNoblePhantasmDefinition::range),
      SkillEffectEntry.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(ServantNoblePhantasmDefinition::effects),
      Codec.STRING.listOf().xmap(values -> values.stream().map(ServantTraitTag::fromKey).toList(), values -> values.stream().map(ServantTraitTag::key).toList()).optionalFieldOf("special_attack_conditions", List.of()).forGetter(ServantNoblePhantasmDefinition::specialAttackConditions),
      Codec.STRING.listOf().xmap(values -> values.stream().map(ServantClassType::fromKey).toList(), values -> values.stream().map(ServantClassType::key).toList()).optionalFieldOf("class_restrictions", List.of()).forGetter(ServantNoblePhantasmDefinition::classRestrictions)
   ).apply(instance, ServantNoblePhantasmDefinition::new));

   public ServantNoblePhantasmDefinition {
      id = id == null ? "" : id;
      displayName = displayName == null ? "" : displayName;
      displayNameZh = displayNameZh == null ? "" : displayNameZh;
      type = type == null ? NpType.ANTI_UNIT : type;
      rank = rank == null ? "E" : rank;
      mpCost = Math.max(0, mpCost);
      overChargeLevels = Math.max(1, overChargeLevels);
      baseDamageMultiplier = Math.max(0.0, baseDamageMultiplier);
      range = Math.max(0.0, range);
      effects = effects == null ? List.of() : List.copyOf(effects);
      specialAttackConditions = specialAttackConditions == null ? List.of() : List.copyOf(specialAttackConditions);
      classRestrictions = classRestrictions == null ? List.of() : List.copyOf(classRestrictions);
   }
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
            if (type.key.equals(key.toLowerCase()) || ("anti_fortress".equals(key.toLowerCase()) && type == ANTI_UNIT)) {
               return type;
            }
         }

         return ANTI_UNIT;
      }
   }
}
