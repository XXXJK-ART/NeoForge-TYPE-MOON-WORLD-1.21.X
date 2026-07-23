package net.xxxjk.TYPE_MOON_WORLD.servant.model;

import java.util.List;
import java.util.Map;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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
   public static final Codec<ServantSkillDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.STRING.optionalFieldOf("id", "").forGetter(ServantSkillDefinition::id),
      Codec.STRING.optionalFieldOf("display_name", "").forGetter(ServantSkillDefinition::displayName),
      Codec.STRING.optionalFieldOf("display_name_zh", "").forGetter(ServantSkillDefinition::displayNameZh),
      Codec.STRING.xmap(SkillType::fromKey, value -> value.name().toLowerCase()).optionalFieldOf("type", SkillType.ACTIVE).forGetter(ServantSkillDefinition::type),
      Codec.INT.optionalFieldOf("mp_cost", 0).forGetter(ServantSkillDefinition::mpCost),
      Codec.INT.optionalFieldOf("cooldown_ticks", 0).forGetter(ServantSkillDefinition::cooldownTicks),
      Codec.INT.optionalFieldOf("duration_ticks", 0).forGetter(ServantSkillDefinition::durationTicks),
      SkillEffectEntry.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(ServantSkillDefinition::effects)
   ).apply(instance, ServantSkillDefinition::new));

   public ServantSkillDefinition {
      id = id == null ? "" : id;
      displayName = displayName == null ? "" : displayName;
      displayNameZh = displayNameZh == null ? "" : displayNameZh;
      type = type == null ? SkillType.ACTIVE : type;
      mpCost = Math.max(0, mpCost);
      cooldownTicks = Math.max(0, cooldownTicks);
      durationTicks = Math.max(0, durationTicks);
      effects = effects == null ? List.of() : List.copyOf(effects);
   }
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
